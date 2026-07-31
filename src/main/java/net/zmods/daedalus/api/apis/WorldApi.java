package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

public class WorldApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "world";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // world.explode(level, x, y, z, power, fire?, breakBlocks?)
        // fire defaults to false, breakBlocks defaults to true (destroys terrain like TNT).
        // Pass breakBlocks=false for a "damage only" explosion (e.g. a knockback-only effect).
        table.set("explode", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                double x = args.checkdouble(2);
                double y = args.checkdouble(3);
                double z = args.checkdouble(4);
                float power = (float) args.checkdouble(5);
                boolean fire = args.narg() >= 6 && args.checkboolean(6);
                boolean breakBlocks = args.narg() < 7 || args.checkboolean(7);

                Level.ExplosionInteraction interaction = breakBlocks
                        ? Level.ExplosionInteraction.TNT
                        : Level.ExplosionInteraction.NONE;

                level.explode(
                        null,                         // no entity source
                        null,                         // default damage source
                        (ExplosionDamageCalculator) null,
                        x, y, z,
                        power,
                        fire,
                        interaction
                );
                return NONE;
            }
        });

        // world.playSound(level, x, y, z, "minecraft:entity.generic.explode", volume, pitch, category?)
        // category defaults to "master". Broadcasts to every player in range.
        table.set("playSound", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                double x = args.checkdouble(2);
                double y = args.checkdouble(3);
                double z = args.checkdouble(4);
                String soundId = args.checkjstring(5);
                float volume = args.narg() >= 6 ? (float) args.checkdouble(6) : 1.0f;
                float pitch = args.narg() >= 7 ? (float) args.checkdouble(7) : 1.0f;
                String categoryStr = args.narg() >= 8 ? args.checkjstring(8) : "master";

                Identifier id;
                try {
                    id = Identifier.parse(soundId);
                } catch (Exception e) {
                    return LuaValue.error("Invalid sound identifier: " + soundId);
                }

                var holder = BuiltInRegistries.SOUND_EVENT.get(id);
                if (holder.isEmpty()) {
                    return LuaValue.error("Unknown sound: " + soundId);
                }

                SoundSource category;
                try {
                    category = SoundSource.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return LuaValue.error("Unknown sound category: " + categoryStr);
                }

                SoundEvent sound = holder.get().value();
                level.playSound(null, x, y, z, sound, category, volume, pitch);
                return NONE;
            }
        });

        // world.spawnParticles(level, "minecraft:flame", x, y, z, count, dx, dy, dz, speed)
        // dx/dy/dz/speed control the random spread & velocity per vanilla particle semantics.
        // Only simple (parameterless) particle types are supported here
        table.set("spawnParticles", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                String particleId = args.checkjstring(2);
                double x = args.checkdouble(3);
                double y = args.checkdouble(4);
                double z = args.checkdouble(5);
                int count = args.checkint(6);
                double dx = args.narg() >= 7 ? args.checkdouble(7) : 0.0;
                double dy = args.narg() >= 8 ? args.checkdouble(8) : 0.0;
                double dz = args.narg() >= 9 ? args.checkdouble(9) : 0.0;
                double speed = args.narg() >= 10 ? args.checkdouble(10) : 0.0;

                Identifier id;
                try {
                    id = Identifier.parse(particleId);
                } catch (Exception e) {
                    return LuaValue.error("Invalid particle identifier: " + particleId);
                }

                var holder = BuiltInRegistries.PARTICLE_TYPE.get(id);
                if (holder.isEmpty()) {
                    return LuaValue.error("Unknown particle: " + particleId);
                }

                if (!(holder.get().value() instanceof SimpleParticleType particle)) {
                    return LuaValue.error("Particle '" + particleId + "' requires extra data and isn't supported by spawnParticles yet");
                }

                level.sendParticles((ParticleOptions) particle, x, y, z, count, dx, dy, dz, speed);
                return NONE;
            }
        });

        // world.getTime(level) -> int (current day time, 0-24000)
        table.set("getTime", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                return LuaValue.valueOf(level.getGameTime());
            }
        });

        // world.isRaining(level) -> boolean
        table.set("isRaining", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                return LuaValue.valueOf(level.isRaining());
            }
        });

        // world.isThundering(level) -> boolean
        table.set("isThundering", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                return LuaValue.valueOf(level.isThundering());
            }
        });

        // world.setDifficulty(level, "hard")
        table.set("setDifficulty", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                String diffStr = args.checkjstring(2);

                Difficulty difficulty = switch (diffStr.toLowerCase()) {
                    case "peaceful" -> Difficulty.PEACEFUL;
                    case "easy" -> Difficulty.EASY;
                    case "normal" -> Difficulty.NORMAL;
                    case "hard" -> Difficulty.HARD;
                    default -> null;
                };

                if (difficulty == null) {
                    return LuaValue.error("Unknown difficulty: " + diffStr);
                }

                level.getServer().setDifficulty(difficulty, true);
                return NONE;
            }
        });

        // world.getDifficulty(level) -> "peaceful"/"easy"/"normal"/"hard"
        table.set("getDifficulty", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                return LuaValue.valueOf(level.getDifficulty().name().toLowerCase());
            }
        });
    }
}