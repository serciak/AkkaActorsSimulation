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

public class Filtering extends AbstractBehavior<Filtering.Command> {
    public interface Command {}

    public static class AcquireUnfilteredWine implements Command {
        private int unfilteredWine;

        public AcquireUnfilteredWine(int unfilteredWine) {
            this.unfilteredWine = unfilteredWine;
        }
    }

    public static class Start implements Command {}

    public static class FilteringSlotReleased implements Command {
        private int slot;

        public FilteringSlotReleased(int slot) {
            this.slot = slot;
        }
    }

    private ActorRef<Bottling.Command> bottling;

    private int unfilteredWine;
    private int unfilteredWineIn;
    private int wineOut;
    private double failure;
    private int slots;
    private Map<Integer, Boolean> slotsStatus = new HashMap<>();
    private int duration;
    private int acceleration;

    private Filtering(ActorContext<Command> context, ActorRef<Bottling.Command> bottling, int duration, double failure, int acceleration,
                     int slots, int unfilteredWineIn, int wineOut) {
        super(context);
        this.bottling = bottling;
        this.duration = duration;
        this.failure = failure;
        this.acceleration = acceleration;
        this.slots = slots;
        this.unfilteredWineIn = unfilteredWineIn;
        this.wineOut = wineOut;
        unfilteredWine = 0;

        for (int i = 0; i < slots; i++) {
            slotsStatus.put(i, true);
        }
    }

    public static Behavior<Command> create(ActorRef<Bottling.Command> bottling, int duration, double failure, int acceleration,
                                           int slots, int unfilteredWineIn, int wineOut) {
        return Behaviors.setup(context -> new Filtering(context, bottling, duration, failure, acceleration, slots, unfilteredWineIn, wineOut));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AcquireUnfilteredWine.class, this::onAcquireUnfilteredWine)
                .onMessage(Start.class, this::onSlotStart)
                .onMessage(FilteringSlotReleased.class, this::onFilteringSlotReleased)
                .build();
    }

    private Behavior<Command> onAcquireUnfilteredWine(AcquireUnfilteredWine auw) {
        unfilteredWine += auw.unfilteredWine;
        getContext().getSelf().tell(new Start());
        return this;
    }

    private Behavior<Command> onSlotStart(Start start) {
        while(slotsStatus.values().stream().anyMatch(x -> x) && unfilteredWine >= unfilteredWineIn) {
            Random random = new Random();

            int slot = random.nextInt(slots);
            while (!slotsStatus.get(slot)) {
                slot = random.nextInt(slots);
            }

            System.out.println("Unfiltered wine acquired from fermentation: " + unfilteredWineIn + ". On slot: " + slot);
            unfilteredWine -= unfilteredWineIn;

            slotsStatus.put(slot, false);
            getContext().scheduleOnce(Duration.ofMillis(duration / acceleration), getContext().getSelf(), new FilteringSlotReleased(slot));
        }

        if(slotsStatus.values().stream().noneMatch(x -> x)) {
            System.out.println("Filtering slots full");
        }

        return this;
    }

    private Behavior<Command> onFilteringSlotReleased(FilteringSlotReleased fsr) {
        slotsStatus.put(fsr.slot, true);

        if(Math.random() < failure) {
            System.out.println("Filtering process failure on slot: " + fsr.slot);
        }
        else {
            System.out.println("Filtering process done on slot " + fsr.slot + ". Produced wine " + wineOut + " L.");
            bottling.tell(new Bottling.AcquireWine(wineOut));
        }

        getContext().getSelf().tell(new AcquireUnfilteredWine(0));
        return this;
    }
}
