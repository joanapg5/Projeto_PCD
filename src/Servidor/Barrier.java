package Servidor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Barrier {

    private final int participants;
    private int count;
    private final Lock lock = new ReentrantLock();
    private final Condition barrierOpen = lock.newCondition();

    public Barrier(int participants) {
        this.participants = participants;
        this.count = 0;
    }

    public void await(long timeoutSeconds) throws InterruptedException {
        lock.lock();
        try {
            count++;

            if (count == participants) {
                barrierOpen.signalAll(); //se for o ultimo a chegar, acorda todos
            } else {
                long nanos = TimeUnit.SECONDS.toNanos(timeoutSeconds); //tempo maximo que vai ficar a dormir
                while (count < participants) {
                    if (nanos <= 0L) {
                        break; //sai do loop se der timeout
                    }
                    nanos = barrierOpen.awaitNanos(nanos); //a funcao devolve o tempo que ainda falta esperar caso a thread acorde por falso sinal 
                    										//e assim volta a dormir se ainda nao chegaram todos
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    
}