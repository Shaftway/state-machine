package com.hounshell.st8;

import static java.lang.System.lineSeparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;

/* protected */ class Transition {
    private final Class<?> fromClass;
    private final Class<?> toClass;

    /* protected */ Transition(Class<?> from, Class<?> to) {
        this.fromClass = from;
        this.toClass = to;
    }

    /* protected */ boolean isValid(Object from, Object to) {
        assert (from != null);
        assert (to != null);

        return isObjectValidForClass(from, fromClass)
                && isObjectValidForClass(to, toClass);
    }

    private static boolean isObjectValidForClass(Object obj, Class<?> objClass) {
        return (objClass == null || objClass.isAssignableFrom(obj.getClass()));
    }
}
