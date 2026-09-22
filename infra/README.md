# infra/

Infraestrutura AWS própria do microsserviço. Não depende do `service-track-aws-iac`
e o `service-track-aws-iac` não sabe que ela existe.

| Recurso | Nome |
|---|---|
| ECR | `servicetrack-<ambiente>-catalogo` |
| Parâmetro SSM com a URL do ECR | `/servicetrack/<ambiente>/catalogo/ecr-url` |

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
