package net.zmods.daedalus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Fabric doesn't expose a placement-side equivalent of PlayerBlockBreakEvents.AFTER, so this
// mixin fires BLOCK_PLACE right after BlockItem successfully places a block. If the compiler
// complains about the "placeBlock" method signature or its return type, check the decompiled
// BlockItem class for this MC version and adjust - this is the same iterative fix loop used
// for the rest of the 26.2 API surface.
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {

    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void daedalus$onPlaceBlock(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;

        Player player = context.getPlayer();
        if (player == null) return;

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        BlockPos pos = context.getClickedPos();

        EventFirer.fireGlobalEvent(Events.BLOCK_PLACE,
                CoerceJavaToLua.coerce(player),
                LuaValue.valueOf(pos.getX()), LuaValue.valueOf(pos.getY()), LuaValue.valueOf(pos.getZ()),
                LuaValue.valueOf(blockId.toString()));
    }
}