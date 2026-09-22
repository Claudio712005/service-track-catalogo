# k8s/

Manifestos Kubernetes deste microsserviço. GitOps: o ArgoCD sincroniza a partir
daqui, ninguém roda `kubectl apply` na aplicação.

```
k8s/
├── base/                Deployment, Service ClusterIP, namespace
├── overlays/
│   ├── local/            kind, 2 réplicas, sem chave JWT
│   ├── hml/               vazio — fora do escopo desta rodada
│   └── prod/              vazio — fora do escopo desta rodada
└── argocd/
    └── local.yaml         Application do kind, aplicada à mão
```

## Este serviço não é alcançável de fora do cluster

O `Service` é `ClusterIP`, em todos os ambientes. Diferente do monólito principal
(que expõe `NodePort 30080` para o API Gateway alcançar via NLB), este
microsserviço só é consumido por outros serviços dentro do cluster — BFF ou
demais microsserviços. Não há NodePort, Ingress nem LoadBalancer neste
diretório, e nenhum overlay deve introduzir um.

Isso é aplicado por omissão (não existe caminho de entrada externo), não por
NetworkPolicy — o kind usa kindnet, que não impõe NetworkPolicy, e o EKS da
Fase 3 usa o CNI padrão da AWS, que também não impõe sem o add-on Calico. Uma
NetworkPolicy aqui seria decorativa. Se isso importar de verdade num ambiente
gerenciado, é decisão de infraestrutura da plataforma (`aws-iac`), não deste
repositório.

## Perfil local

- **2 réplicas por padrão**, não 1: qualquer teste já observa balanceamento
  entre pods sem precisar disparar um scale primeiro.
- **`kubectl scale deployment/service-track-catalogo -n service-track-catalogo
  --replicas=N`** é o jeito fácil de testar escala horizontal manualmente.
  Não há HPA no perfil local — HPA depende de `metrics-server`, que não está
  instalado no kind por padrão, e sem carga real numa máquina de desenvolvedor
  ele não aciona de forma confiável. HPA fica para quando o overlay de `prd`
  for construído, seguindo o padrão do monólito principal.
- **Requests baixos de propósito** (100m CPU / 320Mi memória): é o que
  realmente limita quantas réplicas cabem num kind rodando no seu laptop.
  Reduzir isso importa mais para "escalar com facilidade" do que ligar HPA.
- **`SPRING_PROFILES_ACTIVE=dev`** desliga a validação de JWT
  (`SecurityConfigDesenvolvimento`, com aviso de log na subida). Evita
  gerenciar par de chaves RS256 só para testar deploy e escala localmente.
  Não usar em `hml` nem `prd`.
- **`imagePullPolicy: IfNotPresent`**: a imagem é local, `kind` não deve tentar
  puxar do registry.

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

O `kind load docker-image` é obrigatório: o kind não enxerga o Docker local do
host, a imagem precisa ser carregada explicitamente no cluster.

Conferir:

```bash
kubectl -n argocd get application service-track-catalogo-local
kubectl -n service-track-catalogo get pods -w
kubectl -n service-track-catalogo port-forward svc/service-track-catalogo 8080:80
```

## Réplicas para outro microsserviço

Copiar este diretório inteiro para o repositório novo e substituir
`service-track-catalogo` por `service-track-<dominio>` em todos os arquivos
— nome do Deployment, do Service, da imagem, do namespace, da ConfigMap e da
`Application` do ArgoCD. É a única mudança necessária; a estrutura, os
probes, os requests e a ausência de exposição externa se aplicam a qualquer
microsserviço interno.

## Pendências conhecidas

- **`project: default`** na `Application` — provisório. Assim que a topologia
  GitOps de múltiplos microsserviços for decidida (`GLOBAL-RFC-009`), isso
  deve migrar para um `AppProject` próprio, análogo ao `service-track` que já
  existe em `aws-iac`.
- **Sem MongoDB local.** O repositório de serviços hoje é
  `ServicoRepositoryMemoriaAdapter`, em memória — o perfil local não precisa
  de banco ainda. Quando a persistência Mongo entrar (fase F1 do plano de
  evolução), este overlay ganha um Deployment de Mongo efêmero, no mesmo
  padrão do `postgres.yaml` do monólito principal.
- **Sem OTLP local.** Nenhum coletor de observabilidade roda no kind hoje;
  `OTEL_JAVAAGENT_ENABLED=false` reflete isso, não uma escolha permanente.
