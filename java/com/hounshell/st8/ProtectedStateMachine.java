package com.hounshell.st8;

import java.util.function.Consumer;

public class ProtectedStateMachine<T> implements ReadableStateMachine<T> {
    private final StateMachine<T> stateMachine;

    protected ProtectedStateMachine(StateMachine<T> underlyingStateMachine) {
        this.stateMachine = underlyingStateMachine;
    }

    protected synchronized void transition(T to) {
        stateMachine.transition(to);
    }

    protected synchronized boolean transition(T from, T to) {
        return stateMachine.transition(from, to);
    }

    public T getCurrentState() {
        return stateMachine.getCurrentState();
    }

    public boolean isState(Class<? extends T>... stateClasses) {
        return stateMachine.isState(stateClasses);
    }

    public synchronized boolean isNextStateQueued() {
        return stateMachine.isNextStateQueued();
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Callback<From, To> callback) {
        return stateMachine.addCallback(from, to, callback);
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Consumer<To> callback) {
        return stateMachine.addCallback(from, to, callback);
    }

    public <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Runnable callback) {
        return stateMachine.addCallback(from, to, callback);
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Callback<T, To> callback) {
        return stateMachine.addCallbackFromAnythingTo(to, callback);
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Consumer<To> callback) {
        return stateMachine.addCallbackFromAnythingTo(to, callback);
    }

    public <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Runnable callback) {
        return stateMachine.addCallbackFromAnythingTo(to, callback);
    }

    public <From extends T, To extends T> CallbackToken addCallbackToAnythingFrom(
            Class<From> from, Callback<From, T> callback) {
        return stateMachine.addCallbackToAnythingFrom(from, callback);
    }

    public CallbackToken addCallbackForAnything(Callback<T, T> callback) {
        return stateMachine.addCallbackForAnything(callback);
    }

    public void removeCallback(CallbackToken token) {
        stateMachine.removeCallback(token);
    }
}
