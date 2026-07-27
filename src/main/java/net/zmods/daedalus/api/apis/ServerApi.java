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

public class ServerApi implements LuaApiRegistry.LuaApiModule {
    private final MinecraftServer server;

    public ServerApi(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getNamespace() {
        return "server";
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // server.getOnline() -> table of player entities (userdata)
        table.set("getOnlinePlayers", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaTable result = new LuaTable();
                int i = 1;
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    result.set(i++, CoerceJavaToLua.coerce(sp));
                }
                return result;
            }
        });

        // server.stop()
        table.set("stop", new org.luaj.vm2.lib.ZeroArgFunction() {
            @Override
            public LuaValue call() {
                server.halt(false);
                return NIL;
            }
        });

    }
}