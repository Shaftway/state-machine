package com.hounshell.st8;

public interface Callback<From, To> {
    void onStateChanged(From from, To to);
}
