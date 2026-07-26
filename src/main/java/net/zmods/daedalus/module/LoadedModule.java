package net.zmods.daedalus.module;

import org.luaj.vm2.LuaValue;

public class LoadedModule {
    public ModuleMetadata metadata;
    public LuaValue environment;

    public LoadedModule(ModuleMetadata metadata, LuaValue environment) {
        this.metadata = metadata;
        this.environment = environment;
    }
}