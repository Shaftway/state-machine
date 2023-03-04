package com.hounshell.st8;

/* protected */ class CallbackWrapper<T, From extends T, To extends T> {
    /* protected */ final Transition transition;
    /* protected */ final Callback<From, To> callback;

    /* protected */ CallbackWrapper(Transition transition, Callback<From, To> callback) {
        this.transition = transition;
        this.callback = callback;
    }
}
