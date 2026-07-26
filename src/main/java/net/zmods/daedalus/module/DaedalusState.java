package net.zmods.daedalus.module;

//This file is exclusively used for the daedalus pause/resume commands :3

public class DaedalusState {
    private static volatile boolean running = true;
    private static volatile boolean debug = false;

    public static boolean isRunning() {
        return running;
    }

    public static void setRunning(boolean value) {
        running = value;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean value) {
        debug = value;
    }
}