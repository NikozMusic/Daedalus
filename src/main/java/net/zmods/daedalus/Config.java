package net.zmods.daedalus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    // Gates dangerous/irreversible server-side operations exposed to Lua,
    // e.g. server.stop() or accessing files outside the module's own directory
    public static boolean allowDangerousOperations = false;

    // per-script execution timeout, in seconds.
    // hook checked every 1000 instructions, so it's a guard against
    // runaway/infinite Lua loops
    public static int luaTimeoutSeconds = 5;

    // Whether the built-in "/daedalus" management command (reload, modules, debug, etc.) is registered.
    public static boolean enableDaedalusCommand = true;

    // Whether lua errors are sent in game chat, useful for debugging.
    public static boolean reportErrorsToChat = true;

    // Whether "[Daedalus] Loaded module: ..." lines are printed to console on
    // load/reload. Handy to silence on servers with a lot of modules.
    public static boolean logModuleLoads = true;

    // Movement distance (in blocks) a player must move before ENTITY_MOVE fires again.
    // Lower = more responsive but more event traffic; higher = cheaper but coarser.
    public static double entityMoveThreshold = 0.05;

    // Caps how many characters of captured command output (from command.execute()
    // and friends) get returned to Lua, to keep a spammy command from blowing up memory.
    public static int maxCommandOutputChars = 4000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("daedalus.json");

    private static class Data {
        boolean allowDangerousOperations = false;
        int luaTimeoutSeconds = 5;
        boolean enableDaedalusCommand = true;
        boolean reportErrorsToChat = true;
        boolean logModuleLoads = true;
        double entityMoveThreshold = 0.05;
        int maxCommandOutputChars = 4000;
    }

    public static void load() {
        try {
            Data data = Files.exists(CONFIG_PATH)
                    ? GSON.fromJson(Files.readString(CONFIG_PATH), Data.class)
                    : new Data();
            allowDangerousOperations = data.allowDangerousOperations;
            luaTimeoutSeconds = Math.max(1, Math.min(30, data.luaTimeoutSeconds));
            enableDaedalusCommand = data.enableDaedalusCommand;
            reportErrorsToChat = data.reportErrorsToChat;
            logModuleLoads = data.logModuleLoads;
            entityMoveThreshold = Math.max(0.0, data.entityMoveThreshold);
            maxCommandOutputChars = Math.max(0, data.maxCommandOutputChars);
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() throws IOException {
        Data data = new Data();
        data.allowDangerousOperations = allowDangerousOperations;
        data.luaTimeoutSeconds = luaTimeoutSeconds;
        data.enableDaedalusCommand = enableDaedalusCommand;
        data.reportErrorsToChat = reportErrorsToChat;
        data.logModuleLoads = logModuleLoads;
        data.entityMoveThreshold = entityMoveThreshold;
        data.maxCommandOutputChars = maxCommandOutputChars;
        Files.writeString(CONFIG_PATH, GSON.toJson(data));
    }
}