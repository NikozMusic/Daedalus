package net.zmods.daedalus.event;

import net.zmods.daedalus.module.DaedalusState;
import net.zmods.daedalus.module.ModuleContext;
import org.luaj.vm2.LuaValue;
import net.minecraft.world.entity.Entity;

public class EventFirer {

    public static void fireGlobalEvent(Events event, LuaValue... args) {
        if (!DaedalusState.isRunning()) return;

        EventBindingRegistry registry = EventBindingRegistry.getInstance();
        if (!registry.hasGlobalListeners(event)) return; // fast path - nothing to do

        if (DaedalusState.isDebug()) {
            System.out.println("[Daedalus][DEBUG] Firing global event: " + event.id);
        }

        for (EventBindingRegistry.EventBinding binding : registry.getGlobalBindings(event)) {
            ModuleContext.set(binding.moduleId);
            try {
                binding.function.invoke(LuaValue.varargsOf(args));
            } catch (Exception e) {
                System.err.println("[Daedalus] Error firing event " + event.id + " in module " + binding.moduleId);
                e.printStackTrace();
            } finally {
                ModuleContext.clear();
            }
        }
    }

    public static void fireEntityEvent(Entity entity, Events event, LuaValue... args) {
        if (!DaedalusState.isRunning()) return;

        EventBindingRegistry registry = EventBindingRegistry.getInstance();
        if (!registry.hasEntityListeners(entity, event)) return; // fast path - nothing to do

        if (DaedalusState.isDebug()) {
            System.out.println("[Daedalus][DEBUG] Firing entity event: " + event.id + " on " + entity.getName().getString());
        }

        for (EventBindingRegistry.EventBinding binding : registry.getEntityBindings(entity, event)) {
            ModuleContext.set(binding.moduleId);
            try {
                binding.function.invoke(LuaValue.varargsOf(args));
            } catch (Exception e) {
                System.err.println("[Daedalus] Error firing event " + event.id + " in module " + binding.moduleId);
                e.printStackTrace();
            } finally {
                ModuleContext.clear();
            }
        }
    }
}