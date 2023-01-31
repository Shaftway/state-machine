package com.hounshell.st8;

public interface EuropeanTrafficLight {

    interface Green extends EuropeanTrafficLight { }
    interface Yellow extends EuropeanTrafficLight { }
    interface Red extends EuropeanTrafficLight { }

    static StateMachine<EuropeanTrafficLight> getStateMachine() {
        return getStateMachine(new Green() {});
    }

    static StateMachine<EuropeanTrafficLight> getStateMachine(EuropeanTrafficLight initialState) {
        return new StateMachine.Builder<EuropeanTrafficLight>()
                .addValidTransitionFromAnythingTo(Yellow.class)
                .addValidTransitionToAnythingFrom(Yellow.class)
                .buildWithInitialState(initialState);
    }
}
