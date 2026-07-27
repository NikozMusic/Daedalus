package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
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

    // Builds a CommandSource that captures every message sent to it instead of
    // broadcasting to console/ops, so we can hand the text back to Lua.
    private static CommandSource capturingSource(StringBuilder output) {
        return new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                if (output.length() > 0) output.append("\n");
                output.append(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
    }

    @Override
    public void register(LuaTable table, Globals globals) {
        table.set("execute", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue commandArg) {
                String command = commandArg.checkjstring();
                StringBuilder output = new StringBuilder();
                try {
                    CommandSourceStack source = server.createCommandSourceStack()
                            .withSource(capturingSource(output));
                    server.getCommands().performPrefixedCommand(source, command);
                    return LuaValue.valueOf(output.toString());
                } catch (Exception e) {
                    System.err.println("[Daedalus] Command error: " + e.getMessage());
                    e.printStackTrace();
                    return LuaValue.valueOf("");
                }
            }
        });

        table.set("executeAs", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue entityArg, LuaValue commandArg) {
                StringBuilder output = new StringBuilder();
                try {
                    Entity entity = (Entity) entityArg.checkuserdata(Entity.class);
                    String command = commandArg.checkjstring();

                    CommandSourceStack source = server.createCommandSourceStack()
                            .withPosition(entity.position())
                            .withSource(capturingSource(output));
                    server.getCommands().performPrefixedCommand(source, command);
                    return LuaValue.valueOf(output.toString());
                } catch (Exception e) {
                    System.err.println("[Daedalus] Command error: " + e.getMessage());
                    e.printStackTrace();
                    return LuaValue.valueOf("");
                }
            }
        });
    }
}