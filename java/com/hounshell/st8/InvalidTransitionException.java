package com.hounshell.st8;

public final class InvalidTransitionException extends RuntimeException {
    public <T> InvalidTransitionException(T from, T to) {
        super(String.format("Invalid transition from %s to %s", from, to));
    }
}
