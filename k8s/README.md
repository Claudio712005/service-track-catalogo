# k8s/

Manifestos Kubernetes deste microsserviço. GitOps: o ArgoCD sincroniza a partir
daqui, ninguém roda `kubectl apply` na aplicação.

```
k8s/
├── base/                    Deployment, Service ClusterIP, namespace, HPA
├── componentes/
│   ├── mongo-efemero/       Mongo com emptyDir, só no local
│   ├── postgres-efemero/    Postgres de dado descartável, só no local
│   ├── kafka-efemero/       Broker KRaft de nó único, só no local
├── overlays/
│   ├── local/               kind, imagem local, os três bancos/broker em contêiner
│   ├── hml/                 ECR de hml, HPA 1..2, Mongo no Atlas, Postgres no RDS
│   └── prd/                 ECR de prd, HPA 2..4, Mongo no Atlas, Postgres no RDS
└── argocd/
    ├── local.yaml           Application do kind, aplicada à mão
    ├── hml.yaml             marcador de descoberta
    └── prd.yaml             marcador de descoberta
```

## De onde vem cada dependência

| Dependência | local (kind) | hml e prd |
|---|---|---|
| Postgres `st_cat` | `componentes/postgres-efemero` | RDS do próprio serviço, criado por `infra/terraform` |
| Mongo `ST_INS` | `componentes/mongo-efemero` | **MongoDB Atlas (`GLOBAL-ADR-012`)**, com URI e credencial no `Secret` `service-track-catalogo-mongo`; [detalhes](../README.md#o-mongo-vive-no-atlas-fora-do-cluster) |
| Broker Kafka | `componentes/kafka-efemero` | **broker do `service-track-ordens`**, por FQDN (`GLOBAL-ADR-011`) — este serviço não hospeda broker em ambiente compartilhado |

## Como se chega neste serviço

| De onde | Por onde |
|---|---|
| Outro pod no cluster, inclusive o BFF | `http://service-track-catalogo.service-track-catalogo.svc.cluster.local` |
| Internet, ou qualquer gateway | **de nenhuma forma**, e isso é desenho, não pendência |

**Não há NodePort em `hml` nem `prd` desde 12/10/2026.** A `IAC-ADR-035` removeu a API Gateway
privada e as rotas por microsserviço: só o BFF é alcançável por gateway, e o BFF chama este
serviço pelo DNS do cluster. Um NodePort sem quem o chame é porta aberta no nó sem função, então
saiu — e com ele o par porta/serviço que atravessava dois repositórios e que nada validava.

## O broker não é deste serviço

A mensageria está **ligada** em `hml` e `prd` desde 10/10/2026, contra o broker do
`service-track-ordens` (`GLOBAL-ADR-011`). Quem orquestra a saga hospeda a fila; este serviço
participa.

```
KAFKA_BOOTSTRAP_SERVERS=service-track-ordens-kafka.service-track-ordens.svc.cluster.local:9092
```

**O FQDN não é preciosismo.** Cliente Kafka conecta no bootstrap e **reconecta no endereço que o
broker anuncia**. Enquanto o broker anunciava nome curto, apontar para ele de outro namespace
conectava e travava no metadata, com erro que não diz o que é. Os dois valores — este e o
`KAFKA_ADVERTISED_LISTENERS` no repositório do `ordens` — têm de ser FQDN, e **nada valida o
par**: trocar num repositório não quebra o outro, quebra o apply.

O overlay `local` continua com broker próprio, de propósito: ele existe para subir este
repositório sozinho. Divergência contra a `GLOBAL-RFC-007`, declarada no ADR.

Desligar a mensageria segue possível pela flag, e o serviço sobe e atende HTTP sem consumidor,
publicador nem rotina de expiração.

## Credencial do banco não vem do ConfigMap

`hml` e `prd` leem `ST_CAT_DB_URL`, `ST_CAT_DB_USER` e `ST_CAT_DB_PASSWORD` do Secret
`service-track-catalogo-db`, que **não está neste repositório** e nunca estará. Ele é criado a
partir do SSM, no ritual de subida:

```bash
scripts/criar-secret-do-banco.sh hml
```

O `secretRef` no `base/deployment.yaml` é `optional: true` justamente porque no `local` não
existe Secret: lá as credenciais do Postgres efêmero vêm nas literais do ConfigMap do overlay,
que são descartáveis por definição.

`hml` e `prd` também recebem `MONGODB_URI`, `MONGODB_USERNAME` e
`MONGODB_PASSWORD` do `Secret` `service-track-catalogo-mongo`. Antes do baseline, a esteira
Banco chama `scripts/criar-secret-do-mongo.sh`, que lê
`/servicetrack/<env>/catalogo/mongodb/{uri,username,password}` do SSM. A URI não contém
credencial; o script recusa `@`. No overlay `local`, `MONGODB_URI` vem do
`configMapGenerator`: `mongodb://service-track-catalogo-mongo:27017/ST_INS`, sem autenticação.

## Ordem de subida do ambiente

```
1. rede e EKS            (service-track-aws-iac)
2. esteira Infra         (deste repo: ECR + RDS + SSM, e ela chama a esteira Banco)
3. esteira Banco         (segredos do Postgres e do Atlas via SSM, baseline, restart da app)
4. esteira CD            (imagem no ECR + PR com a nova tag)
5. merge do PR           (ArgoCD sincroniza o overlay do ambiente)
```

Sem Flyway, `ddl-auto: validate` recusa banco vazio: é esperado ver `CrashLoopBackOff` até o
passo 3 ter rodado com o cluster de pé. Não é defeito de manifesto.

A esteira Banco é idempotente e pode ser reexecutada. Em `hml`/`prd`, o cluster Atlas precisa
ser cadastrado manualmente uma vez por ambiente, antes do baseline; ele sobrevive ao `destroy`.
Não há pod de banco Mongo nesses overlays. O baseline usa `kubectl run` com a imagem do
`mongosh`, de dentro do cluster, passando `--username` e `--password`; no `local`, continua
usando `kubectl exec` no pod Mongo.

## Como este serviço chega em hml e prd

```
infra/terraform (deste repo)  ->  ECR do ambiente + SSM com a URL
esteira CD (deste repo)       ->  imagem com a tag do commit + PR trocando newTag
merge na main                 ->  ArgoCD sincroniza k8s/overlays/<ambiente>
```

A `Application` em `hml`/`prd` **não** vem de `k8s/argocd/<ambiente>.yaml`: a esteira do
`aws-iac` usa esse arquivo só como marcador de descoberta e gera a `Application` a partir
do template dela. O conteúdo daqui serve ao fluxo local e mantém o desenho visível.

Enquanto o CD não rodar pela primeira vez, `newName` aponta para `ecr-do-ambiente` e `newTag`
para `bootstrap` — nenhum dos dois existe: os pods ficam em `ImagePullBackOff`. É esperado num
ambiente recém-criado.

**O registro da imagem não está escrito aqui de propósito.** A URL do ECR carrega o
identificador da conta AWS, e essa conta muda a cada laboratório. Quem escreve `newName` e
`newTag` é a esteira CD, com o valor que ela lê do SSM. Ver `CAT-ADR-002` e `IAC-ADR-029`.

Ver os pods no cluster da AWS:

```bash
aws eks update-kubeconfig --name servicetrack-hml --region us-east-1
kubectl -n service-track-catalogo get pods -o wide
kubectl -n argocd get application service-track-catalogo-hml
```

## Este serviço não é alcançável de fora do cluster

O `Service` é `ClusterIP`, em todos os ambientes, inclusive `hml` e `prd`. O API Gateway
não tem backend no cluster, e este microsserviço só é consumido por outros serviços de
dentro do cluster — um BFF ou outro microsserviço. Não há NodePort, Ingress nem
LoadBalancer neste diretório, e nenhum overlay deve introduzir um.

Para testar de fora, na AWS, o caminho é o mesmo do local: `kubectl port-forward`. É você
alcançando o cluster com a sua credencial, não a aplicação exposta na internet.

Isso é aplicado por omissão (não existe caminho de entrada externo), não por
NetworkPolicy — o kind usa kindnet, que não impõe NetworkPolicy, e o EKS da
Fase 3 usa o CNI padrão da AWS, que também não impõe sem o add-on Calico. Uma
NetworkPolicy aqui seria decorativa. Se isso importar de verdade num ambiente
gerenciado, é decisão de infraestrutura da plataforma (`aws-iac`), não deste
repositório.

## Perfil local

- **HPA ativo**, `minReplicas: 2` / `maxReplicas: 3`, CPU alvo `50%` (a
  definição genérica vive em `base/hpa.yaml`; este overlay só ajusta os
  números via `hpa-patch.yaml`). Exige `metrics-server` no cluster — **não é
  instalado pelo bootstrap do `aws-iac`**, é passo manual, um por cluster:

  ```bash
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
  kubectl -n kube-system patch deployment metrics-server --type=json \
    -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
  ```

  O patch de TLS inseguro é específico do `kind`: o certificado do kubelet lá
  dentro é autoassinado, e o `metrics-server` recusa por padrão. Sem ele, o
  deployment sobe mas nunca fica `Ready` — `kubectl -n kube-system logs
  deploy/metrics-server` mostra erro de certificado.

  Testado disparando carga real (dois pods empurrados a mais de 500% do
  request de CPU) — o HPA escalou 2 → 3 em segundos, com o motivo exato no
  `kubectl get events`: `cpu resource utilization (percentage of request)
  above target`. Detalhado, com o que cada campo significa, no documento de
  estudo (ver final deste README).
- **`kubectl scale --replicas=N` não funciona bem com HPA presente** — o
  controller do HPA reconcilia por cima em segundos e desfaz o scale manual.
  Para testar escala manualmente, é mais direto editar `minReplicas`/
  `maxReplicas` no `hpa-patch.yaml`, ou gerar carga de verdade contra o
  serviço (exemplo de comando no documento de estudo).
- **Requests baixos de propósito** (100m CPU / 320Mi memória): é o que
  realmente limita quantas réplicas cabem num kind rodando no seu laptop, e é
  a base sobre a qual o `averageUtilization: 50%` do HPA local foi calibrado
  (50% de 100m = 50m — atingível sem gerar uma carga absurda).
- **`SPRING_PROFILES_ACTIVE=dev`** desliga a validação de JWT
  (`SecurityConfigDev`, com aviso de log na subida). Evita
  gerenciar par de chaves RS256 só para testar deploy e escala localmente.
  Não usar em `hml` nem `prd`.
- **`imagePullPolicy: IfNotPresent`**: a imagem é local, `kind` não deve tentar
  puxar do registry.
- **Mensageria ligada**, contra o broker do `componentes/kafka-efemero`. Os
  tópicos nascem sozinhos (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`, 3 partições),
  o que só é aceitável porque o dado é descartável. Em ambiente de verdade o
  tópico é criado com política explícita, não por acidente do primeiro
  consumidor.

## Subir localmente

O cluster `kind` e o ArgoCD são provisionados pelo repositório
`service-track-aws-iac`, não por este. **Se `kind get clusters` não listar
`service-track`, o cluster ainda não existe** — rode o bootstrap uma vez, a
partir do repositório `service-track-aws-iac`:

```bash
cd ../service-track-aws-iac
kind create cluster --config kubernetes/kind/cluster.yaml
kubectl apply -k kubernetes/argocd/bootstrap
kubectl apply -f kubernetes/argocd/projects/service-track.appproject.yaml
```

Isso cria o cluster e instala o ArgoCD dentro dele — nenhuma aplicação ainda.
O nome do cluster tem que ser exatamente `service-track`: é o que
`kind load docker-image --name service-track` abaixo espera encontrar.

Feito isso uma vez, o ciclo deste repositório é:

```bash
docker build -t servicetrack-catalogo:local software/
kind load docker-image servicetrack-catalogo:local --name service-track

kubectl apply -f k8s/argocd/local.yaml
```

O `kind load docker-image` é obrigatório mesmo com o cluster já existindo: o
kind não enxerga o Docker do host, a imagem precisa ser carregada
explicitamente no nó do cluster a cada rebuild.

Com os pods do Postgres e do Mongo prontos, aplicar o schema — sem isso a
aplicação reinicia em laço, porque `ddl-auto` é `validate`:

```bash
scripts/aplicar-baseline.sh local
```

Conferir:

```bash
kubectl -n argocd get application service-track-catalogo-local
kubectl -n service-track-catalogo get pods -w
kubectl -n service-track-catalogo port-forward svc/service-track-catalogo 18080:80
```

A porta `18080` é só convenção. Clusters kind criados antes da Fase 4 ainda mapeiam
a `8080` do host para o `NodePort 30080` do monólito; nesses, a `8080` já está ocupada.

## Acessar o ArgoCD localmente

Não existe URL fixa: `argocd-server` é `ClusterIP`, sem NodePort, Ingress ou
LoadBalancer — o bootstrap do `aws-iac` não expõe a UI externamente. O acesso
é sempre por `kubectl port-forward`, criado sob demanda.

```bash
kubectl -n argocd port-forward svc/argocd-server 8443:443
```

Com o comando rodando (ele bloqueia o terminal — abra outro para o resto),
acesse:

```
https://localhost:8443
```

O navegador vai reclamar de certificado: é autoassinado, do próprio ArgoCD.
Aceitar o risco e prosseguir é esperado neste ambiente local.

**Usuário:** `admin`. **Senha inicial**, gerada pelo próprio bootstrap e
diferente a cada `kind create cluster`:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d; echo
```

Se esse comando não devolver nada, a senha já foi trocada pela UI em algum
momento — o ArgoCD apaga o secret `argocd-initial-admin-secret` após a
primeira troca. Nesse caso, use a senha que você definiu.

Não versionar a senha em nenhum lugar, nem citá-la em commit ou PR — ela
autentica o ArgoCD deste cluster local, mesmo sendo efêmero.

Depois de logado, a `Application` deste serviço aparece em **Applications →
service-track-catalogo-local**, com os dois pods do perfil local visíveis na
árvore de recursos.

## Comandos do dia a dia

Todos testados contra um cluster local de verdade. Namespace fixo nos
exemplos — exporte uma vez e reaproveite:

```bash
export NS=service-track-catalogo
```

### Primeiro: confirmar o contexto certo

O `kubeconfig` deste projeto acumula os contextos das contas AWS Academy das
Fases 3/4 (`servicetrack-dev`, `-hml`, `-prd`) **junto** com o `kind`. Rodar
um comando sem checar o contexto ativo, contra um EKS que expirou, dá erro
confuso de rede — não fica óbvio que o problema é o alvo errado, não o
cluster local.

```bash
kubectl config current-context
```

Precisa ser `kind-service-track`. Se vier outra coisa:

```bash
kubectl config use-context kind-service-track
```

Se o comando reclamar que o contexto **não existe**, o cluster foi destruído
(sobrevive a `colima stop`, não sobrevive a um reinício da VM ou do host) —
volta para "Subir localmente" acima e recria do zero.

### Cluster

```bash
kubectl cluster-info                 # endereço do control plane
kubectl get nodes -o wide            # nós disponíveis (kind = 1 nó só)
```

### Pods

```bash
kubectl -n $NS get pods -o wide      # lista com IP e node
kubectl -n $NS get pods -w           # acompanha em tempo real (Ctrl+C sai)
kubectl -n $NS get all               # Deployment, ReplicaSet, Service e Pods juntos
```

### Logs

```bash
kubectl -n $NS logs -l app=service-track-catalogo --tail=50 --prefix    # os dois pods, prefixados por nome
kubectl -n $NS logs <nome-do-pod> -f                                    # segue um pod específico
kubectl -n $NS logs <nome-do-pod> --previous                            # log do container anterior, após um crash
```

### Investigar um pod

```bash
kubectl -n $NS describe pod <nome-do-pod>    # eventos de agendamento, probes, restarts
kubectl -n $NS get events --sort-by='.lastTimestamp'   # eventos do namespace inteiro, mais recente por último
```

```bash
kubectl -n $NS top pods    # exige metrics-server — comando de instalação em "Perfil local" acima
```

Sem `metrics-server` instalado, esse comando responde `Metrics API not
available` — não é o pod que está com problema, é a peça de infraestrutura
que falta.

### Mandar requisição para o serviço — que só existe dentro do cluster

Não tem `curl` na imagem da aplicação (só o `wget` do Alpine, herdado da
imagem base). Três formas, da mais simples à mais próxima de produção:

**De dentro do próprio pod, com `wget`:**

```bash
POD=$(kubectl -n $NS get pods -o jsonpath='{.items[0].metadata.name}')
kubectl -n $NS exec "$POD" -- wget -qO- http://localhost:8080/servicos
```

**De um pod efêmero de teste, pelo nome curto do `Service`** — funciona
porque o pod de teste está no mesmo namespace, e é o caminho mais parecido
com o de outro microsserviço chamando este:

```bash
kubectl -n $NS run debug-curl --rm -i --restart=Never --image=curlimages/curl -- \
  curl -s http://service-track-catalogo/servicos
```

O mesmo pod, de **outro** namespace, precisa do nome qualificado
(`<service>.<namespace>.svc.cluster.local`) — é assim que um outro
microsserviço, no namespace dele, vai chamar este:

```bash
kubectl run debug-curl --rm -i --restart=Never --image=curlimages/curl -- \
  curl -s http://service-track-catalogo.service-track-catalogo.svc.cluster.local/servicos
```

**Do seu terminal, fora do cluster**, com `port-forward` — único caso onde
"fora do cluster" faz sentido, porque é você testando, não outro serviço:

```bash
kubectl -n $NS port-forward svc/service-track-catalogo 18080:80
curl -s http://localhost:18080/servicos
```

Lembrete da porta: `18080`, não `8080` — motivo em "Subir localmente" acima.

## Parar a execução local

Três níveis, do mais cirúrgico ao mais completo. Use o primeiro que resolver.

### Só este microsserviço

Remove a `Application`, e com ela — por causa do finalizer
`resources-finalizer.argocd.argoproj.io` no manifesto — todo o namespace que
ela criou: pods, `Service`, `ConfigMap`. Testado: a cascata deixa zero
resíduo, sem precisar de um segundo comando para limpar o namespace.

```bash
kubectl delete -f k8s/argocd/local.yaml
```

O cluster `kind`, o ArgoCD e qualquer outro microsserviço nele continuam de
pé. É o comando do dia a dia — usar antes de reconstruir a imagem já resolve
o que `kind load docker-image` sozinho não limpa (pods antigos).

### O cluster local inteiro

Remove o cluster `kind` inteiro — ArgoCD, todos os microsserviços que estejam
nele, tudo. Não é comando deste repositório (o cluster é provisionado pelo
`aws-iac`), mas documentado aqui por simetria com "Subir localmente":

```bash
kind delete cluster --name service-track
```

Não sobra nada para desfazer depois — nem volume, nem imagem carregada. Da
próxima vez, o bootstrap completo de "Subir localmente" roda do zero,
incluindo `kind load docker-image` de novo (imagem carregada num cluster
antigo não sobrevive à recriação dele).

### Docker por completo

Se não for usar Docker/kind por um tempo, parar a VM libera CPU e memória do
host de vez — relevante aqui porque essa VM já precisou ser realocada de
2 CPU/2 GiB para 6 CPU/8 GiB neste projeto (ver histórico de commits): rodar
cluster + build ao mesmo tempo na alocação pequena sufocava a VM.

```bash
colima stop
```

`colima start` na próxima vez sobe de novo com os mesmos 6 CPU/8 GiB — a
alocação de recursos persiste entre paradas, só a VM em si é desligada.

## Réplicas para outro microsserviço

Copiar este diretório inteiro para o repositório novo e substituir
`service-track-catalogo` por `service-track-<dominio>` em todos os arquivos
— nome do Deployment, do Service, da imagem, do namespace, da ConfigMap e da
`Application` do ArgoCD. É a única mudança necessária; a estrutura, os
probes, os requests e a ausência de exposição externa se aplicam a qualquer
microsserviço interno.

## Banco em hml e prd: MongoDB Atlas

O MongoDB `ST_INS` vive no Atlas M0, fora da AWS, conforme a
`GLOBAL-ADR-012` de **12/10/2026**. Os cadastros sobrevivem ao rollout da aplicação e ao
`destroy` do ambiente. `componentes/mongo-efemero`, com `mongo:7` e `emptyDir`, fica apenas
no `local`, para permitir subir o repositório sozinho, como o broker (`GLOBAL-ADR-011`).

O egresso do cluster passa pelo NAT Gateway, cuja EIP muda a cada recriação. A
`GLOBAL-ADR-012` aceita liberar `0.0.0.0/0` na lista de IPs do Atlas M0, com senha forte e
TLS obrigatório. O baseline roda de dentro do cluster e aproveita a mesma permissão da
aplicação, sem adicionar o IP dinâmico do runner do GitHub.

O grupo `readiness` do Spring **não** inclui o Mongo: o pod fica `Ready` mesmo sem banco, e
só `/actuator/health` (completo) acusa `DOWN`. Por isso a esteira de CI sobe um Mongo ao lado
da imagem e checa o health completo — sem isso o teste de fumaça passaria com o banco
inalcançável.

## Pendências conhecidas

- **`k8s/argocd/<ambiente>.yaml` é marcador de descoberta.** A esteira do
  `aws-iac` procura esse arquivo em cada repositório e gera a `Application`
  a partir de um template próprio — o conteúdo daqui não é o que vai para o
  cluster em `hml`/`prd`. Mantido alinhado (`project: service-track`) para que
  o `kubectl apply` manual do fluxo local produza a mesma coisa.
- **Este serviço não valida JWT, e isso é desenho.** `servicetrack.security.jwt.habilitado`
  é `false` no `application.yaml`. Quem valida token é o BFF, o único alcançável por gateway
  (`IAC-ADR-035`); daqui para dentro o tráfego já passou pela borda. A autenticação em si está
  ligada por padrão no `aws-iac` desde o `IAC-ADR-036`, com emissor na Lambda e chave pública
  no SSM — não é a ausência de emissor que mantém a flag desligada aqui.
- **Sem OTLP local.** Nenhum coletor de observabilidade roda no kind hoje;
  `OTEL_JAVAAGENT_ENABLED=false` reflete isso, não uma escolha permanente.
