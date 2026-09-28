# CAT-ADR-001: credencial do banco vem do secret da esteira

## Data
27/09/2026

## Status
Aceita.

---

## Contexto

O RDS deste serviço nasceu com a senha gerada por `random_password` do Terraform e publicada
no SSM. Funciona, mas tem três consequências que só aparecem no uso:

1. **A senha muda a cada recriação do ambiente.** Como os ambientes aqui são destruídos e
   recriados o tempo todo, todo consumidor precisa reler o SSM depois de cada `apply`. Um
   `psql` aberto, um `.env` local, um Secret do Kubernetes já criado: todos ficam inválidos.
2. **Quem opera não escolhe a credencial**, então não há como preparar acesso antes de o
   ambiente existir.
3. A senha fica no state em S3 de qualquer forma — o `random_password` não protege disso.
   É a dívida `I-16` registrada no workspace, e ela vale para os dois desenhos.

## Decisão

`db_username` e `db_password` passam a ser variáveis **sem default**, marcadas como sensíveis,
alimentadas pelos secrets `ST_CAT_DB_USER` e `ST_CAT_DB_PASSWORD` do *environment* do GitHub
(`hml` e `prd`, separados). A esteira `Infra` exporta como `TF_VAR_*`; o Terraform continua
publicando os valores no SSM, que segue sendo a fonte lida em tempo de uso pela aplicação e
pelos scripts.

O provider `random` foi removido.

## Consequências

- A credencial é **estável entre recriações**: o Secret do Kubernetes, o `psql` do ritual e o
  roteiro de demonstração continuam valendo depois de um `destroy` seguido de `apply`.
- `apply` sem os secrets **falha antes** de tocar na AWS, com mensagem dizendo qual secret
  falta. É melhor que descobrir isso vinte minutos depois, no meio da criação do RDS.
- Validação na variável recusa senha fora de 16–128 caracteres e com `/`, `@`, `"`, `'` ou
  espaço, que são os caracteres que o RDS rejeita no usuário mestre. O erro aparece no `plan`,
  não no meio do `apply`.
- **Trocar `ST_CAT_DB_USER` recria a instância**: usuário mestre não é alterável em lugar. A
  senha, sim, muda em lugar.
- O segredo passa a existir em dois lugares: no state em S3 e nos secrets do GitHub. É uma
  superfície a mais do que antes, e aceitável porque o ambiente é educacional e efêmero. Em
  ambiente real o caminho seria o Secrets Manager com rotação, sem senha em variável.
- `apply` local exige exportar as duas variáveis; não há mais caminho que "simplesmente
  funciona" sem credencial declarada. Isso é intencional.

## Alternativas consideradas

| Alternativa | Por que não agora |
|---|---|
| Manter `random_password` | senha muda a cada recriação, que é o problema que motivou esta decisão |
| AWS Secrets Manager com `manage_master_user_password` | é o desenho correto fora daqui, mas custa por segredo e a conta educacional bloqueia parte das ações de IAM necessárias |
| Senha em arquivo `.tfvars` | `*.tfvars` é ignorado pelo git de propósito; obrigaria a distribuir o arquivo por fora, sem controle de acesso |
