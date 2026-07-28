package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

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

    // Strips a single leading '/' since CommandDispatcher.execute expects a bare command,
    // unlike performPrefixedCommand which tolerates (and ignores) the slash.
    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    // Runs the command straight through Brigadier so we get the int result code back
    // (e.g. 1 for a plain success, a count for things like "execute if ...", 0 on failure),
    // instead of performPrefixedCommand which discards that result and only logs internally.
    // Returns two Lua values: result (int), output (string).
    private static Varargs runCommand(CommandSourceStack source, String command, StringBuilder output) {
        try {
            int result = source.getServer().getCommands().getDispatcher().execute(stripSlash(command), source);
            return LuaValue.varargsOf(
                    LuaValue.valueOf(result),
                    LuaValue.valueOf(output.toString())
            );
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            // Syntax/argument errors are normal Brigadier failures, not bugs - no stack trace needed
            return LuaValue.varargsOf(
                    LuaValue.valueOf(0),
                    LuaValue.valueOf(e.getMessage())
            );
        } catch (Exception e) {
            System.err.println("[Daedalus] Command error: " + e.getMessage());
            e.printStackTrace();
            return LuaValue.varargsOf(
                    LuaValue.valueOf(0),
                    LuaValue.valueOf("")
            );
        }
    }

    @Override
    public void register(LuaTable table, Globals globals) {
        // command.execute("say hi") -> result:int, output:string
        table.set("execute", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String command = args.checkjstring(1);
                StringBuilder output = new StringBuilder();
                CommandSourceStack source = server.createCommandSourceStack()
                        .withSource(capturingSource(output));
                return runCommand(source, command, output);
            }
        });

        // command.executeAs(entity, "say hi") -> result:int, output:string
        table.set("executeAs", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity entity = (Entity) args.checkuserdata(1, Entity.class);
                String command = args.checkjstring(2);
                StringBuilder output = new StringBuilder();

                CommandSourceStack source = server.createCommandSourceStack()
                        .withPosition(entity.position())
                        .withSource(capturingSource(output));
                return runCommand(source, command, output);
            }
        });
    }
}