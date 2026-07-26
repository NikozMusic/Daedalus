package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.lib.OneArgFunction;
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
    }
}