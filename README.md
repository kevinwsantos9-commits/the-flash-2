# Flash Mod 1.1 (Fabric 1.21.1)

Mod de velocista para Minecraft 1.21.1 (Fabric, Java 21).

## Controles (mudam em Opções > Controles > Flash Mod)

| Tecla | Ação |
|-------|------|
| V | Liga/desliga a Força da Velocidade (sem limite de tempo) |
| B / N | Aumenta / diminui o nível de velocidade (segure para repetir), de 1 a 20 |
| C | Liga/desliga atravessar paredes |
| K | Menu da cor do raio |

## O que tem

- **20 níveis de velocidade**, mostrados em uma barra de 20 quadradinhos (cada quadrado = 1 ponto).
- **Raios de verdade** desenhados em 3D (tortos, com brilho e ramificações) em volta e atrás do jogador,
  na cor escolhida. Os outros jogadores também veem.
- **Nível 5+:** anda sobre a água (agache para afundar, pule para sair).
- **Nível 10+:** faíscas de velocidade voando em volta do jogador (mais faíscas a cada nível).
- **Atravessar paredes:** o corpo passa pelos blocos e o modelo do jogador vibra.
- **Correr na parede:** andando para frente contra um bloco, você sobe.
- Sem dano de queda enquanto o poder está ligado.
- Menu de cor com cores prontas, sliders RGB, código hex, arco-íris e prévia do raio.

## Importante

- Para os outros jogadores verem os raios e para atravessar paredes funcionar em servidor,
  o mod precisa estar instalado no servidor também.
- Em servidor dedicado, `allow-flight=true` evita ser expulso por "voar" ao andar na água ou escalar.
- Dentro de blocos, pular sobe e agachar desce. Não dá para desligar o poder com o corpo dentro de blocos.

## Como compilar pelo GitHub Actions

1. Envie todos os arquivos desta pasta para um repositório no GitHub.
2. Aba **Actions** > **Build Flash Mod** (ou faça um push).
3. Baixe o artefato **flashmod** e use o `flashmod-1.1.0.jar` (não o `-sources`) na pasta `mods`,
   junto com o **Fabric API** para 1.21.1.

## Ajustes rápidos

- Velocidades: `BASE_SPEED` e `STEP_SPEED` em `client/FlashClientState.java`
  (nível N anda `BASE_SPEED + STEP_SPEED * (N - 1)` blocos por tick).
- Níveis de água e faíscas: `WATER_LEVEL` e `SPARKS_LEVEL` no mesmo arquivo.
- Tamanho e quantidade dos raios: `client/FlashRenderer.java`.
