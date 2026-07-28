package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.Daedalus;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.event.TickTracker;
import net.zmods.daedalus.module.ModuleManager;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

//Misc functionality that has to execute on Minecraft's engine itself such as logging to the console
//This includes functions that don't yet have their own dedicated api

public class CoreApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "minecraft";
    }

    @Override
    public void register(LuaTable table, Globals globals) {
        table.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue msg) {
                System.out.println("[Lua] " + msg.checkjstring());
                return NIL;
            }
        });

        // minecraft.getVersion() -> "0.4.0" (the Daedalus mod version)
        table.set("getDaedalusVersion", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                String version = FabricLoader.getInstance()
                        .getModContainer("daedalus")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("Unknown");
                return LuaValue.valueOf(version);
            }
        });

        table.set("getMinecraftVersion", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                String version = FabricLoader.getInstance()
                        .getModContainer("minecraft")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("Unknown");
                return LuaValue.valueOf(version);
            }
        });

        table.set("warn", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Daedalus.LOGGER.warn(arg.tojstring());
                return LuaValue.NIL;
            }
        });

        table.set("error", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Daedalus.LOGGER.error(arg.tojstring());
                return LuaValue.NIL;
            }
        });


    }
}