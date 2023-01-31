package com.hounshell.st8;

public final class TransitionContentionException extends RuntimeException {
    public <T> TransitionContentionException(T state, T next, T contention) {
        super(String.format("Contention detected in state machine. State is %s, will be %s, but this thread is trying to change it to %s", state, next, contention));
    }
}
