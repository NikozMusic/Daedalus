package net.zmods.daedalus.event;

import java.util.*;
import org.luaj.vm2.LuaValue;
import net.minecraft.world.entity.Entity;

//This is the thing that actually binds the events, EventApi just exposes it to Lua

public class EventBindingRegistry {

    private static final EventBindingRegistry INSTANCE = new EventBindingRegistry();

    public static EventBindingRegistry getInstance() {
        return INSTANCE;
    }

    public static class EventBinding {
        public String moduleId;
        public LuaValue function;
        public UUID entity; // null for global events

        public EventBinding(String moduleId, LuaValue function, UUID entity) {
            this.moduleId = moduleId;
            this.function = function;
            this.entity = entity;
        }
    }

    private final Map<Events, List<EventBinding>> globalBindings = new HashMap<>();
    private final Map<UUID, Map<Events, List<EventBinding>>> entityBindings = new HashMap<>();


    public boolean hasGlobalListeners(Events event) {
        List<EventBinding> bindings = globalBindings.get(event);
        return bindings != null && !bindings.isEmpty();
    }

    public boolean hasEntityListeners(Object entity, Events event) {
        if (!(entity instanceof Entity mcEntity)) {
            return false;
        }

        UUID uuid = mcEntity.getUUID();
        Map<Events, List<EventBinding>> entityMap = entityBindings.get(uuid);
        if (entityMap == null) return false;

        List<EventBinding> bindings = entityMap.get(event);
        return bindings != null && !bindings.isEmpty();
    }

    public void bindGlobalEvent(String moduleId, Events event, LuaValue function) {
        globalBindings.computeIfAbsent(event, k -> new ArrayList<>())
                .add(new EventBinding(moduleId, function, null));

        System.out.println("Module " + moduleId + " bound to global event: " + event.id);
    }


    public void bindEntityEvent(String moduleId, Object entity, Events event, LuaValue function) {
        if (!(entity instanceof Entity mcEntity)) {
            System.err.println("Attempted to bind entity event to non-entity object");
            return;
        }

        UUID uuid = mcEntity.getUUID();

        entityBindings
                .computeIfAbsent(uuid, k -> new HashMap<>())
                .computeIfAbsent(event, k -> new ArrayList<>())
                .add(new EventBinding(moduleId, function, uuid));

        System.out.println(
                "Module " + moduleId +
                        " bound to entity event: " + event.id +
                        " (" + mcEntity.getName().getString() + ")"
        );
    }


    public List<EventBinding> getGlobalBindings(Events event) {
        return new ArrayList<>(
                globalBindings.getOrDefault(event, Collections.emptyList())
        );
    }


    public List<EventBinding> getEntityBindings(Object entity, Events event) {
        if (!(entity instanceof Entity mcEntity)) {
            return Collections.emptyList();
        }

        UUID uuid = mcEntity.getUUID();

        Map<Events, List<EventBinding>> entityMap = entityBindings.get(uuid);

        if (entityMap == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(
                entityMap.getOrDefault(event, Collections.emptyList())
        );
    }


    public void unbindGlobalEvent(String moduleId, Events event) {
        List<EventBinding> bindings = globalBindings.get(event);

        if (bindings != null) {
            bindings.removeIf(
                    b -> b.moduleId.equals(moduleId)
            );
        }
    }


    public void unbindEntityEvent(String moduleId, Object entity, Events event) {
        if (!(entity instanceof Entity mcEntity)) {
            return;
        }

        UUID uuid = mcEntity.getUUID();

        Map<Events, List<EventBinding>> entityMap = entityBindings.get(uuid);

        if (entityMap != null) {
            List<EventBinding> bindings = entityMap.get(event);

            if (bindings != null) {
                bindings.removeIf(
                        b -> b.moduleId.equals(moduleId)
                );
            }
        }
    }


    public void clearAll() {
        globalBindings.clear();
        entityBindings.clear();
    }


    public void unbindGlobalEventByFunction(String moduleId, Events event, LuaValue function) {
        List<EventBinding> bindings = globalBindings.get(event);

        if (bindings != null) {
            bindings.removeIf(
                    b -> b.moduleId.equals(moduleId)
                            && b.function == function
            );
        }
    }
}