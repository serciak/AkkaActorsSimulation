package org.task2;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Bank bank = new Bank(100, 300, 100, 300);
        bank.simulate();
    }
}
