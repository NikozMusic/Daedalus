package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class MiniMessageApi implements LuaApiRegistry.LuaApiModule {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final MinecraftServer server;

    public MiniMessageApi(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public String getNamespace() {
        return "text";
    }

    // Bridges an Adventure Component into a vanilla net.minecraft.network.chat.Component
    // by round-tripping through JSON + the vanilla Component codec.
    private net.minecraft.network.chat.Component toVanilla(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        JsonElement element = JsonParser.parseString(json);
        RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return ComponentSerialization.CODEC.parse(ops, element)
                .getOrThrow(); // throws if the JSON is somehow malformed - shouldn't happen with our own output
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // text.strip("<red>Hello</red>") -> "Hello" (tags removed, plain text only)
        table.set("strip", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue input) {
                Component component = MINI_MESSAGE.deserialize(input.checkjstring());
                return LuaValue.valueOf(PlainTextComponentSerializer.plainText().serialize(component));
            }
        });

        // text.send(player, "<gradient:red:blue>Hello!</gradient>")
        table.set("send", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg, LuaValue input) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                Component component = MINI_MESSAGE.deserialize(input.checkjstring());
                sp.sendSystemMessage(toVanilla(component));
                return NIL;
            }
        });
    }
}