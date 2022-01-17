package io.quarkus.status.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StringTuple {
    public final String x;
    public final String y;
    public StringTuple(String x, String y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return x + ":" + y;
    }
}
