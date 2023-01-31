package com.hounshell.st8;

/* protected */ class CallbackWrapper<T, From extends T, To extends T> {
    private final Transition transition;
    private final Callback<From, To> callback;

    /* protected */ CallbackWrapper(Transition transition, Callback<From, To> callback) {
        this.transition = transition;
        this.callback = callback;
    }

    /* protected */ void handleCall(T from, T to) {
        if (transition.isValid(from, to)) {
            callback.onStateChanged((From) from, (To) to);
        }
    }
}
