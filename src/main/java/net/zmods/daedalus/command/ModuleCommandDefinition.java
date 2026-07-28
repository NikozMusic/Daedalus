package net.zmods.daedalus.command;

public class ModuleCommandDefinition {
    public final String moduleId;
    public final String commandKey;   // key under "commands" in commands.json
    public final String name;         // in-game command name, e.g. /ExampleCommand
    public final String luaFileKey;   // resolved key into the module's luaFiles map (no ".lua")

    public ModuleCommandDefinition(String moduleId, String commandKey, String name, String luaFileKey) {
        this.moduleId = moduleId;
        this.commandKey = commandKey;
        this.name = name;
        this.luaFileKey = luaFileKey;
    }
}