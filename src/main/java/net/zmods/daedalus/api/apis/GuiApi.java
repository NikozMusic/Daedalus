package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Map;
import java.util.WeakHashMap;

public class GuiApi implements LuaApiRegistry.LuaApiModule {

    // Tracks the title we assigned to each open menu, since vanilla's ChestMenu
    // doesn't expose a reliable way to read the display name back out.
    private static final Map<AbstractContainerMenu, String> menuTitles = new WeakHashMap<>();

    @Override
    public String getNamespace() {
        return "gui";
    }

    private static MenuType<ChestMenu> menuTypeForRows(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> null;
        };
    }

    private static @NonNull MenuProvider buildProvider(int rows, String title, MenuType<ChestMenu> menuType) {
        SimpleContainer inventory = new SimpleContainer(rows * 9);
        Component titleComponent = Component.literal(title);

        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return titleComponent;
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
                return new ChestMenu(menuType, syncId, playerInventory, inventory, rows);
            }
        };
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // gui.open(player, "My Title", 3) -> opens a 3-row (27 slot) chest GUI, returns the menu
        table.set("open", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ServerPlayer sp = (ServerPlayer) args.checkuserdata(1, ServerPlayer.class);
                String title = args.checkjstring(2);
                int rows = args.checkint(3);

                MenuType<ChestMenu> menuType = menuTypeForRows(rows);
                if (menuType == null) {
                    return LuaValue.error("Invalid row count: " + rows + " (must be 1-6)");
                }

                MenuProvider provider = buildProvider(rows, title, menuType);
                sp.openMenu(provider);

                AbstractContainerMenu opened = sp.containerMenu;
                menuTitles.put(opened, title);

                return CoerceJavaToLua.coerce(opened);
            }
        });

        // gui.getOpen(player) -> currently open menu, or nil if just their own inventory
        table.set("getOpen", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                AbstractContainerMenu menu = sp.containerMenu;

                if (menu == sp.inventoryMenu) {
                    return NIL;
                }
                return CoerceJavaToLua.coerce(menu);
            }
        });

        // gui.getTitle(player) -> title string assigned via gui.open, or nil
        table.set("getTitle", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                AbstractContainerMenu menu = sp.containerMenu;

                if (menu == sp.inventoryMenu) {
                    return NIL;
                }

                String title = menuTitles.get(menu);
                return title != null ? LuaValue.valueOf(title) : NIL;
            }
        });

        // gui.close(player) -> force-closes whatever menu the player has open
        table.set("close", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {
                ServerPlayer sp = (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);
                sp.closeContainer();
                return NIL;
            }
        });

        // gui.setItem(menu, slot, itemStack) -> places an item in a slot (0-indexed)
        table.set("setItem", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue menuArg, LuaValue slotArg, LuaValue itemArg) {
                AbstractContainerMenu menu = (AbstractContainerMenu) menuArg.checkuserdata(AbstractContainerMenu.class);
                int slot = slotArg.checkint();
                ItemStack stack = (ItemStack) itemArg.checkuserdata(ItemStack.class);

                if (slot < 0 || slot >= menu.slots.size()) {
                    return error("Slot index out of range: " + slot + " (menu has " + menu.slots.size() + " slots)");
                }

                menu.getSlot(slot).set(stack);
                menu.broadcastChanges();
                return NIL;
            }
        });

        // gui.getItem(menu, slot) -> ItemStack in that slot, or nil if empty
        table.set("getItem", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue menuArg, LuaValue slotArg) {
                AbstractContainerMenu menu = (AbstractContainerMenu) menuArg.checkuserdata(AbstractContainerMenu.class);
                int slot = slotArg.checkint();

                if (slot < 0 || slot >= menu.slots.size()) {
                    return error("Slot index out of range: " + slot + " (menu has " + menu.slots.size() + " slots)");
                }

                ItemStack stack = menu.getSlot(slot).getItem();
                if (stack.isEmpty()) return NIL;
                return CoerceJavaToLua.coerce(stack);
            }
        });

        // gui.clearItem(menu, slot) -> removes whatever is in that slot
        table.set("clearItem", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue menuArg, LuaValue slotArg) {
                AbstractContainerMenu menu = (AbstractContainerMenu) menuArg.checkuserdata(AbstractContainerMenu.class);
                int slot = slotArg.checkint();

                if (slot < 0 || slot >= menu.slots.size()) {
                    return error("Slot index out of range: " + slot + " (menu has " + menu.slots.size() + " slots)");
                }

                menu.getSlot(slot).set(ItemStack.EMPTY);
                menu.broadcastChanges();
                return NIL;
            }
        });

        // gui.getSlotCount(menu) -> total number of slots (including the player's own inventory slots appended by ChestMenu)
        table.set("getSlotCount", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue menuArg) {
                AbstractContainerMenu menu = (AbstractContainerMenu) menuArg.checkuserdata(AbstractContainerMenu.class);
                return LuaValue.valueOf(menu.slots.size());
            }
        });
    }
}