package com.hounshell.st8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;

public class StateMachineTest {
    private static class TestCallback<From, To> implements Callback<From, To> {
        public boolean wasCalled = false;
        public int callCount = 0;
        private final Callback<From, To> moreTests;

        private TestCallback() {
            this(null);
        }

        private TestCallback(Callback<From, To> moreTests) {
            this.moreTests = moreTests;
        }

        @Override
        public void onStateChanged(From from, To to) {
            wasCalled = true;
            callCount += 1;
            if (moreTests != null)
                moreTests.onStateChanged(from, to);
        }
    }

    @Test
    public void validTransitionWithoutCallback() {
        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();

        assertTrue(stateMachine.getCurrentState() instanceof TrafficLight.Green);
        stateMachine.transition(new TrafficLight.Yellow() {});
        assertTrue(stateMachine.getCurrentState() instanceof TrafficLight.Yellow);
    }

    @Test
    public void validTransitionWithCallback() {
        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback = new TestCallback<>();

        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();
        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback);

        stateMachine.transition(new TrafficLight.Yellow() {});

        assertTrue(callback.wasCalled);
    }

    @Test
    public void validTransitionWithMultipleCallbacksInOrder() {
        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback3 = new TestCallback<>();
        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback2 = new TestCallback<>(
                (from, to) -> assertFalse(callback3.wasCalled));

        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback1 = new TestCallback<>(
                (from, to) -> assertFalse(callback2.wasCalled));

        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();
        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback1);
        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback2);
        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback3);

        stateMachine.transition(new TrafficLight.Yellow() {});

        assertTrue(callback1.wasCalled);
        assertTrue(callback2.wasCalled);
        assertTrue(callback3.wasCalled);
    }

    @Test
    public void validTransitionWithRemovedCallback() {
        TestCallback<TrafficLight, TrafficLight> callback1 = new TestCallback<>();
        TestCallback<TrafficLight, TrafficLight> callback2 = new TestCallback<>();

        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();
        stateMachine.addCallbackForAnything(callback1);
        CallbackToken token = stateMachine.addCallbackForAnything(callback2);
        stateMachine.removeCallback(token);
        stateMachine.transition(new TrafficLight.Yellow() {});

        assertTrue(callback1.wasCalled);
        assertFalse(callback2.wasCalled);
    }

    @Test
    public void validTransitionWithRemovedCallbackInCallback() {
        ArrayList<CallbackToken> tokens = new ArrayList<>();
        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();

        TestCallback<TrafficLight, TrafficLight> callback1 = new TestCallback<>((from, to) -> {
            for (CallbackToken token : tokens) {
                stateMachine.removeCallback(token);
            }
        });
        TestCallback<TrafficLight, TrafficLight> callback2 = new TestCallback<>((from, to) -> {
            for (CallbackToken token : tokens) {
                stateMachine.removeCallback(token);
            }
        });
        TestCallback<TrafficLight, TrafficLight> callback3 = new TestCallback<>();

        tokens.add(stateMachine.addCallbackForAnything(callback1));
        tokens.add(stateMachine.addCallbackForAnything(callback2));
        stateMachine.addCallbackForAnything(callback3);

        stateMachine.transition(new TrafficLight.Yellow() {});
        stateMachine.transition(new TrafficLight.Red() {});

        assertEquals(1, callback1.callCount);
        assertEquals(1, callback2.callCount);
        assertEquals(2, callback3.callCount);
    }

    @Test
    public void invalidTransition() {
        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();

        assertTrue(stateMachine.getCurrentState() instanceof TrafficLight.Green);
        assertThrows(
                InvalidTransitionException.class,
                () -> stateMachine.transition(new TrafficLight.Red() {}));
    }

    @Test
    public void oneQueuedTransition() {
        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();

        TestCallback<TrafficLight.Yellow, TrafficLight.Red> callback1 = new TestCallback<>();
        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback2 = new TestCallback<>(
                (from, to) -> {
                    assertFalse(stateMachine.isNextStateQueued());
                    stateMachine.transition(new TrafficLight.Red() {});
                    assertTrue(stateMachine.isNextStateQueued());
                });

        stateMachine.addCallback(TrafficLight.Yellow.class, TrafficLight.Red.class, callback1);
        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback2);

        assertTrue(stateMachine.getCurrentState() instanceof TrafficLight.Green);
        stateMachine.transition(new TrafficLight.Yellow() {});
        assertTrue(stateMachine.getCurrentState() instanceof TrafficLight.Red);

        assertTrue(callback1.wasCalled);
        assertTrue(callback2.wasCalled);
    }

    @Test
    public void twoQueuedTransition() {
        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();

        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback1 = new TestCallback<>(
                (from, to) -> stateMachine.transition(new TrafficLight.Red() {}));

        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback2 = new TestCallback<>(
                (from, to) -> assertThrows(
                        TransitionContentionException.class,
                        () -> stateMachine.transition(new TrafficLight.Red() {})));

        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback1);
        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback2);

        stateMachine.transition(new TrafficLight.Yellow() {});
    }

    @Test
    public void toAnythingValid() {
        // Yellow -> Green
        StateMachine<EuropeanTrafficLight> stateMachine1 = EuropeanTrafficLight.getStateMachine(
                new EuropeanTrafficLight.Yellow() {});
        stateMachine1.transition(new EuropeanTrafficLight.Green() {});
        assertTrue(stateMachine1.getCurrentState() instanceof EuropeanTrafficLight.Green);

        // Yellow -> Red
        StateMachine<EuropeanTrafficLight> stateMachine2 = EuropeanTrafficLight.getStateMachine(
                new EuropeanTrafficLight.Yellow() {});
        stateMachine2.transition(new EuropeanTrafficLight.Red() {});
        assertTrue(stateMachine2.getCurrentState() instanceof EuropeanTrafficLight.Red);

        // Yellow -> Different Yellow
        EuropeanTrafficLight.Yellow firstYellow = new EuropeanTrafficLight.Yellow() {};
        StateMachine<EuropeanTrafficLight> stateMachine3 = EuropeanTrafficLight.getStateMachine(
                firstYellow);
        stateMachine3.transition(new EuropeanTrafficLight.Yellow() {});
        assertTrue(stateMachine3.getCurrentState() instanceof EuropeanTrafficLight.Yellow);
        assertNotEquals(firstYellow, stateMachine3.getCurrentState());
    }

    @Test
    public void fromAnythingValid() {
        // Green -> Yellow
        StateMachine<EuropeanTrafficLight> stateMachine1 = EuropeanTrafficLight.getStateMachine(
                new EuropeanTrafficLight.Green() {});
        stateMachine1.transition(new EuropeanTrafficLight.Yellow() {});
        assertTrue(stateMachine1.getCurrentState() instanceof EuropeanTrafficLight.Yellow);

        // Red -> Yellow
        StateMachine<EuropeanTrafficLight> stateMachine2 = EuropeanTrafficLight.getStateMachine(
                new EuropeanTrafficLight.Red() {});
        stateMachine2.transition(new EuropeanTrafficLight.Yellow() {});
        assertTrue(stateMachine2.getCurrentState() instanceof EuropeanTrafficLight.Yellow);

        // Yellow -> Different Yellow
        EuropeanTrafficLight.Yellow firstYellow = new EuropeanTrafficLight.Yellow() {};
        StateMachine<EuropeanTrafficLight> stateMachine3 = EuropeanTrafficLight.getStateMachine(
                firstYellow);

        stateMachine3.transition(new EuropeanTrafficLight.Yellow() {});
        assertTrue(stateMachine3.getCurrentState() instanceof EuropeanTrafficLight.Yellow);
        assertNotEquals(firstYellow, stateMachine3.getCurrentState());
    }

    @Test
    public void exceptionBubblesUp() {
        StateMachine<TrafficLight> stateMachine = TrafficLight.getStateMachine();

        TestCallback<TrafficLight.Green, TrafficLight.Yellow> callback1 = new TestCallback<>(
                (from, to) -> stateMachine.transition(new TrafficLight.Red() {}));

        TestCallback<TrafficLight.Yellow, TrafficLight.Red> callback2 = new TestCallback<>(
                (from, to) -> {
                    throw new IllegalStateException();
                });

        stateMachine.addCallback(TrafficLight.Green.class, TrafficLight.Yellow.class, callback1);
        stateMachine.addCallback(TrafficLight.Yellow.class, TrafficLight.Red.class, callback2);

        assertThrows(
                IllegalStateException.class,
                () -> stateMachine.transition(new TrafficLight.Yellow() {}));
    }
}
