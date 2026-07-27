package net.zmods.daedalus.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDropItemMixin {
    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"))
    private void daedalus$onDrop(ItemStack stack, boolean bool, CallbackInfoReturnable<ItemEntity> cir) {
        Player self = (Player)(Object) this;
        EventFirer.fireGlobalEvent(Events.ITEM_DROP,
                CoerceJavaToLua.coerce(self),
                CoerceJavaToLua.coerce(stack));
    }
}