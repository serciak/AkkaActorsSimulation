package org.example;
import akka.actor.typed.ActorSystem;

public class Main {
    public static void main(String[] args) {
        ActorSystem<Warehouse.Command> warehouse = ActorSystem.create(Warehouse.start(), "warehouse");
        warehouse.tell(new Warehouse.Init(60, 100, 10, 200, 100));
        warehouse.tell(new Warehouse.Start());
    }
}