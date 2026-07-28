package net.zmods.daedalus.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Tokenizes a raw command argument string and converts each token into the most
// useful Lua representation it can - numbers, booleans, resolved players/entities
// for basic (non-bracketed) selectors, or plain strings otherwise. Deliberately
// simple: no selector filters like @e[type=...] - anything more advanced is
// expected to be handled in Lua using the entity/player APIs directly.
public class CommandParser {

    // Splits on whitespace but keeps "quoted strings" and 'quoted strings' together as one token
    public static List<String> tokenize(String raw) {
        List<String> tokens = new ArrayList<>();
        if (raw == null || raw.isBlank()) return tokens;

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) tokens.add(current.toString());
        return tokens;
    }

    public static LuaTable toLuaArgs(CommandSourceStack source, String raw) {
        LuaTable table = new LuaTable();
        List<String> tokens = tokenize(raw);
        int i = 1;
        for (String token : tokens) {
            table.set(i++, classify(source, token));
        }
        return table;
    }

    private static LuaValue classify(CommandSourceStack source, String token) {
        // basic selectors take priority - unambiguous syntax (@ prefix)
        LuaValue selectorResult = resolveBasicSelector(source, token);
        if (selectorResult != null) return selectorResult;

        // exact match against a currently online player name
        if (source.getServer() != null) {
            ServerPlayer sp = source.getServer().getPlayerList().getPlayerByName(token);
            if (sp != null) return CoerceJavaToLua.coerce(sp);
        }

        // boolean
        if (token.equalsIgnoreCase("true")) return LuaValue.valueOf(true);
        if (token.equalsIgnoreCase("false")) return LuaValue.valueOf(false);

        // number
        try {
            double d = Double.parseDouble(token);
            return LuaValue.valueOf(d);
        } catch (NumberFormatException ignored) {}

        // fallback: raw string
        return LuaValue.valueOf(token);
    }

    // Returns null if the token isn't one of the recognized selector literals
    // (as opposed to LuaValue.NIL, which means "it was a selector but resolved to nothing").
    private static LuaValue resolveBasicSelector(CommandSourceStack source, String token) {
        switch (token) {
            case "@s": {
                Entity self = source.getEntity();
                return self != null ? CoerceJavaToLua.coerce(self) : LuaValue.NIL;
            }
            case "@p": {
                ServerPlayer nearest = source.getServer() != null
                        ? source.getServer().getPlayerList().getPlayers().stream()
                        .min((a, b) -> Double.compare(
                                a.position().distanceToSqr(source.getPosition()),
                                b.position().distanceToSqr(source.getPosition())))
                        .orElse(null)
                        : null;
                return nearest != null ? CoerceJavaToLua.coerce(nearest) : LuaValue.NIL;
            }
            case "@r": {
                List<ServerPlayer> players = source.getServer() != null
                        ? source.getServer().getPlayerList().getPlayers()
                        : List.of();
                if (players.isEmpty()) return LuaValue.NIL;
                return CoerceJavaToLua.coerce(players.get(new Random().nextInt(players.size())));
            }
            case "@a": {
                LuaTable table = new LuaTable();
                int i = 1;
                if (source.getServer() != null) {
                    for (ServerPlayer sp : source.getServer().getPlayerList().getPlayers()) {
                        table.set(i++, CoerceJavaToLua.coerce(sp));
                    }
                }
                return table;
            }
            case "@e": {
                LuaTable table = new LuaTable();
                int i = 1;
                if (source.getLevel() instanceof ServerLevel level) {
                    for (Entity e : level.getAllEntities()) {
                        table.set(i++, CoerceJavaToLua.coerce(e));
                    }
                }
                return table;
            }
            default:
                return null; // not a recognized selector token
        }
    }
}