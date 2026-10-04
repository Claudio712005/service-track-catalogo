# CAT-ADR-004: teto de heap abaixo do limite do contêiner, e HPA só por CPU

## Data
04/10/2026

## Status
Aceita.

---

## Contexto

Em `hml`, com os dois serviços de pé, o HPA de ambos estava preso no máximo de réplicas:

```
catalogo   cpu: 6%/70%   memory: 100%/80%   min=1 max=2 atual=2
usuarios   cpu: 7%/70%   memory: 109%/80%   min=1 max=2 atual=2
```

E o consumo medido estava rente ao limite:

| Pod | Uso | Limite |
|---|---|---|
| `usuarios-veiculos` | **381Mi** | 384Mi |
| `catalogo` | 337Mi | 384Mi |

Usuários estava a **3Mi do OOMKill**, sem ter reiniciado ainda.

### A causa, medida

O `Dockerfile` dos dois serviços declarava:

```
-XX:MaxRAMPercentage=75
-XX:InitialRAMPercentage=50
```

Com limite de 384Mi, conferido dentro do contêiner com `-XX:+PrintFlagsFinal`:

```
InitialHeapSize = 192 Mi
MaxHeapSize     = 288 Mi
```

E o non-heap medido em `/actuator/prometheus` (metaspace, code cache, pilhas, direct buffers):
**140Mi**.

```
288 (heap máximo permitido) + 140 (non-heap) = 428Mi  >  384Mi de limite
```

**A JVM estava autorizada a passar do próprio limite em 44Mi.** Não é limite apertado por
pouco: é configuração aritmeticamente impossível de respeitar.

Reproduzido em `kind`, com a imagem real e 4.800 requisições:

| Configuração | heap pico | non-heap | RSS | limite | reinícios |
|---|---|---|---|---|---|
| 384Mi, MaxRAM 75% | 108Mi | 140Mi | 332Mi | 384Mi | **2, `OOMKilled`, exit 137** |
| 1Gi, MaxRAM 75% | 201Mi | 165Mi | 477Mi | 1Gi | 0 |
| **640Mi, MaxRAM 60%** | 100Mi | 166Mi | **377Mi** | 640Mi | **0** |

A variante do meio mostra por que **subir só o limite não resolve**: com `MaxRAMPercentage=75`
o heap acompanha o limite, e o estouro vai junto — 75% de 640Mi são 480Mi de heap, mais 165Mi
de non-heap, 645Mi outra vez acima. E `InitialRAMPercentage=50` faz o RSS nascer colado no
limite (477Mi num limite de 1Gi), o que por si só inflava a métrica de memória do HPA.

### Sobre o HPA

A utilização de memória do HPA é medida contra **`requests`**, não contra o limite. Com
`requests: 320Mi` e uso real de 337–381Mi, a conta dava 105–119% contra um alvo de 80%: o HPA
pedia o máximo de réplicas para sempre, e `minReplicas: 1` em `hml` era letra morta.

Registrado porque é contraintuitivo: **isso não significa que HPA por memória seja impossível
em JVM.** Medido no `kind` com `requests: 448Mi` e uso de 334Mi, a utilização fica em 74% e o
HPA permanece em 1 réplica, estável. O que fixava no máximo era `requests` abaixo do consumo
real, não a métrica em si.

## Decisão

Três mudanças, que só funcionam juntas:

1. **`MaxRAMPercentage=60` e `InitialRAMPercentage=25`.** O teto de heap passa a caber no
   limite com o non-heap somado, e o processo não nasce pedindo metade do limite.
2. **`requests: 448Mi`, `limits: 640Mi`.** Acima do RSS medido (377Mi), com folga de 263Mi.
3. **Métrica de memória removida do HPA**, em `base` e nos três overlays. Fica CPU a 70%.

O motivo de (3) não é que a métrica não possa funcionar com (2) — pode, a 74% contra 80%. É que
**memória de JVM é praticamente constante**, então não carrega sinal de escala: sobe no
aquecimento e não volta. Seis pontos percentuais de margem até o alvo desaparecem na primeira
dependência nova, e aí o HPA volta a ficar preso no máximo sem ninguém perceber. Métrica que
não varia não serve para decidir escala.

Verificado em `kind`: com CPU apenas, o HPA subiu para 2 sob carga e **voltou para 1** depois da
janela de estabilização — comportamento que a configuração anterior nunca teve.

## Consequências

- **Fim do risco de OOMKill em demonstração.** O pico medido fica em 59% do limite novo.
- **O HPA volta a ser HPA.** Escala por CPU e desce quando a carga passa.
- Soma de `requests` de memória em `hml`, com os dois serviços em 2 réplicas, Mongo e Redis:
  cerca de 2.144Mi contra 7.249Mi alocáveis no nó `t3.large` — 30%. Antes da mudança eram 27%,
  com o nó a 18 de 35 slots de pod. Sobra espaço.
- `IAC-ADR-023`, que dimensiona o nó, não muda.
- **Orçamento de conexão (`DB-ADR-004`) não muda:** `minReplicas`/`maxReplicas` seguem iguais,
  e o teto do orçamento é calculado sobre o máximo.
- **A medição foi feita no `usuarios-veiculos`, não neste serviço.** A aritmética que causa o
  estouro é a mesma — mesma imagem base, mesmas flags, mesmo limite — e em `hml` este serviço
  consumia 337Mi contra os 381Mi do outro, ou seja, é o mais leve dos dois e sobreviveu por
  carga menor, não por estar correto. Os números escolhidos têm margem suficiente para os dois.
  Fica registrado o que é medido e o que é inferido.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Só subir o limite para 640Mi | `MaxRAMPercentage=75` leva o heap junto: 480+165 = 645Mi, estoura de novo |
| Só baixar `MaxRAMPercentage`, mantendo 384Mi | caberia (60% de 384 = 230Mi + 140 = 370Mi), mas com 14Mi de folga — a mesma fragilidade de hoje |
| Fixar `-Xmx` em valor absoluto | quebra quando o limite muda; a percentagem acompanha |
| Manter a métrica de memória e só corrigir `requests` | funciona hoje a 74%/80%, e volta a prender no máximo na primeira dependência nova |
| Trocar `UseSerialGC` por G1 | G1 tem mais overhead de memória; em contêiner pequeno com 2 vCPU o serial é adequado |
| Subir `maxReplicas` em vez de arrumar a memória | mais réplicas presas no máximo, gastando o dobro, sem resolver o OOM |
