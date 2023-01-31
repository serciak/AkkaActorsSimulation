package org.task1;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    private static final ReentrantLock lock = new ReentrantLock();
    private static boolean increment;
    private static int number;

    public static class Incrementer implements Runnable {

        @Override
        public void run() {
            while(increment) {
                boolean operation = ThreadLocalRandom.current().nextBoolean();
                lock.lock();

                try {
                    if(operation) {
                        number++;
                    }
                    else {
                        --number;
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        increment = true;
        number = 0;
        int threadsNum = 10;
        long incrementingTime = 1000;

        for(int i = 0; i < threadsNum; i++) {
            new Thread(new Incrementer()).start();
        }

        Thread.sleep(incrementingTime);
        increment = false;

        System.out.println(number);
    }
}
