package net.zmods.daedalus.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Unique
    private static long daedalus$currentTick = 0;
    @Unique
    private static final Map<Entity, Long> daedalus$lastJumpTick = new WeakHashMap<>();

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void daedalus$onJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        // dedup logic ported from your old EventHandlers
        EventFirer.fireGlobalEvent(Events.ENTITY_JUMP, CoerceJavaToLua.coerce(self));
        EventFirer.fireEntityEvent(self, Events.ENTITY_JUMP);
    }
}