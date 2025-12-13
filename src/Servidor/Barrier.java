package Servidor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Barrier {

    private final int participants;
    private int count;
    private int generation = 0; // Para detetar se a barreira já rodou (reutilização)
    private final Lock lock = new ReentrantLock();
    private final Condition trip = lock.newCondition();

    public Barrier(int participants) {
        this.participants = participants;
        this.count = 0;
    }

    public void await(long timeoutSeconds) throws InterruptedException {
        lock.lock();
        try {
            int myGeneration = generation; // Guarda a geração atual localmente
            count++;

            if (count == participants) {
                // Último a chegar: abre a barreira
                count = 0;
                generation++; // Muda a geração para a próxima ronda
                trip.signalAll();
            } else {
                // Espera
                long nanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
                
                // CORREÇÃO CRÍTICA: 'while' em vez de 'if'
                // Verifica a geração para saber se a barreira "rodou" enquanto dormíamos
                while (count < participants && generation == myGeneration) {
                    if (nanos <= 0L) {
                        // Timeout: Opcional - quebrar a barreira ou apenas sair
                        // Aqui forçamos a saída desta thread, mas idealmente tratarias o erro
                        break; 
                    }
                    nanos = trip.awaitNanos(nanos);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}