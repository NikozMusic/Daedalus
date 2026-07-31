package net.zmods.daedalus.api.apis;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.zmods.daedalus.Config;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;

public class CommandApi implements LuaApiRegistry.LuaApiModule {

    //Helpers
    private enum ExecuteLevel {
        OVERWORLD,
        NETHER,
        END,
        ALL
    }

    private ExecuteLevel parseLevel(String level) {
        return switch (level.toLowerCase()) {
            case "overworld" -> ExecuteLevel.OVERWORLD;
            case "nether" -> ExecuteLevel.NETHER;
            case "end" -> ExecuteLevel.END;
            case "all" -> ExecuteLevel.ALL;
            default -> throw new IllegalArgumentException("Unknown level: " + level);
        };
    }

    private ServerLevel getLevel(ExecuteLevel level) {
        return switch (level) {
            case OVERWORLD -> server.overworld();
            case NETHER -> server.getLevel(ServerLevel.NETHER);
            case END -> server.getLevel(ServerLevel.END);
            default -> null;
        };
    }

    private CommandSourceStack createSource(ServerLevel level, StringBuilder output) {
        CommandSourceStack source = server.createCommandSourceStack()
                .withSource(capturingSource(output));

        if (level != null) {
            source = source.withLevel(level);
        }

        return source;
    }


    //Main part
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

    // Caps captured command output at Config.maxCommandOutputChars so a spammy
    // command (e.g. "execute run say ..." spam, big /forceload, etc.) can't
    // hand Lua an unbounded string.
    private static String truncateOutput(String output) {
        int max = Config.maxCommandOutputChars;
        if (output.length() <= max) return output;
        return output.substring(0, max) + "... [truncated, " + (output.length() - max) + " more chars]";
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
                    LuaValue.valueOf(truncateOutput(output.toString()))
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
                CommandSourceStack source = createSource(server.overworld(), output);
                return runCommand(source, command, output);
            }        });

        // command.executeAs(entity, "say hi") -> result:int, output:string
        table.set("executeAs", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Entity entity = (Entity) args.checkuserdata(1, Entity.class);
                String command = args.checkjstring(2);
                StringBuilder output = new StringBuilder();

                CommandSourceStack source = server.createCommandSourceStack()
                        .withPosition(entity.position())
                        .withLevel((ServerLevel) entity.level())
                        .withSource(capturingSource(output));
                return runCommand(source, command, output);
            }
        });

        // command.executeIn("overworld", "say hi") -> result:int, output:string
        table.set("executeIn", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String levelName = args.checkjstring(1);
                String command = args.checkjstring(2);
                ExecuteLevel level = parseLevel(levelName);
                StringBuilder output = new StringBuilder();
                if (level == ExecuteLevel.ALL) {
                    int result = 0;

                    for (ExecuteLevel dimension : new ExecuteLevel[]{
                            ExecuteLevel.OVERWORLD,
                            ExecuteLevel.NETHER,
                            ExecuteLevel.END
                    }) {
                        ServerLevel serverLevel = getLevel(dimension);
                        if (serverLevel == null)
                            continue;
                        Varargs response = runCommand(
                                createSource(serverLevel, output),
                                command,
                                output
                        );
                        result = Math.max(result, response.arg1().toint());
                    }
                    return LuaValue.varargsOf(
                            LuaValue.valueOf(result),
                            LuaValue.valueOf(truncateOutput(output.toString()))
                    );
                }
                ServerLevel serverLevel = getLevel(level);
                return runCommand(
                        createSource(serverLevel, output),
                        command,
                        output
                );
            }
        });

        // command.executeAt(x, y, z, "command")
        // or
        // command.executeAt("level", x, y, z, "command")
        table.set("executeAt", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                StringBuilder output = new StringBuilder();

                CommandSourceStack source;

                int offset;

                // executeAt("nether", x, y, z, command)
                if (args.arg1().isstring()) {
                    ExecuteLevel level = parseLevel(args.checkjstring(1));

                    double x = args.checkdouble(2);
                    double y = args.checkdouble(3);
                    double z = args.checkdouble(4);
                    String command = args.checkjstring(5);
                    ServerLevel serverLevel = getLevel(level);
                    source = createSource(serverLevel, output)
                            .withPosition(new Vec3(x, y, z));
                    return runCommand(source, command, output);
                }

                // executeAt(x, y, z, command)
                double x = args.checkdouble(1);
                double y = args.checkdouble(2);
                double z = args.checkdouble(3);
                String command = args.checkjstring(4);
                source = createSource(server.overworld(), output)
                        .withPosition(new Vec3(x, y, z));
                return runCommand(source, command, output);
            }
        });
    }
}