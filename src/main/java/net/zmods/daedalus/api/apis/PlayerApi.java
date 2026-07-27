package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import com.mojang.authlib.GameProfile;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Optional;

public class PlayerApi implements LuaApiRegistry.LuaApiModule {
    private final MinecraftServer server;

    public PlayerApi(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getNamespace() {
        return "players";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // player.getUUIDByName("Steve") -> uuid string, or nil if not online
        table.set("getUUIDByName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue nameArg) {
                String name = nameArg.checkjstring();
                ServerPlayer sp = server.getPlayerList().getPlayerByName(name);
                if (sp == null) return NIL;
                return LuaValue.valueOf(sp.getUUID().toString());
            }
        });

        // player.getByName("Steve") -> player entity, or nil if not online
        table.set("getByName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue nameArg) {
                ServerPlayer sp = server.getPlayerList().getPlayerByName(nameArg.checkjstring());
                if (sp == null) return NIL;
                return CoerceJavaToLua.coerce(sp);
            }
        });

        // player.isOnline("Steve") -> boolean
        table.set("isOnline", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue nameArg) {
                return LuaValue.valueOf(server.getPlayerList().getPlayerByName(nameArg.checkjstring()) != null);
            }
        });

        // player.getName(playerEntity) -> username string
        table.set("getName", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                return LuaValue.valueOf(sp.getGameProfile().name());
            }
        });

        // player.kick(playerEntity, "reason") -- reason optional
        table.set("kick", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerPlayer sp = (ServerPlayer) args.checkuserdata(1, ServerPlayer.class);
                String reason = args.narg() >= 2 ? args.checkjstring(2) : "Kicked by an operator.";
                sp.connection.disconnect(Component.literal(reason));
                return NONE;
            }
        });

        // player.sendMessage(playerEntity, "message")
        table.set("sendMessage", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg, LuaValue msgArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                sp.sendSystemMessage(Component.literal(msgArg.checkjstring()));
                return NIL;
            }
        });

        // player.getPing(playerEntity) -> int (ms)
        table.set("getPing", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                return LuaValue.valueOf(sp.connection.latency());
            }
        });

        // player.isOp(playerEntity) -> boolean
        /* CURRENTLY BROKEN
        table.set("isOp", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                return LuaValue.valueOf(server.getPlayerList().isOp(sp.getGameProfile()));
            }
        });
        */

        // player.getGameMode(playerEntity) -> "survival"/"creative"/"adventure"/"spectator"
        table.set("getGameMode", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                return LuaValue.valueOf(sp.gameMode.getGameModeForPlayer().getName());
            }
        });

        // player.setGameMode(playerEntity, "creative")
        table.set("setGameMode", new org.luaj.vm2.lib.TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg, LuaValue modeArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                String modeStr = modeArg.checkjstring();

                GameType mode = switch (modeStr.toLowerCase()) {
                    case "survival" -> GameType.SURVIVAL;
                    case "creative" -> GameType.CREATIVE;
                    case "adventure" -> GameType.ADVENTURE;
                    case "spectator" -> GameType.SPECTATOR;
                    default -> null;
                };

                if (mode == null) {
                    return error("Unknown game mode: " + modeStr);
                }

                sp.setGameMode(mode);
                return NIL;
            }
        });
    }
}