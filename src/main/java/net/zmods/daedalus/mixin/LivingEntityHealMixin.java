package net.zmods.daedalus.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import net.zmods.daedalus.event.TickTracker;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {
    @Inject(method = "heal", at = @At("HEAD"))
    private void daedalus$onHeal(float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object) this;
        EventFirer.fireGlobalEvent(Events.ENTITY_HEAL,
                CoerceJavaToLua.coerce(self), LuaValue.valueOf(amount));
        EventFirer.fireEntityEvent(self, Events.ENTITY_HEAL, LuaValue.valueOf(amount));
    }
}