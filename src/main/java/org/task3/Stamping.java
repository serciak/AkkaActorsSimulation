package org.task3;

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

public class Stamping extends AbstractBehavior<Stamping.Command> {

    public interface Command {}

    public static class AcquireGrapes implements Command {}

    public static class StampingSlotReleased implements Command {
        private int slot;

        public StampingSlotReleased(int slot) {
            this.slot = slot;
        }
    }

    public static class Stop implements Command {}

    private ActorRef<Fermentation.Command> fermentation;

    private int grapesIn;
    private int grapeJuiceOut;
    private int totalAcquiredGrapes;
    private int grapesLeft;
    private double failure;
    private int slots;
    private Map<Integer, Boolean> slotsStatus = new HashMap<>();
    private int duration;
    private int acceleration;

    private Stamping(ActorContext<Command> context, ActorRef<Fermentation.Command> fermentation, int duration, double failure, int acceleration, int slots, int grapesIn, int grapeJuiceOut, int grapes) {
        super(context);
        this.fermentation = fermentation;
        this.acceleration = acceleration;
        this.slots = slots;
        this.duration = duration;
        this.failure = failure;
        this.grapesIn = grapesIn;
        this.grapeJuiceOut = grapeJuiceOut;
        this.grapesLeft = grapes;
        totalAcquiredGrapes = 0;

        for (int i = 0; i < slots; i++) {
            slotsStatus.put(i, true);
        }
    }

    public static Behavior<Stamping.Command> create(ActorRef<Fermentation.Command> fermentation, int duration, double failure, int acceleration, int slots, int grapesIn, int grapeJuiceOut, int grapes) {
        return Behaviors.setup(context -> new Stamping(context, fermentation, duration, failure, acceleration, slots, grapesIn, grapeJuiceOut, grapes));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(AcquireGrapes.class, this::onAcquireGrapes)
                .onMessage(StampingSlotReleased.class, this::onStampingSlotReleased)
                .build();
    }

    private Behavior<Command> onAcquireGrapes(AcquireGrapes acquireGrapes) {
        while(slotsStatus.values().stream().anyMatch(x -> x) && grapesLeft >= grapesIn) {
            Random random = new Random();

            int slot = random.nextInt(slots);
            while (!slotsStatus.get(slot)) {
                slot = random.nextInt(slots);
            }

            System.out.println("Grapes acquired from warehouse: " + grapesIn + ". On slot: " + slot);
            grapesLeft -= grapesIn;

            slotsStatus.put(slot, false);
            getContext().scheduleOnce(Duration.ofMillis(duration / acceleration), getContext().getSelf(), new StampingSlotReleased(slot));
        }

        if(slotsStatus.values().stream().noneMatch(x -> x)) {
            System.out.println("Stamping slots full");
        }

        return this;
    }

    private Behavior<Command> onStampingSlotReleased(StampingSlotReleased ssr) {
        slotsStatus.put(ssr.slot, true);

        if(Math.random() < failure) {
            System.out.println("Stamping process failure on slot: " + ssr.slot);
        }
        else {
            System.out.println("Stamping process done on slot " + ssr.slot + ". Produced juice " + grapeJuiceOut + " L.");
            fermentation.tell(new Fermentation.AcquireGrapeJuice(grapeJuiceOut));
        }

        getContext().getSelf().tell(new AcquireGrapes());
        return this;
    }
}
