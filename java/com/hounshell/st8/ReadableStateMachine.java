package com.hounshell.st8;

import java.util.function.Consumer;

public interface ReadableStateMachine<T> {
    T getCurrentState();

    boolean isState(Class<? extends T>... stateClasses);

    boolean isNextStateQueued();

    <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Callback<From, To> callback);

    <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Consumer<To> callback);

    <From extends T, To extends T> CallbackToken addCallback(
            Class<From> from, Class<To> to, Runnable callback);

    <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Callback<T, To> callback);

    <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Consumer<To> callback);

    <To extends T> CallbackToken addCallbackFromAnythingTo(
            Class<To> to, Runnable callback);

    <From extends T, To extends T> CallbackToken addCallbackToAnythingFrom(
            Class<From> from, Callback<From, T> callback);

    CallbackToken addCallbackForAnything(Callback<T, T> callback);

    void removeCallback(CallbackToken token);
}
