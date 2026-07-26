package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

//Misc functionality that has to execute on Minecraft's engine itself such as logging to the console
//This includes functions that don't yet have their own dedicated api

public class ItemApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "item";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // minecraft.createItem("minecraft:diamond_sword", 1) -> ItemStack
        table.set("createItem", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemIdArg, LuaValue countArg) {
                String itemId = itemIdArg.checkjstring();
                int count = countArg.checkint();

                Identifier id;
                try {
                    id = Identifier.parse(itemId);
                } catch (Exception e) {
                    return error("Invalid item identifier: " + itemId);
                }

                var holder = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                if (holder.isEmpty()) {
                    return error("Unknown item: " + itemId);
                }

                ItemStack stack = new ItemStack(holder.get().value(), count);
                return CoerceJavaToLua.coerce(stack);
            }
        });

        // item.getId(stack) -> "minecraft:diamond_sword"
        table.set("getId", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                return LuaValue.valueOf(id.toString());
            }
        });

        // item.isEmpty(stack) -> boolean
        table.set("isEmpty", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(stack.isEmpty());
            }
        });

        // item.getCount(stack) -> int
        table.set("getCount", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(stack.getCount());
            }
        });

        // item.setCount(stack, count)
        table.set("setCount", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg, LuaValue countArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                stack.setCount(countArg.checkint());
                return NIL;
            }
        });

        // item.getMaxStackSize(stack) -> int
        table.set("getMaxStackSize", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(stack.getMaxStackSize());
            }
        });

        // item.copy(stack) -> ItemStack (new independent copy)
        table.set("copy", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return CoerceJavaToLua.coerce(stack.copy());
            }
        });

        // item.isDamageable(stack) -> boolean
        table.set("isDamageable", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(stack.isDamageableItem());
            }
        });

        // item.getDamage(stack) -> int
        table.set("getDamage", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(stack.getDamageValue());
            }
        });

        // item.setDamage(stack, damage)
        table.set("setDamage", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg, LuaValue damageArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                stack.setDamageValue(damageArg.checkint());
                return NIL;
            }
        });

        // item.getMaxDamage(stack) -> int
        table.set("getMaxDamage", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArg) {
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(stack.getMaxDamage());
            }
        });

        // item.matches(stackA, stackB) -> boolean (same item type, ignoring count)
        table.set("matches", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue itemArgA, LuaValue itemArgB) {
                ItemStack a = (ItemStack) itemArgA.checkuserdata(ItemStack.class);
                ItemStack b = (ItemStack) itemArgB.checkuserdata(ItemStack.class);
                return LuaValue.valueOf(ItemStack.isSameItemSameComponents(a, b));
            }
        });
    }
}