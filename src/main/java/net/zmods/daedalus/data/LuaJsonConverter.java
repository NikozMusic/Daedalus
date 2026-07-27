package net.zmods.daedalus.data;

import com.google.gson.*;
import org.luaj.vm2.*;


//This converts the tag system to and from json files for more optimized storage instead of just loading everything into ram all at once
public class LuaJsonConverter {

    public static JsonElement luaToJson(LuaValue value) {
        if (value.isnil()) return JsonNull.INSTANCE;
        if (value.isboolean()) return new JsonPrimitive(value.checkboolean());
        if (value.isint()) return new JsonPrimitive(value.checkint());
        if (value.isnumber()) return new JsonPrimitive(value.checkdouble());
        if (value.isstring()) return new JsonPrimitive(value.checkjstring());

        if (value.istable()) {
            LuaTable table = value.checktable();
            // Detect array-like tables (1..n consecutive integer keys)
            int len = table.length();
            boolean isArray = len > 0;
            if (isArray) {
                JsonArray array = new JsonArray();
                for (int i = 1; i <= len; i++) {
                    array.add(luaToJson(table.get(i)));
                }
                return array;
            } else {
                JsonObject obj = new JsonObject();
                LuaValue key = LuaValue.NIL;
                while (true) {
                    Varargs next = table.next(key);
                    key = next.arg1();
                    if (key.isnil()) break;
                    LuaValue val = next.arg(2);
                    obj.add(key.tojstring(), luaToJson(val));
                }
                return obj;
            }
        }

        //stringify anything it fails to process in case of emergency
        return new JsonPrimitive(value.tojstring());
    }

    public static LuaValue jsonToLua(JsonElement element) {
        if (element == null || element.isJsonNull()) return LuaValue.NIL;

        if (element.isJsonPrimitive()) {
            JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean()) return LuaValue.valueOf(prim.getAsBoolean());
            if (prim.isNumber()) return LuaValue.valueOf(prim.getAsDouble());
            return LuaValue.valueOf(prim.getAsString());
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            LuaTable table = new LuaTable();
            for (int i = 0; i < array.size(); i++) {
                table.set(i + 1, jsonToLua(array.get(i)));
            }
            return table;
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            LuaTable table = new LuaTable();
            for (String key : obj.keySet()) {
                table.set(key, jsonToLua(obj.get(key)));
            }
            return table;
        }

        return LuaValue.NIL;
    }
}