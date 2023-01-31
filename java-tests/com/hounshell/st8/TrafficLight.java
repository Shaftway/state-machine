package com.hounshell.st8;

public interface TrafficLight {

    interface Green extends TrafficLight { }
    interface Yellow extends TrafficLight { }
    interface Red extends TrafficLight { }

    static StateMachine<TrafficLight> getStateMachine() {
        return new StateMachine.Builder<TrafficLight>()
                .addValidTransition(Green.class, Yellow.class)
                .addValidTransition(Yellow.class, Red.class)
                .addValidTransition(Red.class, Green.class)
                .buildWithInitialState(new Green() {});
    }
}
