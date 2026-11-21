package com.campusgigs.pattern.observer;

/** A user topped up their wallet with {@code amount} simulated tokens. */
public record WalletToppedUp(long userId, long amount) implements AppEvent {
}
