# infra/

Infraestrutura AWS própria do microsserviço. Não depende do `service-track-aws-iac`
e o `service-track-aws-iac` não sabe que ela existe.

| Recurso | Nome |
|---|---|
| ECR | `servicetrack-<ambiente>-catalogo` |
| Parâmetro SSM com a URL do ECR | `/servicetrack/<ambiente>/catalogo/ecr-url` |
| RDS Postgres do serviço | `servicetrack-<ambiente>-catalogo` |
| Parâmetros SSM do banco | `/servicetrack/<ambiente>/catalogo/db/{url,endpoint,port,name,username,password}` |

State no mesmo bucket da plataforma, chave própria:
`servicetrack/<ambiente>/catalogo/terraform.tfstate`.

## Subir

```bash
cd infra/terraform
terraform init -backend-config=backend/hml.hcl -reconfigure
terraform apply -var ambiente=hml
```

Para `prd`, trocar `hml` por `prd` nas duas linhas.

## Destruir

```bash
terraform destroy -var ambiente=hml
```

Independente da ordem rede → banco → stack: o ECR não depende de VPC nem de EKS. Destruir o
stack da plataforma não apaga as imagens deste serviço — só este `destroy` apaga.

## Onde a esteira publica a imagem

A URL do repositório muda entre contas e ambientes. Ler em tempo de uso:

```bash
aws ssm get-parameter --name /servicetrack/hml/catalogo/ecr-url --query Parameter.Value --output text
```

## O banco é deste serviço, não da plataforma

O Postgres `st_cat` é provisionado aqui, não no `service-track-aws-iac` nem no
`service-track-db-infra`. Nenhum outro serviço tem credencial dele, e este serviço não tem
credencial do banco de ninguém. É a regra da Fase 4: **nenhum serviço acessa o banco de outro**.

A VPC e as subnets privadas vêm da plataforma, descobertas por tag (`servicetrack-<ambiente>-vpc`
e `servicetrack-<ambiente>-private-*`). Por isso a ordem de subida continua valendo: **rede
primeiro, este apply depois**. Sem a VPC, o `plan` falha ao não encontrar a VPC pela tag.

O grupo de segurança libera 5432 apenas para o CIDR da VPC. O banco não é acessível da
internet, o que significa que nenhum `psql` do seu laptop alcança ele — o acesso é de dentro
do cluster (ver `scripts/aplicar-baseline.sh`).

## Orçamento de conexão

O `apply` falha se o pool estourar o teto do banco:

```
db_pool_maximo x replicas_maximas + conexoes_reservadas <= db_max_connections
```

Com os padrões: `10 x 4 + 10 = 50 <= 60`. Subir o `maxReplicas` do HPA sem subir o orçamento
aqui é exatamente o que a precondição existe para impedir — o `plan` recusa antes de o
ambiente aceitar mais réplicas do que o banco suporta.

## Ler os valores em tempo de uso

Endpoint e senha mudam a cada recriação. Nunca fixar em documento, cliente ou vídeo:

```bash
terraform output db_jdbc_url

aws ssm get-parameter --name /servicetrack/hml/catalogo/db/password \
  --with-decryption --query Parameter.Value --output text
```

A senha é gerada pelo Terraform e publicada como `SecureString`. Ela fica no state, como todo
segredo entregue por Terraform — é a mesma exposição estrutural registrada em `I-16` no
workspace, não uma novidade deste repositório.

## O schema não é aplicado por este apply

Não há Flyway neste serviço. Depois do `apply`, o banco está vazio e a aplicação sobe em
falha, porque `ddl-auto` é `validate`. Aplicar o baseline é passo do ritual de ambiente:

```bash
scripts/criar-secret-do-banco.sh hml
scripts/aplicar-baseline.sh hml
```

O primeiro cria o Secret do Kubernetes a partir do SSM; o segundo aplica
`db/postgres/*.sql` e `db/mongo/*.js` de dentro do cluster. Os dois são idempotentes e
podem ser repetidos sem efeito colateral.
