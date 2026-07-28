package net.zmods.daedalus.module;

import net.zmods.daedalus.command.ModuleCommandDefinition;
import org.luaj.vm2.LuaValue;

import java.util.List;
import java.util.Map;

public class LoadedModule {
    public ModuleMetadata metadata;
    public LuaValue environment;
    public Map<String, String> luaFiles;
    public List<ModuleCommandDefinition> commands;

    public LoadedModule(ModuleMetadata metadata, LuaValue environment,
                        Map<String, String> luaFiles, List<ModuleCommandDefinition> commands) {
        this.metadata = metadata;
        this.environment = environment;
        this.luaFiles = luaFiles;
        this.commands = commands;
    }
}