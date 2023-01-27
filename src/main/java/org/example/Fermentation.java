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

public class Fermentation extends AbstractBehavior<Fermentation.Command> {

    public interface Command {}

    public static class AcquireGrapeJuice implements Command {
        private int grapeJuice;

        public AcquireGrapeJuice(int grapeJuice) {
            this.grapeJuice = grapeJuice;
        }
    }

    public static class Start implements Command {}

    public static class FermentationSlotReleased implements Command {
        private int slot;

        public FermentationSlotReleased(int slot) {
            this.slot = slot;
        }
    }

    private ActorRef<Filtering.Command> filtering;

    private int grapeJuice;
    private int grapeJuiceIn;
    private int unfilteredWineOut;
    private int water;
    private int waterIn;
    private int sugar;
    private int sugarIn;
    private double failure;
    private int slots;
    private Map<Integer, Boolean> slotsStatus = new HashMap<>();
    private int duration;
    private int acceleration;

    private Fermentation(ActorContext<Command> context, ActorRef<Filtering.Command> filtering, int duration, double failure, int acceleration,
                        int slots, int grapeJuiceIn, int waterIn, int sugarIn, int unfilteredWineOut, int water, int sugar) {
        super(context);
        this.filtering = filtering;
        this.duration = duration;
        this.failure = failure;
        this.acceleration = acceleration;
        this.slots = slots;
        this.grapeJuiceIn = grapeJuiceIn;
        this.waterIn = waterIn;
        this.sugarIn = sugarIn;
        this.unfilteredWineOut = unfilteredWineOut;
        this.water = water;
        this.sugar = sugar;
        this.grapeJuice = 0;

        for (int i = 0; i < slots; i++) {
            slotsStatus.put(i, true);
        }
    }

    public static Behavior<Fermentation.Command> create(ActorRef<Filtering.Command> filtering, int duration, double failure, int acceleration,
                                                        int slots, int grapeJuiceIn, int waterIn, int sugarIn, int unfilteredWineOut, int water, int sugar) {
        return Behaviors.setup(context -> new Fermentation(context, filtering, duration, failure, acceleration, slots, grapeJuiceIn, waterIn, sugarIn, unfilteredWineOut, water, sugar));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AcquireGrapeJuice.class, this::onAcquireGrapeJuice)
                .onMessage(Start.class, this::onSlotStart)
                .onMessage(FermentationSlotReleased.class, this::onFermentationSlotReleased)
                .build();
    }

    private Behavior<Command> onAcquireGrapeJuice(AcquireGrapeJuice agj) {
        grapeJuice += agj.grapeJuice;
        getContext().getSelf().tell(new Start());
        return this;
    }

    private Behavior<Command> onSlotStart(Start start) {
        while(slotsStatus.values().stream().anyMatch(x -> x) && grapeJuice >= grapeJuiceIn && water >= waterIn && sugar >= sugarIn) {
            Random random = new Random();

            int slot = random.nextInt(slots);
            while (!slotsStatus.get(slot)) {
                slot = random.nextInt(slots);
            }

            System.out.println("Grape juice acquired from stamping: " + grapeJuiceIn + ". On slot: " + slot);
            System.out.println("Water acquired from warehouse: " + waterIn + ". On slot: " + slot);
            System.out.println("Sugar acquired from warehouse: " + sugarIn + ". On slot: " + slot);
            grapeJuice -= grapeJuiceIn;
            water -= waterIn;
            sugar -= sugarIn;

            slotsStatus.put(slot, false);
            getContext().scheduleOnce(Duration.ofMillis(duration / acceleration), getContext().getSelf(), new FermentationSlotReleased(slot));
        }

        if(slotsStatus.values().stream().noneMatch(x -> x)) {
            System.out.println("Fermentation slots full");
        }

        return this;
    }

    private Behavior<Command> onFermentationSlotReleased(FermentationSlotReleased fsr) {
        slotsStatus.put(fsr.slot, true);

        if(Math.random() < failure) {
            System.out.println("Fermentation process failure on slot: " + fsr.slot);
        }
        else {
            System.out.println("Fermentation process done on slot " + fsr.slot + ". Produced unfiltered wine " + unfilteredWineOut + " L.");
            filtering.tell(new Filtering.AcquireUnfilteredWine(unfilteredWineOut));
        }

        getContext().getSelf().tell(new AcquireGrapeJuice(0));
        return this;
    }
}
