package me.mneri.immutable.example;

import me.mneri.immutable.Immutable;

@Immutable
public class A extends B {
    private final C c;
    private final String string;

    public A(C c, String string) {
        this.c = c;
        this.string = string;
    }
}
