package me.mneri.immutable.example;

import me.mneri.immutable.Immutable;

@Immutable
public class A extends B {
    private final C c;

    public A(C c) {
        this.c = c;
    }
}
