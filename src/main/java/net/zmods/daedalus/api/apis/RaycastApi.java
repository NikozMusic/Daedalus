package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Optional;

// Raycasting: block-only, entity-only, and combined (whichever hit is closer) casts,
// plus a convenience that fires from an entity's eyes along its current look vector.
//
// Every cast returns a single Lua table shaped like:
//   { hit = bool, type = "block"|"entity"|"none", x, y, z, distance,
//     blockId, face,   -- only present when type == "block"
//     entity           -- only present when type == "entity" (userdata)
//   }
//
// NOTE (26.2 API surface): ClipContext's constructor signature and Entity.getBoundingBox()/
// getEyePosition()/getLookAngle() are written here using the standard vanilla names. If the
// compiler complains about any of these (same story as ResourceLocation -> Identifier etc.),
// swap in whatever the real 26.2 name/signature turns out to be - the shape of the logic
// shouldn't need to change, just the method names.
public class RaycastApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "raycast";
    }

    // -- Result builders --

    private static LuaTable missResult() {
        LuaTable t = new LuaTable();
        t.set("hit", LuaValue.FALSE);
        t.set("type", LuaValue.valueOf("none"));
        return t;
    }

    private static LuaTable blockResult(ServerLevel level, BlockHitResult result, Vec3 from) {
        LuaTable t = new LuaTable();
        Vec3 hitPos = result.getLocation();
        BlockPos pos = result.getBlockPos();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        Direction face = result.getDirection();

        t.set("hit", LuaValue.TRUE);
        t.set("type", LuaValue.valueOf("block"));
        t.set("x", LuaValue.valueOf(hitPos.x));
        t.set("y", LuaValue.valueOf(hitPos.y));
        t.set("z", LuaValue.valueOf(hitPos.z));
        t.set("distance", LuaValue.valueOf(from.distanceTo(hitPos)));
        t.set("blockId", LuaValue.valueOf(blockId.toString()));
        t.set("face", LuaValue.valueOf(face != null ? face.getName() : "unknown"));
        return t;
    }

    private static LuaTable entityResult(Entity entity, Vec3 hitPos, Vec3 from) {
        LuaTable t = new LuaTable();
        t.set("hit", LuaValue.TRUE);
        t.set("type", LuaValue.valueOf("entity"));
        t.set("x", LuaValue.valueOf(hitPos.x));
        t.set("y", LuaValue.valueOf(hitPos.y));
        t.set("z", LuaValue.valueOf(hitPos.z));
        t.set("distance", LuaValue.valueOf(from.distanceTo(hitPos)));
        t.set("entity", CoerceJavaToLua.coerce(entity));
        return t;
    }

    // -- Raw clip helpers --

    private static BlockHitResult clipBlocks(ServerLevel level, Vec3 from, Vec3 to, boolean includeFluids) {
        // NOTE (26.2): the (Vec3, Vec3, Block, Fluid, Entity) overload forwards straight into
        // CollisionContext.of(entity) without a null-check in this version, so passing a null
        // Entity NPEs as soon as the level actually clips against it. Build the
        // CollisionContext directly instead - CollisionContext.empty() is exactly what a null
        // entity was meant to represent anyway (no shooter to exclude from collision shapes).
        ClipContext ctx = new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );
        return level.clip(ctx);
    }

    private record EntityHit(Entity entity, Vec3 pos) {}

    // Simple closest-AABB-intersection scan - no fancy broadphase, just every candidate
    // entity in the ray's bounding box. Fine for the occasional Lua-triggered raycast;
    // don't call this every tick for every player without thinking about cost first.
    private static EntityHit clipEntities(ServerLevel level, Vec3 from, Vec3 to, Entity exclude) {
        AABB searchBox = new AABB(from, to);

        Entity closest = null;
        Vec3 closestPos = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity candidate : level.getEntities(exclude, searchBox, e -> e.isPickable())) {
            Optional<Vec3> hit = candidate.getBoundingBox().clip(from, to);
            if (hit.isEmpty()) continue;

            double distSq = from.distanceToSqr(hit.get());
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = candidate;
                closestPos = hit.get();
            }
        }

        return closest != null ? new EntityHit(closest, closestPos) : null;
    }

    // Shared combine-and-pick-closest logic used by both ray() and rayFromEntity()
    private static LuaValue performRay(ServerLevel level, Vec3 from, Vec3 to, Entity exclude,
                                       boolean includeFluids, boolean includeEntities) {
        BlockHitResult blockHit = clipBlocks(level, from, to, includeFluids);
        boolean gotBlock = blockHit.getType() == HitResult.Type.BLOCK;
        double blockDist = gotBlock ? from.distanceTo(blockHit.getLocation()) : Double.MAX_VALUE;

        EntityHit entityHit = includeEntities ? clipEntities(level, from, to, exclude) : null;
        double entityDist = entityHit != null ? from.distanceTo(entityHit.pos()) : Double.MAX_VALUE;

        if (!gotBlock && entityHit == null) {
            return missResult();
        }
        if (entityHit != null && entityDist <= blockDist) {
            return entityResult(entityHit.entity(), entityHit.pos(), from);
        }
        return blockResult(level, blockHit, from);
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // raycast.blockRay(level, x1, y1, z1, x2, y2, z2, includeFluids?) -> result table
        table.set("blockRay", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                Vec3 from = new Vec3(args.checkdouble(2), args.checkdouble(3), args.checkdouble(4));
                Vec3 to = new Vec3(args.checkdouble(5), args.checkdouble(6), args.checkdouble(7));
                boolean includeFluids = args.narg() >= 8 && args.checkboolean(8);

                BlockHitResult result = clipBlocks(level, from, to, includeFluids);
                if (result.getType() != HitResult.Type.BLOCK) {
                    return missResult();
                }
                return blockResult(level, result, from);
            }
        });

        // raycast.entityRay(level, x1, y1, z1, x2, y2, z2, exclude?) -> result table
        table.set("entityRay", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                Vec3 from = new Vec3(args.checkdouble(2), args.checkdouble(3), args.checkdouble(4));
                Vec3 to = new Vec3(args.checkdouble(5), args.checkdouble(6), args.checkdouble(7));
                Entity exclude = (args.narg() >= 8 && args.arg(8).isuserdata(Entity.class))
                        ? (Entity) args.checkuserdata(8, Entity.class) : null;

                EntityHit hit = clipEntities(level, from, to, exclude);
                if (hit == null) return missResult();
                return entityResult(hit.entity(), hit.pos(), from);
            }
        });

        // raycast.ray(level, x1, y1, z1, x2, y2, z2, exclude?, includeFluids?, includeEntities?)
        // -> result table (whichever of block/entity is closer along the ray)
        table.set("ray", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerLevel level = (ServerLevel) args.checkuserdata(1, ServerLevel.class);
                Vec3 from = new Vec3(args.checkdouble(2), args.checkdouble(3), args.checkdouble(4));
                Vec3 to = new Vec3(args.checkdouble(5), args.checkdouble(6), args.checkdouble(7));
                Entity exclude = (args.narg() >= 8 && args.arg(8).isuserdata(Entity.class))
                        ? (Entity) args.checkuserdata(8, Entity.class) : null;
                boolean includeFluids = args.narg() >= 9 && args.checkboolean(9);
                boolean includeEntities = args.narg() < 10 || args.checkboolean(10);

                return performRay(level, from, to, exclude, includeFluids, includeEntities);
            }
        });

        // raycast.rayFromEntity(entity, distance, includeEntities?, includeFluids?) -> result table
        // Casts from the entity's eye position along its current look vector - handy for
        // "what is this player looking at" style checks. Automatically excludes the source
        // entity itself from entity hits.
        table.set("rayFromEntity", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity source = (Entity) args.checkuserdata(1, Entity.class);
                double distance = args.checkdouble(2);
                boolean includeEntities = args.narg() < 3 || args.checkboolean(3);
                boolean includeFluids = args.narg() >= 4 && args.checkboolean(4);

                if (!(source.level() instanceof ServerLevel level)) {
                    return error("Entity is not in a server level");
                }

                Vec3 from = source.getEyePosition();
                Vec3 look = source.getLookAngle();
                Vec3 to = from.add(look.scale(distance));

                return performRay(level, from, to, source, includeFluids, includeEntities);
            }
        });
    }
}