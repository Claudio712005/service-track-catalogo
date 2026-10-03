# componentes/nodeport

Service `type=NodePort` na porta **30080**, que é como o NLB interno da plataforma alcança este
serviço. Caminho completo:

```
BFF -> API Gateway privada -> VPC Link -> NLB interno -> NodePort 30080 -> pod
```

## A porta é um contrato entre dois repositórios

A outra metade está em `service-track-aws-iac`, em
`apis/service-track-api-int/servicos-<ENV>.yaml`, e precisa declarar **a mesma porta**
(`IAC-ADR-033`).

**Nada valida esse par automaticamente.** Porta divergente não falha nenhum apply nem nenhuma
sincronização do ArgoCD: aparece como alvo `unhealthy` no target group do NLB, e a rota
`/catalogo/...` responde `503`. Ao mudar a porta aqui, mude lá no mesmo momento.

## Isto não abre o serviço para a internet

O NodePort só é alcançável de dentro da VPC, e o security group dos nós aceita tráfego nessa
porta **apenas** do security group do NLB interno — regra criada pelo módulo `api-interna`. Sem
a plataforma com `habilitar_api_interna = true`, a porta existe no nó e ninguém a alcança.

O `Service` ClusterIP do `base/` continua existindo e é o caminho de dentro do cluster.

Não entra no overlay `local`: `kind` não tem NLB nem API Gateway.
