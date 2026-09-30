# CAT-ADR-002: o registro da imagem é escrito pela esteira, não pelo repositório

## Data
29/09/2026

## Status
Aceita.

---

## Contexto

O overlay de cada ambiente aponta a imagem por `images[].newName`, e a URL de um repositório
ECR tem o formato `<conta>.dkr.ecr.<regiao>.amazonaws.com/<repositorio>`. Ou seja: o overlay
carregava o identificador da conta AWS dentro de um arquivo versionado.

A conta é de laboratório educacional e muda. Quando mudou, o overlay passou a apontar para o
ECR de uma conta à qual o cluster não tem acesso — e o sintoma é `ImagePullBackOff`, que é
exatamente o mesmo sintoma do estado normal de um ambiente recém-criado. Erro que se disfarça
de comportamento esperado é o pior tipo de erro.

O `newTag` já era escrito pela esteira CD. O `newName` não, apesar de a esteira **já ler** a URL
do ECR em `/servicetrack/<ambiente>/catalogo/ecr-url` para fazer o `docker push`.

## Decisão

A esteira CD passa a escrever **as duas linhas** do overlay, `newName` e `newTag`, a partir do
que ela já tem em mãos: o `newName` vem do parâmetro do SSM publicado pelo `infra/terraform`, e
o `newTag` continua vindo do commit.

No repositório, `newName` fica como `ecr-do-ambiente/servicetrack-<ambiente>-catalogo` —
propositalmente inválido, para que nenhum ambiente suba a partir do valor versionado.

## Consequências

- **Trocar de conta AWS não exige alteração neste repositório.** A primeira execução do CD no
  ambiente novo corrige o registro junto com a tag.
- O valor versionado passa a ser um marcador, não uma configuração. `kustomize build` continua
  funcionando; o que não funciona é o `pull`, e é essa a intenção.
- Depois da primeira execução do CD, o registro real fica no histórico — escrito pela esteira,
  em pull request, nunca à mão. Numa conta nova ele fica obsoleto por uma janela: entre a
  criação do ambiente e o primeiro CD, o pod fica em `ImagePullBackOff`, que já era o estado
  esperado nessa janela.
- Quem aplica o overlay à mão no `local` não é afetado: o overlay de `local` usa imagem local,
  sem ECR.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Manter a URL completa versionada | volta a exigir um pull request mecânico a cada troca de conta, com sintoma indistinguível do estado normal |
| Substituir o registro por um `ConfigMap` lido em runtime | o Kubernetes resolve a imagem antes de qualquer configuração da aplicação; não há como |
| `ImagePullSecrets` apontando para um proxy fixo | acrescenta componente para esconder o problema em vez de resolvê-lo |
| Deixar o ArgoCD reescrever a imagem por plugin | plugin de kustomize no ArgoCD é peça nova para manter, e tira do pull request a visibilidade da versão que subiu |
