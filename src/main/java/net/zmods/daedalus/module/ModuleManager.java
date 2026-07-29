package net.zmods.daedalus.module;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.command.CommandParser;
import net.zmods.daedalus.command.ModuleCommandDefinition;
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

    private CommandDispatcher<CommandSourceStack> dispatcher;
    private CommandBuildContext buildContext;
    private final Set<String> registeredCommandNames = new HashSet<>();

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

        List<ModuleCommandDefinition> commands = new ArrayList<>();
        File commandsFile = new File(folder, "commands.json");
        if (commandsFile.exists()) {
            commands = parseCommands(Files.readString(commandsFile.toPath()), metadata.data.id);
        }

        executeModule(metadata, luaFiles, folder.getName(), commands);
    }

    private void loadFromZip(File zipFile) throws IOException {
        ModuleMetadata metadata;
        Map<String, String> luaFiles = new HashMap<>();
        List<ModuleCommandDefinition> commands = new ArrayList<>();

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

            ZipEntry commandsEntry = zip.getEntry("commands.json");
            if (commandsEntry != null) {
                String commandsJson = new String(zip.getInputStream(commandsEntry).readAllBytes());
                commands = parseCommands(commandsJson, metadata.data.id);
            }
        }

        if (!luaFiles.containsKey("main")) {
            throw new IOException("Module " + metadata.data.id + " missing main.lua");
        }

        executeModule(metadata, luaFiles, zipFile.getName(), commands);
    }

    private void validateMetadata(ModuleMetadata metadata) throws IOException {
        if (metadata == null || metadata.data == null || metadata.data.id == null || metadata.data.id.isBlank()) {
            throw new IOException("Invalid module.json: missing data.id");
        }
        if (loadedModules.containsKey(metadata.data.id)) {
            throw new IOException("Duplicate module id: " + metadata.data.id);
        }
    }

    private List<ModuleCommandDefinition> parseCommands(String json, String moduleId) {
        List<ModuleCommandDefinition> result = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject commandsObj = root.getAsJsonObject("commands");
            if (commandsObj == null) return result;

            for (String key : commandsObj.keySet()) {
                JsonObject cmdObj = commandsObj.getAsJsonObject(key);
                if (!cmdObj.has("name") || !cmdObj.has("filepath")) {
                    System.err.println("[Daedalus] Command '" + key + "' in module " + moduleId
                            + " is missing required 'name' or 'filepath', skipping.");
                    continue;
                }

                String name = cmdObj.get("name").getAsString();
                String filepath = cmdObj.get("filepath").getAsString().replace('\\', '/');
                String luaKey = filepath.endsWith(".lua") ? filepath.substring(0, filepath.length() - 4) : filepath;

                result.add(new ModuleCommandDefinition(moduleId, key, name, luaKey));
            }
        } catch (Exception e) {
            System.err.println("[Daedalus] Failed to parse commands.json for module " + moduleId);
            e.printStackTrace();
        }
        return result;
    }

    private void executeModule(ModuleMetadata metadata, Map<String, String> luaFiles, String sourceName,
                               List<ModuleCommandDefinition> commands) {
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

            loadedModules.put(metadata.data.id, new LoadedModule(metadata, moduleEnv, luaFiles, commands));
            System.out.println("[Daedalus] Loaded module: " + metadata.data.id + " (" + metadata.info.name + ")"
                    + (commands.isEmpty() ? "" : " with " + commands.size() + " command(s)"));
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
        registerModuleCommands();
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

    // Register / reload commands for hotreload
    public void setCommandContext(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        this.dispatcher = dispatcher;
        this.buildContext = buildContext;
        registerModuleCommands();
    }

    public void registerModuleCommands() {
        System.out.println("[Daedalus] registerModuleCommands: dispatcher=" + (dispatcher != null)
                + ", buildContext=" + (buildContext != null));
        if (dispatcher == null) return;

        // Drop whatever module commands were registered previously
        if (!registeredCommandNames.isEmpty()) {
            dispatcher.getRoot().getChildren().removeIf(node -> registeredCommandNames.contains(node.getName()));
            registeredCommandNames.clear();
        }

        for (LoadedModule module : loadedModules.values()) {
            if (module.commands == null) continue;
            for (ModuleCommandDefinition def : module.commands) {
                if (registeredCommandNames.contains(def.name)) {
                    System.err.println("[Daedalus] Command name '" + def.name + "' from module " + def.moduleId
                            + " is already registered by another module, skipping.");
                    continue;
                }
                try {
                    registerSingleCommand(module, def);
                    registeredCommandNames.add(def.name);
                } catch (Exception e) {
                    System.err.println("[Daedalus] Failed to register command '" + def.name + "' from module " + def.moduleId);
                    e.printStackTrace();
                }
            }
        }

        // Push the updated command tree to anyone already connected
        if (currentServer != null) {
            for (net.minecraft.server.level.ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
                currentServer.getCommands().sendCommands(player);
            }
        }
    }

    // Every module command is /command [anything]
    // This lets the connected Lua file do whatever it wants on the arguments
    private void registerSingleCommand(LoadedModule module, ModuleCommandDefinition def) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(def.name)
                .executes(ctx -> {
                    runModuleCommand(module, def, ctx.getSource(), "");
                    return 1;
                })
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String raw = StringArgumentType.getString(ctx, "args");
                            runModuleCommand(module, def, ctx.getSource(), raw);
                            return 1;
                        }));

        dispatcher.register(root);
    }

    private void runModuleCommand(LoadedModule module, ModuleCommandDefinition def,
                                  CommandSourceStack source, String rawArgs) {
        String code = module.luaFiles.get(def.luaFileKey);
        if (code == null) {
            source.sendFailure(Component.literal(
                    "[Daedalus] Command '" + def.name + "' references missing file: " + def.luaFileKey + ".lua"));
            return;
        }

        // Child environment: sees this invocation's args + the module's own globals/require,
        // falls back to globals.
        LuaTable cmdEnv = new LuaTable();
        LuaTable mt = new LuaTable();
        mt.set("__index", module.environment);
        cmdEnv.setmetatable(mt);

        LuaTable argsTable = CommandParser.toLuaArgs(source, rawArgs);
        cmdEnv.set("args", argsTable);
        cmdEnv.set("argCount", LuaValue.valueOf(argsTable.length()));
        cmdEnv.set("rawArgs", LuaValue.valueOf(rawArgs));

        Entity sender = source.getEntity();
        cmdEnv.set("sender", sender != null ? org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(sender) : LuaValue.NIL);

        ModuleContext.set(def.moduleId);
        try {
            LuaValue chunk = globals.load(code, def.luaFileKey, cmdEnv);
            LuaThread thread = new LuaThread(globals, chunk);
            thread.resume(LuaValue.NONE);
        } catch (Exception e) {
            System.err.println("[Daedalus] Error executing command '" + def.name + "' (module " + def.moduleId + ")");
            e.printStackTrace();
            source.sendFailure(Component.literal("[Daedalus] Command error: " + e.getMessage()));
        } finally {
            ModuleContext.clear();
        }
    }
}