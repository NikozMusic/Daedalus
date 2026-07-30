package net.zmods.daedalus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;


public class ChatErrorReporter {
    private static volatile MinecraftServer server;

    // Called once from Daedalus' SERVER_STARTED listener.
    public static void setServer(MinecraftServer s) {
        server = s;
    }

    public static void report(String context, Throwable t) {
        System.err.println("[Daedalus] " + context + ": " + t.getMessage());
        t.printStackTrace();

        if (!Config.reportErrorsToChat) return;

        MinecraftServer s = server;
        if (s == null) return; // not started yet / already shutting down

        Component message = Component.literal("§c[Daedalus] " + context + ": " + t.getMessage());
        s.getPlayerList().broadcastSystemMessage(message, false);
    }
}