package com.hounshell.st8;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class StateMachine<T> implements ReadableStateMachine<T> {
    private final ArrayList<Transition> validTransitions = new ArrayList<>();
    private final LinkedHashMap<CallbackToken, CallbackWrapper<T, ?, ?>> callbacks = new LinkedHashMap<>();
    private T currentState;
    private T nextState;
    private boolean stateTransitioning = false;

    protected StateMachine(Builder<T> builder, T currentState) {
        this.validTransitions.addAll(builder.validTransitions);
        this.currentState = currentState;
    }

    public T getCurrentState() {
        return currentState;
    }

    public boolean isState(Class<? extends T>... stateClasses) {
        if (stateClasses != null) {
            for (Class<?> clazz : stateClasses) {
                if (clazz.isAssignableFrom(currentState.getClass())) return true;
            }
        }

        return false;
    }

    public synchronized boolean isNextStateQueued() {
        return nextState != null;
    }

    public synchronized void transition(T to) {
        transition(null, to);
    }

    public synchronized boolean transition(T from, T to) {
        // Make sure the state isn't being set to null. Null states are not allowed.
        assert (to != null);

        // If we're mid-transition and the next state is already queued up, fail.
        if (stateTransitioning && nextState != null) {
            throw new TransitionContentionException(currentState, nextState, to);
        }

        // If from was provided, and it doesn't match the current state, ignore this transition.
        if (from != null && from != currentState) return false;

        // If this is not a valid transition, fail.
        if (validTransitions.stream().noneMatch(t -> t.isValid(currentState, to))) {
            throw new InvalidTransitionException(currentState, to);
        }

        // Queue this state up.
        nextState = to;

        // If we're still calling callbacks from a previous transition then trust the queue.
        if (stateTransitioning) return true;

        // Mark that we're mid-transition (still calling callbacks).
        stateTransitioning = true;

        // Repeat while we've still got a next state queued up.
        while (nextState != null) {
            // Grab the current state, update the current state and clear the queue.
            T oldState = currentState;
            currentState = nextState;
            nextState = null;

            // Cloned the list of callbacks and iterate over that in case the original is modified.
            for (CallbackWrapper<T, ?, ?> callback : new ArrayList<>(callbacks.values())) {
                if (callback.transition.isValid(oldState, currentState)) {
                    castAndCallCallback(oldState, currentState, callback.callback);
                }
            }
        }

        // Mark that we're finished transitioning (no more callbacks and no more queued state).
        stateTransitioning = false;
        return true;
    }

    /** Helper function with generics tricks to call a callback with properly cast arguments. */
    private <From extends T, To extends T> void castAndCallCallback(
            T from, T to, Callback<From, To> callback) {
        callback.onStateChanged((From) from, (To) to);
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Callback<From, To> callback) {
        CallbackToken token = new CallbackToken() {};
        callbacks.put(token, new CallbackWrapper<>(new Transition(from, to), callback));
        return token;
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Consumer<To> callback) {
        return addCallback(from, to, (f, t) -> callback.accept(t));
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Runnable callback) {
        return addCallback(from, to, (f, t) -> callback.run());
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Callback<T, To> callback) {
        return addCallback(null, to, callback);
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Consumer<To> callback) {
        return addCallbackFromAnythingTo(to, (f, t) -> callback.accept(t));
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Runnable callback) {
        return addCallbackFromAnythingTo(to, (f, t) -> callback.run());
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

    public static <T> Builder<T> newBuilder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private final ArrayList<Transition> validTransitions = new ArrayList<>();

        protected Builder() {}

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
            return new StateMachine<>(this, state);
        }
    }
}
