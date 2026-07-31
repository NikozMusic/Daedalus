package net.zmods.daedalus.data;

import net.minecraft.nbt.*;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;


public class NbtLuaConverter {

    public static LuaValue nbtToLua(Tag tag) {
        if (tag == null) return LuaValue.NIL;

        return switch (tag) {
            case CompoundTag compound -> {
                LuaTable table = new LuaTable();
                for (String key : compound.keySet()) {
                    table.set(key, nbtToLua(compound.get(key)));
                }
                yield table;
            }
            case ListTag list -> {
                LuaTable table = new LuaTable();
                for (int i = 0; i < list.size(); i++) {
                    table.set(i + 1, nbtToLua(list.get(i)));
                }
                yield table;
            }
            case ByteArrayTag arr -> {
                LuaTable table = new LuaTable();
                byte[] bytes = arr.getAsByteArray();
                for (int i = 0; i < bytes.length; i++) table.set(i + 1, LuaValue.valueOf(bytes[i]));
                yield table;
            }
            case IntArrayTag arr -> {
                LuaTable table = new LuaTable();
                int[] ints = arr.getAsIntArray();
                for (int i = 0; i < ints.length; i++) table.set(i + 1, LuaValue.valueOf(ints[i]));
                yield table;
            }
            case LongArrayTag arr -> {
                LuaTable table = new LuaTable();
                long[] longs = arr.getAsLongArray();
                for (int i = 0; i < longs.length; i++) table.set(i + 1, LuaValue.valueOf(longs[i]));
                yield table;
            }
            case StringTag s -> LuaValue.valueOf(s.value());
            case ByteTag b -> LuaValue.valueOf(((NumericTag) b).byteValue());
            case ShortTag s -> LuaValue.valueOf(((NumericTag) s).shortValue());
            case IntTag i -> LuaValue.valueOf(((NumericTag) i).intValue());
            case LongTag l -> LuaValue.valueOf(((NumericTag) l).longValue());
            case FloatTag f -> LuaValue.valueOf(((NumericTag) f).floatValue());
            case DoubleTag d -> LuaValue.valueOf(((NumericTag) d).doubleValue());
            default -> LuaValue.valueOf(tag.toString());
        };
    }


    public static Tag luaToNbt(LuaValue value) {
        if (value.isnil()) return null;
        if (value.isboolean()) return ByteTag.valueOf(value.checkboolean());
        if (value.isint()) return IntTag.valueOf(value.checkint());
        if (value.isnumber()) return DoubleTag.valueOf(value.checkdouble());
        if (value.isstring()) return StringTag.valueOf(value.checkjstring());

        if (value.istable()) {
            LuaTable table = value.checktable();
            int len = table.length();
            if (len > 0) {
                ListTag list = new ListTag();
                for (int i = 1; i <= len; i++) {
                    Tag element = luaToNbt(table.get(i));
                    if (element != null) list.add(element);
                }
                return list;
            } else {
                CompoundTag compound = new CompoundTag();
                LuaValue key = LuaValue.NIL;
                while (true) {
                    Varargs next = table.next(key);
                    key = next.arg1();
                    if (key.isnil()) break;
                    Tag element = luaToNbt(next.arg(2));
                    if (element != null) compound.put(key.tojstring(), element);
                }
                return compound;
            }
        }

        return StringTag.valueOf(value.tojstring());
    }

    public static void merge(CompoundTag base, CompoundTag overlay) {
        for (String key : overlay.keySet()) {
            Tag overlayValue = overlay.get(key);
            Tag baseValue = base.get(key);
            if (overlayValue instanceof CompoundTag overlayCompound && baseValue instanceof CompoundTag baseCompound) {
                merge(baseCompound, overlayCompound);
            } else {
                base.put(key, overlayValue);
            }
        }
    }
}