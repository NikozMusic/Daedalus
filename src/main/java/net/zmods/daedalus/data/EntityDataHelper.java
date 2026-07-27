package net.zmods.daedalus.data;

import com.google.gson.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.world.entity.Entity;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.LuaTable;

public class EntityDataHelper {
    private static final Gson gson = new Gson();

    private static AttachmentTarget target(Entity entity) {
        return (AttachmentTarget) entity;
    }

    private static JsonObject loadRoot(Entity entity) {
        String raw = target(entity).getAttachedOrCreate(DaedalusAttachments.ENTITY_DATA);
        try {
            JsonElement parsed = JsonParser.parseString(raw);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static void saveRoot(Entity entity, JsonObject root) {
        target(entity).setAttached(DaedalusAttachments.ENTITY_DATA, gson.toJson(root));
    }

    private static JsonObject loadNamespace(Entity entity, String moduleId) {
        JsonObject root = loadRoot(entity);
        JsonElement ns = root.get(moduleId);
        return (ns != null && ns.isJsonObject()) ? ns.getAsJsonObject() : new JsonObject();
    }

    private static void saveNamespace(Entity entity, String moduleId, JsonObject nsData) {
        JsonObject root = loadRoot(entity);
        root.add(moduleId, nsData);
        saveRoot(entity, root);
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

    public static void set(Entity entity, String moduleId, String path, LuaValue value) {
        JsonObject ns = loadNamespace(entity, moduleId);
        setPath(ns, path, LuaJsonConverter.luaToJson(value));
        saveNamespace(entity, moduleId, ns);
    }

    public static LuaValue get(Entity entity, String moduleId, String path) {
        JsonElement value = getPath(loadNamespace(entity, moduleId), path);
        if (value == null) return LuaValue.NIL;
        return LuaJsonConverter.jsonToLua(value);
    }

    public static boolean has(Entity entity, String moduleId, String path) {
        return getPath(loadNamespace(entity, moduleId), path) != null;
    }

    public static void remove(Entity entity, String moduleId, String path) {
        JsonObject ns = loadNamespace(entity, moduleId);
        removePath(ns, path);
        saveNamespace(entity, moduleId, ns);
    }

    public static LuaValue list(Entity entity, String moduleId) {
        JsonObject ns = loadNamespace(entity, moduleId);
        LuaTable table = new LuaTable();
        int i = 1;
        for (String key : ns.keySet()) {
            table.set(i++, LuaValue.valueOf(key));
        }
        return table;
    }

    public static void clear(Entity entity, String moduleId) {
        saveNamespace(entity, moduleId, new JsonObject());
    }
}