package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.event.EventBindingRegistry;
import net.zmods.daedalus.event.Events;
import net.zmods.daedalus.module.ModuleContext;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.OneArgFunction;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSource;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.List;

public class EventApi implements LuaApiRegistry.LuaApiModule {

    @Override
    public String getNamespace() {
        return "events";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // Events enum table -> event.Events.TICK, event.Events.PLAYER_JOIN, etc.
        LuaTable eventsTable = new LuaTable();
        for (Events e : Events.values()) {
            eventsTable.set(e.name(), LuaValue.valueOf(e.id));
        }
        table.set("Events", eventsTable);

        // event.bindGlobal(Events.TICK, function() ... end)
        table.set("bindGlobal", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue eventArg, LuaValue function) {
                Events ev = Events.fromId(eventArg.checkjstring());
                if (ev == null) return error("Unknown event: " + eventArg.checkjstring());
                if (!function.isfunction()) return error("Second argument must be a function");

                String moduleId = ModuleContext.getCurrentModuleId();
                EventBindingRegistry.getInstance().bindGlobalEvent(moduleId, ev, function);
                return NIL;
            }
        });

        // event.bindEntity(entity, Events.ENTITY_JUMP, function() ... end)
        table.set("bindEntity", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue eventArg, LuaValue function) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                Events ev = Events.fromId(eventArg.checkjstring());
                if (ev == null) return error("Unknown event: " + eventArg.checkjstring());
                if (!function.isfunction()) return error("Third argument must be a function");

                String moduleId = ModuleContext.getCurrentModuleId();
                EventBindingRegistry.getInstance().bindEntityEvent(moduleId, entity, ev, function);
                return NIL;
            }
        });

        // event.unbindGlobal(Events.TICK)
        table.set("unbindGlobal", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue eventArg) {
                Events ev = Events.fromId(eventArg.checkjstring());
                if (ev == null) return error("Unknown event: " + eventArg.checkjstring());

                String moduleId = ModuleContext.getCurrentModuleId();
                EventBindingRegistry.getInstance().unbindGlobalEvent(moduleId, ev);
                return NIL;
            }
        });

        // event.unbindEntity(entity, Events.ENTITY_JUMP)
        table.set("unbindEntity", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue eventArg) {
                Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                Events ev = Events.fromId(eventArg.checkjstring());
                if (ev == null) return error("Unknown event: " + eventArg.checkjstring());

                String moduleId = ModuleContext.getCurrentModuleId();
                EventBindingRegistry.getInstance().unbindEntityEvent(moduleId, entity, ev);
                return NIL;
            }
        });

        // event.once(Events.PLAYER_JOIN, function(player) ... end) - fires once then auto-unbinds
        table.set("once", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue eventArg, LuaValue function) {
                Events ev = Events.fromId(eventArg.checkjstring());
                if (ev == null) return error("Unknown event: " + eventArg.checkjstring());
                if (!function.isfunction()) return error("Second argument must be a function");

                String moduleId = ModuleContext.getCurrentModuleId();

                // Wrapper function that calls the real function then immediately unbinds itself
                LuaValue[] wrapperHolder = new LuaValue[1];
                LuaValue wrapper = new org.luaj.vm2.lib.VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs args) {
                        function.invoke(args);
                        EventBindingRegistry.getInstance().unbindGlobalEventByFunction(moduleId, ev, wrapperHolder[0]);
                        return LuaValue.NONE;
                    }
                };
                wrapperHolder[0] = wrapper;

                EventBindingRegistry.getInstance().bindGlobalEvent(moduleId, ev, wrapper);
                return NIL;
            }
        });


    }
}