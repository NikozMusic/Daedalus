package net.zmods.daedalus.module;

import com.google.gson.Gson;
import net.zmods.daedalus.api.LuaApiRegistry;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

// This file is the meat and potatoes of Daedalus; where it loads all the modules and executes them,
// also where it loads in the base Lua libraries like String and Math and such.

public class ModuleManager {
    private final Globals globals;
    private final File modulesDir;
    private final Map<String, LoadedModule> loadedModules = new HashMap<>();
    private final Gson gson = new Gson();
    private final net.minecraft.server.MinecraftServer currentServer;

    public ModuleManager(File minecraftDir, LuaApiRegistry apiRegistry, net.minecraft.server.MinecraftServer server) {
        this.modulesDir = new File(minecraftDir, "modules");
        modulesDir.mkdirs();
        this.currentServer = server;

        this.globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new org.luaj.vm2.lib.jse.JseMathLib());
        LuaC.install(globals);

        apiRegistry.applyTo(globals);
    }

    public void discoverAndLoadAll() {
        File[] files = modulesDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            try {
                if (file.isDirectory() && new File(file, "module.json").exists()) {
                    loadFromFolder(file);
                } else if (file.getName().endsWith(".zip")) {
                    loadFromZip(file);
                }
            } catch (Exception e) {
                System.err.println("[Daedalus] Failed to load module: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private void loadFromFolder(File folder) throws IOException {
        File metadataFile = new File(folder, "module.json");
        String json = Files.readString(metadataFile.toPath());
        ModuleMetadata metadata = gson.fromJson(json, ModuleMetadata.class);
        validateMetadata(metadata);

        Map<String, String> luaFiles = new HashMap<>();
        Files.walk(folder.toPath())
                .filter(p -> p.toString().endsWith(".lua"))
                .forEach(p -> {
                    try {
                        String relative = folder.toPath().relativize(p).toString();
                        String name = relative.replace(File.separatorChar, '/').replace(".lua", "");
                        luaFiles.put(name, Files.readString(p));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        if (!luaFiles.containsKey("main")) {
            throw new IOException("Module " + metadata.data.id + " missing main.lua");
        }

        executeModule(metadata, luaFiles, folder.getName());
    }

    private void loadFromZip(File zipFile) throws IOException {
        ModuleMetadata metadata;
        Map<String, String> luaFiles = new HashMap<>();

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry metadataEntry = zip.getEntry("module.json");
            if (metadataEntry == null) throw new IOException("No module.json in " + zipFile.getName());

            String json = new String(zip.getInputStream(metadataEntry).readAllBytes());
            metadata = gson.fromJson(json, ModuleMetadata.class);
            validateMetadata(metadata);

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".lua")) {
                    String name = entry.getName().replace(".lua", "");
                    String content = new String(zip.getInputStream(entry).readAllBytes());
                    luaFiles.put(name, content);
                }
            }
        }

        if (!luaFiles.containsKey("main")) {
            throw new IOException("Module " + metadata.data.id + " missing main.lua");
        }

        executeModule(metadata, luaFiles, zipFile.getName());
    }

    private void validateMetadata(ModuleMetadata metadata) throws IOException {
        if (metadata == null || metadata.data == null || metadata.data.id == null || metadata.data.id.isBlank()) {
            throw new IOException("Invalid module.json: missing data.id");
        }
        if (loadedModules.containsKey(metadata.data.id)) {
            throw new IOException("Duplicate module id: " + metadata.data.id);
        }
    }

    private void executeModule(ModuleMetadata metadata, Map<String, String> luaFiles, String sourceName) {
        // Isolated environment per module, falling back to globals for API access
        LuaTable moduleEnv = new LuaTable();
        LuaTable mt = new LuaTable();
        mt.set("__index", globals);
        moduleEnv.setmetatable(mt);

        // Per-module require() cache and resolver, scoped to this module's own Lua files
        Map<String, LuaValue> requireCache = new HashMap<>();

        moduleEnv.set("require", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue nameArg) {
                String name = nameArg.checkjstring();

                if (requireCache.containsKey(name)) {
                    return requireCache.get(name);
                }

                String code = luaFiles.get(name);
                if (code == null) {
                    return error("module '" + name + "' not found in module '" + metadata.data.id + "'");
                }

                LuaValue chunk = globals.load(code, name, moduleEnv);
                LuaValue result = chunk.call();

                if (result == LuaValue.NIL) {
                    result = LuaValue.TRUE;
                }
                requireCache.put(name, result);
                return result;
            }
        });

        ModuleContext.set(metadata.data.id);
        try {
            String mainCode = luaFiles.get("main");
            LuaValue chunk = globals.load(mainCode, sourceName, moduleEnv);
            chunk.call();

            loadedModules.put(metadata.data.id, new LoadedModule(metadata, moduleEnv));
            System.out.println("[Daedalus] Loaded module: " + metadata.data.id + " (" + metadata.info.name + ")");
        } catch (Exception e) {
            System.err.println("[Daedalus] Error executing module " + metadata.data.id);
            e.printStackTrace();
        } finally {
            ModuleContext.clear();
        }


    }

    public void reloadAll() {
        unloadAll();
        discoverAndLoadAll();
        resyncOnlinePlayers();
    }

    private void unloadAll() {
        loadedModules.clear();
        net.zmods.daedalus.event.EventBindingRegistry.getInstance().clearAll();
    }

    private void resyncOnlinePlayers() {
        if (currentServer == null) return;
        for (net.minecraft.server.level.ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
            net.zmods.daedalus.event.EventFirer.fireGlobalEvent(
                    net.zmods.daedalus.event.Events.PLAYER_JOIN,
                    org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(player)
            );
        }
    }

    public Collection<LoadedModule> getAllModules() {
        return loadedModules.values();
    }

    public LoadedModule getModule(String id) {
        return loadedModules.get(id);
    }
}