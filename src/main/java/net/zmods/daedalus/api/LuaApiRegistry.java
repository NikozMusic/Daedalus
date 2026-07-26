package net.zmods.daedalus.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.Globals;
import java.util.*;

//This class allows Daedalus to easily load all of its APIs for lua to use
public class LuaApiRegistry {
    private final List<LuaApiModule> apiModules = new ArrayList<>();

    public interface LuaApiModule {
        String getNamespace();
        void register(LuaTable namespaceTable, Globals globals);
    }

    public void registerApi(LuaApiModule apiModule) {
        apiModules.add(apiModule);
    }

    public void applyTo(Globals globals) {
        for (LuaApiModule apiModule : apiModules) {
            LuaTable namespaceTable = new LuaTable();
            apiModule.register(namespaceTable, globals);
            globals.set(apiModule.getNamespace(), namespaceTable);
        }
    }
}