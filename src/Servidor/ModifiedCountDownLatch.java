package Servidor;

public class ModifiedCountDownLatch {
    
    private final int bonusFactor;      
    private int bonusCount;             
    private final long waitPeriod;     
    private int count;                 
    
   
    public ModifiedCountDownLatch(int bonusFactor, int bonusCount, int waitPeriodInSeconds, int count) {
        this.bonusFactor = bonusFactor;
        this.bonusCount = bonusCount;
        this.waitPeriod = waitPeriodInSeconds * 1000L; // converter para ms
        this.count = count;
    }

    /*
      Chamado pela thread de cada cliente quando envia uma resposta.
      Retorna o fator de multiplicacao (bonusFactor ou 1).
    */
    public synchronized int countDown() {
        int currentBonus = 1;

        if (count > 0) {
            count--;
            
            // verifica se este jogador ainda pode apanhar um bonus
            if (bonusCount > 0) {
                currentBonus = bonusFactor;
                bonusCount--;
            }

            // se foi o ultimo a responder, acorda a thread principal do jogo
            if (count == 0) {
                notifyAll();
            }
        }
        
        return currentBonus;
    }

    /*
      Chamado pela GameThread para esperar pelo fim da ronda.
      Desbloqueia se todos responderem ou se o tempo acabar.
    */
    public synchronized void await() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeElapsed = 0;

        // enquanto faltarem respostas e houver tempo
        while (count > 0 && timeElapsed < waitPeriod) {
            
            // wait recebe o tempo que falta
            wait(waitPeriod - timeElapsed);
            
            // atualiza o tempo passado
            timeElapsed = System.currentTimeMillis() - startTime;
        }
        
        
    }
}