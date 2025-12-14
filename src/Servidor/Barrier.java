package Servidor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Barrier {

    private final int participants;
    private int count;
    private int generation = 0; // Para detetar se a barreira jÃ¡ rodou (reutilizaÃ§Ã£o)
    private final Lock lock = new ReentrantLock();
    private final Condition barrierOpen = lock.newCondition();

    public Barrier(int participants) {
        this.participants = participants;
        this.count = 0;
    }

    public void await(long timeoutSeconds) throws InterruptedException {
        lock.lock();
        try {
            int myGeneration = generation; // Guarda a geraÃ§Ã£o atual localmente
            count++;

            if (count == participants) {
                // Ãšltimo a chegar: abre a barreira
                count = 0;
                generation++; // Muda a geraÃ§Ã£o para a prÃ³xima ronda
                barrierOpen.signalAll();
            } else {
                // Espera
                long nanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
                
                // CORREÃ‡ÃƒO CRÃ�TICA: 'while' em vez de 'if'
                // Verifica a geraÃ§Ã£o para saber se a barreira "rodou" enquanto dormÃ­amos
                while (count < participants && generation == myGeneration) {
                    if (nanos <= 0L) {
                        // Timeout: Opcional - quebrar a barreira ou apenas sair
                        // Aqui forÃ§amos a saÃ­da desta thread, mas idealmente tratarias o erro
                        break; 
                    }
                    nanos = barrierOpen.awaitNanos(nanos);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void reset() {
        lock.lock();
        try {
            generation++;     // Muda o "número de série" da ronda
            count = 0;        // Zera o contador de participantes
            barrierOpen.signalAll(); // Acorda qualquer thread perdida da ronda anterior
        } finally {
            lock.unlock();
        }
    }
}