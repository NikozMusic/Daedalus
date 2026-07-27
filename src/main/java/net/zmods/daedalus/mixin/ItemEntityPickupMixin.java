package net.zmods.daedalus.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void daedalus$onPickup(Player player, CallbackInfo ci) {
        ItemEntity self = (ItemEntity)(Object) this;
        EventFirer.fireGlobalEvent(Events.ITEM_PICKUP,
                CoerceJavaToLua.coerce(player),
                CoerceJavaToLua.coerce(self.getItem()));
    }
}