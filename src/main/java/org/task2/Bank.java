package org.task2;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class Bank {
    private int accountsNum;
    private int initialBalance;
    private int simulationTime;
    private int maxTransactionAmount;
    private int[] accounts;
    private final ReentrantLock lock;
    private boolean running;

    private long deposits;
    private long withdraws;
    private long transfers;
    private long failedWithdraws;
    private long failedTransfers;

    public Bank(int accountsNum, int initialBalance, int simulationTime, int maxTransactionAmount) {
        this.accountsNum = accountsNum;
        this.initialBalance = initialBalance;
        this.simulationTime = simulationTime;
        this.maxTransactionAmount = maxTransactionAmount;
        accounts = new int[accountsNum];
        this.lock = new ReentrantLock();

        deposits = 0;
        withdraws = 0;
        transfers = 0;
        failedWithdraws = 0;
        failedTransfers = 0;
    }

    public void simulate() throws InterruptedException {
        for(int i = 0; i < accountsNum; i++) {
            accounts[i] = initialBalance;
        }

        running = true;

        for(int i = 0; i < accountsNum; i++) {
            new Thread(new Client()).start();
        }

        Thread.sleep(simulationTime);
        running = false;

        System.out.println("Final balance:");
        for(int i = 0; i < accountsNum; i++) {
            System.out.println("Account " + i + ": " + accounts[i] + "$");
        }

        System.out.println("\nDeposits: " + deposits);
        System.out.println("\nWithdraws: " + withdraws);
        System.out.println("Failed withdraws: " + failedWithdraws);
        System.out.println("\nTransfers: " + transfers);
        System.out.println("Failed transfers: " + failedTransfers);
    }

    private class Client implements Runnable {

        @Override
        public void run() {
            while(running) {
                int operation = ThreadLocalRandom.current().nextInt(3);
                int account, toAccount, amount;

                switch (operation) {
                    case 0 -> {
                        account = ThreadLocalRandom.current().nextInt(accountsNum);
                        amount = ThreadLocalRandom.current().nextInt(maxTransactionAmount);
                        lock.lock();

                        try {
                            if(accounts[account] >= amount) {
                                accounts[account] -= amount;
                                withdraws++;
                            }
                            else {
                                failedWithdraws++;
                            }
                        } finally {
                            lock.unlock();
                        }
                    }

                    case 1 -> {
                        account = ThreadLocalRandom.current().nextInt(accountsNum);
                        amount = ThreadLocalRandom.current().nextInt(maxTransactionAmount);
                        lock.lock();

                        try {
                            accounts[account] += amount;
                            deposits++;
                        } finally {
                            lock.unlock();
                        }
                    }

                    case 2 -> {
                        account = ThreadLocalRandom.current().nextInt(accountsNum);
                        toAccount = ThreadLocalRandom.current().nextInt(accountsNum + (accountsNum/2));
                        amount = ThreadLocalRandom.current().nextInt(maxTransactionAmount);
                        lock.lock();

                        try {
                            if(account != toAccount && accounts[account] >= amount && accountsNum > toAccount) {
                                accounts[account] -= amount;
                                accounts[toAccount] += amount;
                                transfers++;
                            }
                            else {
                                failedTransfers++;
                            }
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            }
        }
    }
}
