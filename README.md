# Jogo isKahoot

Este projeto foi desenvolvido no âmbito da UC Programação Concorrente e Distribuída. Implementa uma plataforma de quizzes estilo Kahoot baseada num modelo Cliente-Servidor.

## Funcionalidades
- **Multijogador em Tempo Real:** Suporte para múltiplas equipas e jogadores em simultâneo através de Sockets Java.
- **Tipos de Rondas:**
  - **Individuais:** Com bónus de pontuação para os primeiros a responder.
  - **Equipa:** Exige coordenação, onde a pontuação é calculada com base no sucesso coletivo.
- **Interface Gráfica (GUI):** Desenvolvida em Swing, com temporizadores e feedback visual de respostas.

## Mecanismos de Concorrência Aplicados
O projeto foca-se na resolução de problemas de sincronização e coordenação de threads:
- **Sincronização:** Uso de `synchronized` e `Locks/Conditions` para gerir o recurso partilhado (`GameState`).
- **Coordenação:**
  - **Barrier:** Implementação personalizada para sincronizar o fim das rondas de equipa.
  - **ModifiedCountDownLatch:** Versão modificada para suportar temporizadores e atribuição de bónus por ordem de chegada.
- **Thread Pool:** Gestão eficiente de múltiplas instâncias de jogos em execução através de uma `GameThreadPool`.

## Como Executar

### 1. Correr o Servidor
### 2. Criar uma sala no Servidor

No terminal do servidor, criar uma sala utilizando o comando:

```bash
new <num_equipas> <jogadores_por_equipa> <num_perguntas>
```

### 3. Iniciar os Clientes

Cada jogador deve iniciar um cliente e ligar-se ao servidor utilizando:

```bash
java Cliente.Client <IP> <Port> <Cod_Sala> <Nome_Equipa> <Username>
```
Quando todos os jogadores se ligarem ao servidor, o jogo inicia automaticamente.


* Trabalho realizado por Joana Guerra, nº 122712 e Artur Nobre, nº 99087
