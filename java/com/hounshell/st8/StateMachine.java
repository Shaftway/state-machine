package com.hounshell.st8;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class StateMachine<T> {
    private final ArrayList<Transition> validTransitions = new ArrayList<>();
    private final LinkedHashMap<CallbackToken, CallbackWrapper<T, ?, ?>> callbacks = new LinkedHashMap<>();
    private T currentState;
    private T nextState;
    private boolean stateTransitioning = false;

    protected StateMachine(BaseBuilder<T, ?, ?> builder, T currentState) {
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
        assert (to != null);

        if (stateTransitioning && nextState != null) {
            throw new TransitionContentionException(currentState, nextState, to);
        }

        if (from != null && from != currentState) return false;

        nextState = to;
        if (stateTransitioning) return true;

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
                            if (callback.transition.isValid(oldState, currentState)) {
                                castAndCallCallback(oldState, currentState, callback.callback);
                            }
                        }

                        break found;
                    }
                }

                throw new InvalidTransitionException(currentState, nextState);
            }
        }

        stateTransitioning = false;
        return true;
    }

    private <From extends T, To extends T> void castAndCallCallback(T from, T to, Callback<From, To> callback) {
        callCallback((From) from, (To) to, callback);
    }

    protected <From extends T, To extends T> void callCallback(From from, To to, Callback<From, To> callback) {
        callback.onStateChanged(from, to);
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Callback<From, To> callback) {
        CallbackToken token = new CallbackToken() {};
        callbacks.put(token, new CallbackWrapper<T, From, To>(new Transition(from, to), callback));
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

    public static final class Builder<T> extends BaseBuilder<T, StateMachine<T>, Builder<T>> {
        @Override
        public StateMachine<T> buildWithInitialState(T state) {
            return new StateMachine<>(this, state);
        }
    }

    public abstract static class BaseBuilder<T, SM extends StateMachine<T>, B extends BaseBuilder<T, SM, B>> {
        private final ArrayList<Transition> validTransitions = new ArrayList<>();

        protected BaseBuilder() {}

        public <To extends T> B addValidTransitionFromAnythingTo(Class<To> to) {
            return addValidTransition(null, to);
        }

        public <From extends T> B addValidTransitionToAnythingFrom(Class<From> from) {
            return addValidTransition(from, null);
        }

        public <From extends T, To extends T> B addValidTransition(Class<From> from, Class<To> to) {
            validTransitions.add(new Transition(from, to));
            return (B) this;
        }

        public B makeAllTransitionsValid() {
            validTransitions.clear();
            validTransitions.add(new Transition(null, null));
            return (B) this;
        }

        public abstract SM buildWithInitialState(T state);
    }
}
