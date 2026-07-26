package net.zmods.daedalus.event;

//Required for mixins to work properly as they can't directly call the tick for some reason
public class TickTracker {
    private static volatile long currentTick = 0;

    public static void increment() {
        currentTick++;
    }

    public static long get() {
        return currentTick;
    }
}