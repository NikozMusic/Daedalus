package net.zmods.daedalus.api.apis;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.UUID;


public class EntityApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "entity";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        table.set("getByUUID", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue levelArg, LuaValue uuidArg) {
                ServerLevel level = (ServerLevel) levelArg.checkuserdata(ServerLevel.class);
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidArg.checkjstring());
                } catch (IllegalArgumentException e) {
                    return error("Invalid UUID string: " + uuidArg.checkjstring());
                }

                Entity found = level.getEntity(uuid);
                if (found == null) return NIL;
                return org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(found);
            }
        });

        table.set("getWorld", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity e = (Entity) entityArg.checkuserdata(Entity.class);
                return org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(e.level());
            }
        });

        table.set("getVelocity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity e = (Entity) args.checkuserdata(1, Entity.class);
                Vec3 v = e.getDeltaMovement();
                return LuaValue.varargsOf(new LuaValue[] {
                        LuaValue.valueOf(v.x),
                        LuaValue.valueOf(v.y),
                        LuaValue.valueOf(v.z)
                });
            }
        });
        table.set("setVelocity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity e = (Entity) args.checkuserdata(1, Entity.class);
                double x = args.checkdouble(2);
                double y = args.checkdouble(3);
                double z = args.checkdouble(4);
                e.setDeltaMovement(x, y, z);
                e.hurtMarked = true; // forces velocity sync to client
                return NONE;
            }
        });

        table.set("addVelocity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity e = (Entity) args.checkuserdata(1, Entity.class);
                double x = args.checkdouble(2);
                double y = args.checkdouble(3);
                double z = args.checkdouble(4);
                Vec3 current = e.getDeltaMovement();
                e.setDeltaMovement(current.x + x, current.y + y, current.z + z);
                e.hurtMarked = true;
                return NONE;
            }
        });

        table.set("getPosition", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity e = (Entity) args.checkuserdata(1, Entity.class);
                return LuaValue.varargsOf(new LuaValue[] {
                        LuaValue.valueOf(e.getX()),
                        LuaValue.valueOf(e.getY()),
                        LuaValue.valueOf(e.getZ())
                });
            }
        });

        table.set("setPosition", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity e = (Entity) args.checkuserdata(1, Entity.class);
                double x = args.checkdouble(2);
                double y = args.checkdouble(3);
                double z = args.checkdouble(4);
                e.teleportTo(x, y, z);
                return NONE;
            }
        });


        table.set("getUUID", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity e = (Entity) entityArg.checkuserdata(Entity.class);
                return LuaValue.valueOf(e.getUUID().toString());
            }
        });

        table.set("kill", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity e = (Entity) entityArg.checkuserdata(Entity.class);
                e.kill(e.level() instanceof net.minecraft.server.level.ServerLevel serverLevel ? serverLevel : null);
                return NIL;
            }
        });

        table.set("getHeldItem", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                if (!(entity instanceof LivingEntity living)) {
                    return NIL;
                }
                ItemStack stack = living.getMainHandItem();
                return CoerceJavaToLua.coerce(stack);
            }
        });
    }
}