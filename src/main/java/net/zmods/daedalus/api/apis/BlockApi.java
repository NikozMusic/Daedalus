package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

public class BlockApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "block";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // block.get(level, x, y, z) -> "minecraft:stone"
        table.set("get", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                BlockPos pos = new BlockPos(
                        args.checkint(2), args.checkint(3), args.checkint(4)
                );
                BlockState state = level.getBlockState(pos);
                Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                return LuaValue.valueOf(id.toString());
            }
        });

        // block.set(level, x, y, z, "minecraft:stone")
        table.set("set", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                BlockPos pos = new BlockPos(
                        args.checkint(2), args.checkint(3), args.checkint(4)
                );
                String blockId = args.checkjstring(5);

                Identifier id;
                try {
                    id = Identifier.parse(blockId);
                } catch (Exception e) {
                    return LuaValue.error("Invalid block identifier: " + blockId);
                }

                var holder = BuiltInRegistries.BLOCK.get(id);
                if (holder.isEmpty()) {
                    return LuaValue.error("Unknown block: " + blockId);
                }

                Block block = holder.get().value();
                level.setBlock(pos, block.defaultBlockState(), 3);
                return LuaValue.NONE;
            }
        });

        // block.isAir(level, x, y, z) -> boolean
        table.set("isAir", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                BlockPos pos = new BlockPos(
                        args.checkint(2), args.checkint(3), args.checkint(4)
                );
                return LuaValue.valueOf(level.getBlockState(pos).isAir());
            }
        });

        // block.break(level, x, y, z) -> triggers natural destroy (drops + effects)
        table.set("break", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                BlockPos pos = new BlockPos(
                        args.checkint(2), args.checkint(3), args.checkint(4)
                );
                level.destroyBlock(pos, true);
                return LuaValue.NONE;
            }
        });

        // block.getLight(level, x, y, z) -> int 0-15
        table.set("getLight", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                BlockPos pos = new BlockPos(
                        args.checkint(2), args.checkint(3), args.checkint(4)
                );
                return LuaValue.valueOf(level.getMaxLocalRawBrightness(pos));
            }
        });

        // block.canSeeSky(level, x, y, z) -> boolean
        table.set("canSeeSky", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                BlockPos pos = new BlockPos(
                        args.checkint(2), args.checkint(3), args.checkint(4)
                );
                return LuaValue.valueOf(level.canSeeSky(pos));
            }
        });
    }
}