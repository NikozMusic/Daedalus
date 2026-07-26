package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class CommandApi implements LuaApiRegistry.LuaApiModule {
    private final MinecraftServer server;

    public CommandApi(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getNamespace() {
        return "command";
    }

    @Override
    public void register(LuaTable table, Globals globals) {
        table.set("execute", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue commandArg) {
                String command = commandArg.checkjstring();
                try {
                    CommandSourceStack source = server.createCommandSourceStack();
                    server.getCommands().performPrefixedCommand(source, command);
                    return LuaValue.valueOf(1);
                } catch (Exception e) {
                    System.err.println("[Daedalus] Command error: " + e.getMessage());
                    e.printStackTrace();
                    return LuaValue.valueOf(0);
                }
            }
        });

        table.set("executeAs", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue commandArg) {
                try {
                    Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                    String command = commandArg.checkjstring();

                    CommandSourceStack source = server.createCommandSourceStack()
                            .withPosition(entity.position());
                    server.getCommands().performPrefixedCommand(source, command);
                    return LuaValue.valueOf(1);
                } catch (Exception e) {
                    System.err.println("[Daedalus] Command error: " + e.getMessage());
                    e.printStackTrace();
                    return LuaValue.valueOf(0);
                }
            }
        });
    }
}