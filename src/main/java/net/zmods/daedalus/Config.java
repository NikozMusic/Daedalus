package net.zmods.daedalus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public static boolean allowDeepSearch = false;
    public static int luaTimeoutSeconds = 5;
    public static boolean enableDaedalusCommand = true;
    public static boolean reportErrorsToChat = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("daedalus.json");

    private static class Data {
        boolean allowDeepSearch = false;
        int luaTimeoutSeconds = 5;
        boolean enableDaedalusCommand = true;
        boolean reportErrorsToChat = true;
    }

    public static void load() {
        try {
            Data data = Files.exists(CONFIG_PATH)
                    ? GSON.fromJson(Files.readString(CONFIG_PATH), Data.class)
                    : new Data();
            allowDeepSearch = data.allowDeepSearch;
            luaTimeoutSeconds = Math.max(1, Math.min(30, data.luaTimeoutSeconds));
            enableDaedalusCommand = data.enableDaedalusCommand;
            reportErrorsToChat = data.reportErrorsToChat;
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() throws IOException {
        Data data = new Data();
        data.allowDeepSearch = allowDeepSearch;
        data.luaTimeoutSeconds = luaTimeoutSeconds;
        data.enableDaedalusCommand = enableDaedalusCommand;
        data.reportErrorsToChat = reportErrorsToChat;
        Files.writeString(CONFIG_PATH, GSON.toJson(data));
    }
}