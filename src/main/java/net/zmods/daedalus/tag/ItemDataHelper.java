package net.zmods.daedalus.tag;

import com.google.gson.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaTable;

public class ItemDataHelper {
    private static final String ROOT_KEY = "daedalus_data";
    private static final Gson gson = new Gson();

    private static JsonObject loadRoot(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return new JsonObject();
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(ROOT_KEY)) return new JsonObject();
        String raw = tag.getString(ROOT_KEY).orElse("{}");
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static void saveRoot(ItemStack stack, JsonObject root) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putString(ROOT_KEY, gson.toJson(root));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static JsonObject loadNamespace(ItemStack stack, String moduleId) {
        JsonObject root = loadRoot(stack);
        JsonElement ns = root.get(moduleId);
        return (ns != null && ns.isJsonObject()) ? ns.getAsJsonObject() : new JsonObject();
    }

    private static void saveNamespace(ItemStack stack, String moduleId, JsonObject nsData) {
        JsonObject root = loadRoot(stack);
        root.add(moduleId, nsData);
        saveRoot(stack, root);
    }

    private static void setPath(JsonObject obj, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject current = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = current.get(parts[i]);
            if (next == null || !next.isJsonObject()) {
                next = new JsonObject();
                current.add(parts[i], next);
            }
            current = next.getAsJsonObject();
        }
        current.add(parts[parts.length - 1], value);
    }

    private static JsonElement getPath(JsonObject obj, String path) {
        String[] parts = path.split("\\.");
        JsonElement current = obj;
        for (String part : parts) {
            if (current == null || !current.isJsonObject()) return null;
            current = current.getAsJsonObject().get(part);
        }
        return current;
    }

    private static void removePath(JsonObject obj, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonElement next = current.get(parts[i]);
            if (next == null || !next.isJsonObject()) return;
            current = next.getAsJsonObject();
        }
        current.remove(parts[parts.length - 1]);
    }

    public static void set(ItemStack stack, String moduleId, String path, LuaValue value) {
        JsonObject ns = loadNamespace(stack, moduleId);
        setPath(ns, path, LuaJsonConverter.luaToJson(value));
        saveNamespace(stack, moduleId, ns);
    }

    public static LuaValue get(ItemStack stack, String moduleId, String path) {
        JsonElement value = getPath(loadNamespace(stack, moduleId), path);
        if (value == null) return LuaValue.NIL;
        return LuaJsonConverter.jsonToLua(value);
    }

    public static boolean has(ItemStack stack, String moduleId, String path) {
        return getPath(loadNamespace(stack, moduleId), path) != null;
    }

    public static void remove(ItemStack stack, String moduleId, String path) {
        JsonObject ns = loadNamespace(stack, moduleId);
        removePath(ns, path);
        saveNamespace(stack, moduleId, ns);
    }

    public static LuaValue list(ItemStack stack, String moduleId) {
        JsonObject ns = loadNamespace(stack, moduleId);
        LuaTable table = new LuaTable();
        int i = 1;
        for (String key : ns.keySet()) {
            table.set(i++, LuaValue.valueOf(key));
        }
        return table;
    }

    public static void clear(ItemStack stack, String moduleId) {
        saveNamespace(stack, moduleId, new JsonObject());
    }
}