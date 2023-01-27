package org.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Bottling extends AbstractBehavior<Bottling.Command> {
    public interface Command {}

    public static class AcquireWine implements Command {
        private int wine;

        public AcquireWine(int wine) {
            this.wine = wine;
        }
    }

    public static class Start implements Command {}

    public static class BottlingSlotReleased implements Command {
        private int slot;

        public BottlingSlotReleased(int slot) {
            this.slot = slot;
        }
    }

    private ActorRef<Warehouse.Command> warehouse;

    private int wine;
    private int wineIn;
    private int bottlesIn;
    private int bottles;
    private int bottledWineOut;
    private double failure;
    private int slots;
    private Map<Integer, Boolean> slotsStatus = new HashMap<>();
    private int duration;
    private int acceleration;

    private Bottling(ActorContext<Command> context, ActorRef<Warehouse.Command> warehouse, int duration, double failure, int acceleration,
                    int slots, int wineIn, int bottlesIn, int bottledWineOut, int bottles) {
        super(context);
        this.warehouse = warehouse;
        this.duration = duration;
        this.failure = failure;
        this.acceleration = acceleration;
        this.slots = slots;
        this.wineIn = wineIn;
        this.bottlesIn = bottlesIn;
        this.bottledWineOut = bottledWineOut;
        this.bottles = bottles;
        wine = 0;

        for (int i = 0; i < slots; i++) {
            slotsStatus.put(i, true);
        }
    }

    public static Behavior<Command> create(ActorRef<Warehouse.Command> warehouse, int duration, double failure, int acceleration,
                                           int slots, int wineIn, int bottlesIn, int bottledWineOut, int bottles) {
        return Behaviors.setup(context -> new Bottling(context, warehouse, duration, failure, acceleration, slots, wineIn, bottlesIn, bottledWineOut, bottles));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AcquireWine.class, this::onAcquireWine)
                .onMessage(Start.class, this::onSlotStart)
                .onMessage(BottlingSlotReleased.class, this::onBottlingSlotReleased)
                .build();
    }

    private Behavior<Command> onAcquireWine(AcquireWine aw) {
        wine += aw.wine;
        getContext().getSelf().tell(new Start());
        return this;
    }

    private Behavior<Command> onSlotStart(Start start) {
        while(slotsStatus.values().stream().anyMatch(x -> x) && wine >= wineIn && bottles >= bottlesIn) {
            Random random = new Random();

            int slot = random.nextInt(slots);
            while (!slotsStatus.get(slot)) {
                slot = random.nextInt(slots);
            }

            System.out.println("Wine acquired from filtration: " + wineIn + ". On slot: " + slot);
            wine -= wineIn;
            bottles -= bottlesIn;

            slotsStatus.put(slot, false);
            getContext().scheduleOnce(Duration.ofMillis(duration / acceleration), getContext().getSelf(), new BottlingSlotReleased(slot));
        }

        if(slotsStatus.values().stream().noneMatch(x -> x)) {
            System.out.println("Filtering slots full");
        }

        return this;
    }

    private Behavior<Command> onBottlingSlotReleased(BottlingSlotReleased bsr) {
        slotsStatus.put(bsr.slot, true);

        if(Math.random() < failure) {
            System.out.println("Bottling process failure on slot: " + bsr.slot);
        }
        else {
            System.out.println("Bottling process done on slot " + bsr.slot + ". Produced bottles of wine " + bottledWineOut + ".");
            warehouse.tell(new Warehouse.AcquireBottledWine(bottledWineOut));
        }

        if(slotsStatus.values().stream().anyMatch(x -> x) && (wine < wineIn || bottles < bottlesIn)) {
            warehouse.tell(new Warehouse.Stop());
        }
        getContext().getSelf().tell(new AcquireWine(0));
        return this;
    }
}
