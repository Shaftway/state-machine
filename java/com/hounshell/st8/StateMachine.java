package com.hounshell.st8;

import static java.lang.System.lineSeparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class StateMachine<T> {
    private final ArrayList<Transition> validTransitions = new ArrayList<>();
    private final LinkedHashMap<CallbackToken, CallbackWrapper<T, ?, ?>> callbacks = new LinkedHashMap<>();
    private T currentState;
    private T nextState;
    private boolean stateTransitioning = false;

    private StateMachine(ArrayList<Transition> validTransitions, T currentState) {
        this.validTransitions.addAll(validTransitions);
        this.currentState = currentState;
    }

    public T getCurrentState() {
        return currentState;
    }

    public synchronized boolean isNextStateQueued() {
        return nextState != null;
    }

    public synchronized void transition(T to) {
        transition(null, to);
    }

    public synchronized void transition(T from, T to) {
        assert (to != null);

        if (stateTransitioning && nextState != null) {
            throw new TransitionContentionException(currentState, nextState, to);
        }

        if (from != null && from != currentState) return;

        nextState = to;
        if (stateTransitioning) return;

        stateTransitioning = true;

        while (nextState != null) {
            found: {
                for (Transition transition : validTransitions) {
                    if (transition.isValid(currentState, nextState)) {
                        T oldState = currentState;
                        currentState = nextState;
                        nextState = null;

                        // Iterate over a cloned list of callbacks in case they're modified.
                        for (CallbackWrapper<T, ?, ?> callback : new ArrayList<>(callbacks.values())) {
                            callback.handleCall(oldState, currentState);
                        }

                        break found;
                    }
                }

                throw new InvalidTransitionException(currentState, nextState);
            }
        }

        stateTransitioning = false;
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Callback<From, To> callback) {
        CallbackToken token = new CallbackToken() {};
        callbacks.put(token, new CallbackWrapper<T, From, To>(new Transition(from, to), callback));
        return token;
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Callback<T, To> callback) {
        return addCallback(null, to, callback);
    }

    public <From extends T, To extends T> CallbackToken addCallbackToAnythingFrom(
            Class<From> from, Callback<From, T> callback) {
        return addCallback(from, null, callback);
    }

    public CallbackToken addCallbackForAnything(Callback<T, T> callback) {
        return addCallback(null, null, callback);
    }

    public void removeCallback(CallbackToken token) {
        callbacks.remove(token);
    }

    public static class Builder<T> {
        private final ArrayList<Transition> validTransitions = new ArrayList<>();

        public <To extends T> Builder<T> addValidTransitionFromAnythingTo(Class<To> to) {
            return addValidTransition(null, to);
        }

        public <From extends T> Builder<T> addValidTransitionToAnythingFrom(Class<From> from) {
            return addValidTransition(from, null);
        }

        public <From extends T, To extends T> Builder<T> addValidTransition(Class<From> from, Class<To> to) {
            validTransitions.add(new Transition(from, to));
            return this;
        }

        public Builder<T> makeAllTransitionsValid() {
            validTransitions.clear();
            validTransitions.add(new Transition(null, null));
            return this;
        }

        public StateMachine<T> buildWithInitialState(T state) {
            return new StateMachine<T>(validTransitions, state);
        }
    }
}
