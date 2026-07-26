package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.module.ModuleContext;
import net.zmods.daedalus.tag.EntityDataHelper;
import net.zmods.daedalus.tag.ItemDataHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.OneArgFunction;

public class DataApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "data";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // -- Entity data --

        table.set("entitySet", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue path, LuaValue value) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                EntityDataHelper.set(entity, ModuleContext.getCurrentModuleId(), path.checkjstring(), value);
                return NIL;
            }
        });

        table.set("entityGet", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue path) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                return EntityDataHelper.get(entity, ModuleContext.getCurrentModuleId(), path.checkjstring());
            }
        });

        table.set("entityHas", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue path) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                return LuaValue.valueOf(EntityDataHelper.has(entity, ModuleContext.getCurrentModuleId(), path.checkjstring()));
            }
        });

        table.set("entityRemove", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue path) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                EntityDataHelper.remove(entity, ModuleContext.getCurrentModuleId(), path.checkjstring());
                return NIL;
            }
        });

        table.set("entityList", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                return EntityDataHelper.list(entity, ModuleContext.getCurrentModuleId());
            }
        });

        table.set("entityClear", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                EntityDataHelper.clear(entity, ModuleContext.getCurrentModuleId());
                return NIL;
            }
        });

        // -- Item data --

        table.set("itemSet", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg, LuaValue path, LuaValue value) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                ItemDataHelper.set(stack, ModuleContext.getCurrentModuleId(), path.checkjstring(), value);
                return NIL;
            }
        });

        table.set("itemGet", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg, LuaValue path) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return ItemDataHelper.get(stack, ModuleContext.getCurrentModuleId(), path.checkjstring());
            }
        });

        table.set("itemHas", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg, LuaValue path) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(ItemDataHelper.has(stack, ModuleContext.getCurrentModuleId(), path.checkjstring()));
            }
        });

        table.set("itemRemove", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg, LuaValue path) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                ItemDataHelper.remove(stack, ModuleContext.getCurrentModuleId(), path.checkjstring());
                return NIL;
            }
        });

        table.set("itemList", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return ItemDataHelper.list(stack, ModuleContext.getCurrentModuleId());
            }
        });

        table.set("itemClear", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                ItemDataHelper.clear(stack, ModuleContext.getCurrentModuleId());
                return NIL;
            }
        });
    }
}