package org.example;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.text.Format;
import java.time.Duration;
import java.time.Instant;

public class Warehouse extends AbstractBehavior<Warehouse.Command> {

    public interface Command {}

    public static class Init implements Command {
        private final int grapes;
        private final int water;
        private final int bottles;
        private final int sugar;
        private final int acceleration;

        public Init(int grapes, int water, int bottles, int sugar, int acceleration) {
            this.grapes = grapes;
            this.water = water;
            this.bottles = bottles;
            this.sugar = sugar;
            this.acceleration = acceleration;
        }
    }

    public static class Start implements Command {}

    public static class Stop implements Command {}

    public static class AcquireBottledWine implements Command {
        private int bottledWine;

        public AcquireBottledWine(int bottledWine) {
            this.bottledWine = bottledWine;
        }
    }

    public static Behavior<Command> start() {
        return Behaviors.setup(Warehouse::new);
    }

    private ActorRef<Stamping.Command> stamping;
    private ActorRef<Fermentation.Command> fermentation;
    private ActorRef<Filtering.Command> filtering;
    private ActorRef<Bottling.Command> bottling;

    private int grapes;
    private int water;
    private int bottles;
    private int sugar;
    private int grapeJuice;
    private int bottledWine;

    private Instant startTime;
    private Instant endTime;
    private int acceleration;

    public Warehouse(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Init.class, this::onInit)
                .onMessage(Start.class, this::onStart)
                .onMessage(Stop.class, stop -> onStop())
                .onMessage(AcquireBottledWine.class, this::onAcquireBottledWine)
                .build();
    }

    private Behavior<Command> onInit(Init init) {
        grapes = init.grapes;
        water = init.water;
        bottles = init.bottles;
        sugar = init.sugar;
        acceleration = init.acceleration;
        grapeJuice = 0;

        bottling = getContext().spawn(Bottling.create(getContext().getSelf(), 5000, 0.05, acceleration, 1, 1, 1, 1, bottles), "bottling");
        filtering = getContext().spawn(Filtering.create(bottling, 12000, 0, acceleration, 3, 25, 24), "filtering");
        fermentation = getContext().spawn(Fermentation.create(filtering, 300000, 0.10, acceleration, 10, 15, 8, 2, 25, water, sugar), "fermentation");
        stamping = getContext().spawn(Stamping.create(fermentation, 12000, 0, acceleration, 1, 15, 10, grapes), "stamping");
        return this;
    }

    private Behavior<Command> onStart(Start start) {
        startTime = Instant.now();
        stamping.tell(new Stamping.AcquireGrapes());
        return this;
    }

    private Behavior<Command> onAcquireBottledWine(AcquireBottledWine abw) {
        bottledWine += abw.bottledWine;
        return this;
    }

    private Behavior<Command> onStop() {
        endTime = Instant.now();
        System.out.println("Process has been finished, Summary: Created new Wine: " +  bottledWine + " Time: " + Duration.between(startTime, endTime).toMillis() * acceleration);

        return Behaviors.stopped();
    }
}
