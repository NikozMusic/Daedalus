package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.Config;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.module.ModuleContext;
import net.zmods.daedalus.data.EntityDataHelper;
import net.zmods.daedalus.data.ItemDataHelper;
import net.zmods.daedalus.data.NbtLuaConverter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.OneArgFunction;

public class DataApi implements LuaApiRegistry.LuaApiModule {

    // 26.2 replaced Entity.saveWithoutId(CompoundTag)/load(CompoundTag) with
    // save(ValueOutput)/load(ValueInput) - an abstraction layer over NBT rather
    // than a raw CompoundTag. TagValueOutput/TagValueInput are the concrete
    // implementations that let us still get a plain CompoundTag in and out, via
    // a ProblemReporter (DISCARDING since we don't need validation diagnostics
    // here) and the level's registry access (for any registry-aware components
    // the entity might serialize).
    private static CompoundTag saveEntityToTag(Entity entity) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                entity.level().registryAccess()
        );
        entity.saveWithoutId(output);
        return output.buildResult();
    }

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

        table.set("entityGetRaw", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                CompoundTag tag = saveEntityToTag(entity);
                return NbtLuaConverter.nbtToLua(tag);
            }
        });

        table.set("entitySetRaw", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue dataArg) {
                if (!Config.allowDangerousOperations) {
                    return error("data.entitySetRaw requires allowDangerousOperations to be enabled in daedalus.json");
                }

                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                if (!dataArg.istable()) {
                    return error("Second argument to data.entitySetRaw must be a table");
                }

                CompoundTag current = saveEntityToTag(entity);

                Tag overlayTag = NbtLuaConverter.luaToNbt(dataArg);
                if (!(overlayTag instanceof CompoundTag overlay)) {
                    return error("data.entitySetRaw expects a table of fields, not an array");
                }

                NbtLuaConverter.merge(current, overlay);

                ValueInput input = TagValueInput.create(
                        ProblemReporter.DISCARDING,
                        entity.level().registryAccess(),
                        current
                );
                entity.load(input);
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