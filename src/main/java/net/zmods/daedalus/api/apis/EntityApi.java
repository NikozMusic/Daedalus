package net.zmods.daedalus.api.apis;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.List;
import java.util.UUID;


public class EntityApi implements LuaApiRegistry.LuaApiModule {

    private final MinecraftServer server;

    public EntityApi(MinecraftServer server) {
        this.server = server;
    }

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

        // entity.getBySelector("@e[type=minecraft:zombie,distance=..10]") -> table of entities
        // Resolves against the overworld at (0,0,0) with no invoking entity - fine for
        // absolute selectors, but relative ones (distance=, @s, dx/dy/dz, etc.) need an anchor:
        //
        // entity.getBySelector(anchorEntity, "@e[distance=..5]") -> table of entities
        // Resolves the selector as if it were run from anchorEntity's position/level (like @s
        // refers to anchorEntity itself).
        table.set("getBySelector", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity anchor = null;
                String selectorStr;

                if (args.arg1().isuserdata(Entity.class)) {
                    anchor = (Entity) args.checkuserdata(1, Entity.class);
                    selectorStr = args.checkjstring(2);
                } else {
                    selectorStr = args.checkjstring(1);
                }

                CommandSourceStack source = server.createCommandSourceStack();
                if (anchor != null) {
                    source = source
                            .withEntity(anchor)
                            .withPosition(anchor.position())
                            .withLevel((ServerLevel) anchor.level());
                }

                try {
                    EntitySelectorParser parser = new EntitySelectorParser(new StringReader(selectorStr), true);
                    EntitySelector selector = parser.parse();
                    List<? extends Entity> found = selector.findEntities(source);

                    LuaTable result = new LuaTable();
                    int i = 1;
                    for (Entity e : found) {
                        result.set(i++, CoerceJavaToLua.coerce(e));
                    }
                    return result;
                } catch (CommandSyntaxException e) {
                    return error("Invalid selector '" + selectorStr + "': " + e.getMessage());
                }
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

        table.set("isPlayer", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Object obj = entityArg.checkuserdata();
                return LuaValue.valueOf(obj instanceof Player);
            }
        });

        table.set("getName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                return LuaValue.valueOf(entity.getName().getString());
            }
        });

        table.set("getHealth", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);

                if (!(entity instanceof LivingEntity living)) {
                    return NIL;
                }

                return LuaValue.valueOf(living.getHealth());
            }
        });

        table.set("setHealth", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity entity = (Entity) args.checkuserdata(1, Entity.class);

                if (!(entity instanceof LivingEntity living)) {
                    return NONE;
                }

                float health = (float) args.checkdouble(2);

                // Clamp between 0 and max health
                health = Math.clamp(health, 0, living.getMaxHealth());

                living.setHealth(health);
                return NONE;
            }
        });

        table.set("getMaxHealth", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);

                if (!(entity instanceof LivingEntity living)) {
                    return NIL;
                }

                return LuaValue.valueOf(living.getMaxHealth());
            }
        });

    }
}