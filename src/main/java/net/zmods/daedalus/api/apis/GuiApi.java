package net.zmods.daedalus.api.apis;

import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.gui.DaedalusChestMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
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

    private static final Map<AbstractContainerMenu, Component> menuTitles = new WeakHashMap<>();

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

    private static @NonNull MenuProvider buildProvider(
            int rows,
            Component title,
            MenuType<ChestMenu> menuType
    ) {
        SimpleContainer inventory = new SimpleContainer(rows * 9);

        return new MenuProvider() {

            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(
                    int syncId,
                    Inventory playerInventory,
                    Player player
            ) {
                return new DaedalusChestMenu(
                        menuType,
                        syncId,
                        playerInventory,
                        inventory,
                        rows
                );
            }
        };
    }

    private static String clickTypeToString(ContainerInput clickType, int button) {
        return switch (clickType) {
            case PICKUP -> button == 0 ? "left" : "right";
            case QUICK_MOVE -> button == 0 ? "shift_left" : "shift_right";
            case SWAP -> "hotbar_swap";
            case CLONE -> "middle";
            case THROW -> button == 0 ? "drop" : "drop_all";
            case QUICK_CRAFT -> "drag";
            case PICKUP_ALL -> "double_click";
            default -> "other";
        };
    }

    @Override
    public void register(LuaTable table, Globals globals) {

        // gui.open(player, title, rows)
        table.set("open", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {

                ServerPlayer sp =
                        (ServerPlayer) args.checkuserdata(1, ServerPlayer.class);

                Component title;

                if (args.arg(2).isuserdata(Component.class)) {
                    title = (Component) args.checkuserdata(2, Component.class);
                } else {
                    title = Component.literal(args.checkjstring(2));
                }

                int rows = args.checkint(3);

                MenuType<ChestMenu> menuType = menuTypeForRows(rows);

                if (menuType == null) {
                    return LuaValue.error(
                            "Invalid row count: " + rows + " (must be 1-6)"
                    );
                }

                MenuProvider provider =
                        buildProvider(rows, title, menuType);

                sp.openMenu(provider);

                AbstractContainerMenu opened = sp.containerMenu;

                menuTitles.put(opened, title);

                return CoerceJavaToLua.coerce(opened);
            }
        });


        // gui.getOpen(player)
        table.set("getOpen", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {

                ServerPlayer sp =
                        (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);

                AbstractContainerMenu menu = sp.containerMenu;

                if (menu == sp.inventoryMenu)
                    return NIL;

                return CoerceJavaToLua.coerce(menu);
            }
        });


        // gui.getTitle(player)
        table.set("getTitle", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {

                ServerPlayer sp =
                        (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);

                AbstractContainerMenu menu = sp.containerMenu;

                if (menu == sp.inventoryMenu)
                    return NIL;

                Component title = menuTitles.get(menu);

                if (title == null)
                    return NIL;

                return CoerceJavaToLua.coerce(title);
            }
        });


        // gui.close(player)
        table.set("close", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue playerArg) {

                ServerPlayer sp =
                        (ServerPlayer) playerArg.checkuserdata(ServerPlayer.class);

                sp.closeContainer();

                return NIL;
            }
        });


        // gui.setItem(menu, slot, item)
        table.set("setItem", new ThreeArgFunction() {
            @Override
            public LuaValue call(
                    LuaValue menuArg,
                    LuaValue slotArg,
                    LuaValue itemArg
            ) {

                AbstractContainerMenu menu =
                        (AbstractContainerMenu)
                                menuArg.checkuserdata(AbstractContainerMenu.class);

                int slot = slotArg.checkint();

                ItemStack stack =
                        (ItemStack)
                                itemArg.checkuserdata(ItemStack.class);


                if (slot < 0 || slot >= menu.slots.size()) {
                    return error(
                            "Slot index out of range: "
                                    + slot
                                    + " (menu has "
                                    + menu.slots.size()
                                    + " slots)"
                    );
                }

                menu.getSlot(slot).set(stack);
                menu.broadcastChanges();

                return NIL;
            }
        });


        // gui.getItem(menu, slot)
        table.set("getItem", new TwoArgFunction() {
            @Override
            public LuaValue call(
                    LuaValue menuArg,
                    LuaValue slotArg
            ) {

                AbstractContainerMenu menu =
                        (AbstractContainerMenu)
                                menuArg.checkuserdata(AbstractContainerMenu.class);

                int slot = slotArg.checkint();


                if (slot < 0 || slot >= menu.slots.size()) {
                    return error(
                            "Slot index out of range: "
                                    + slot
                                    + " (menu has "
                                    + menu.slots.size()
                                    + " slots)"
                    );
                }


                ItemStack stack = menu.getSlot(slot).getItem();

                if (stack.isEmpty())
                    return NIL;

                return CoerceJavaToLua.coerce(stack);
            }
        });


        // gui.clearItem(menu, slot)
        table.set("clearItem", new TwoArgFunction() {
            @Override
            public LuaValue call(
                    LuaValue menuArg,
                    LuaValue slotArg
            ) {

                AbstractContainerMenu menu =
                        (AbstractContainerMenu)
                                menuArg.checkuserdata(AbstractContainerMenu.class);

                int slot = slotArg.checkint();


                if (slot < 0 || slot >= menu.slots.size()) {
                    return error(
                            "Slot index out of range: "
                                    + slot
                                    + " (menu has "
                                    + menu.slots.size()
                                    + " slots)"
                    );
                }


                menu.getSlot(slot).set(ItemStack.EMPTY);
                menu.broadcastChanges();

                return NIL;
            }
        });


        // gui.getSlotCount(menu)
        table.set("getSlotCount", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue menuArg) {

                AbstractContainerMenu menu =
                        (AbstractContainerMenu)
                                menuArg.checkuserdata(AbstractContainerMenu.class);

                return LuaValue.valueOf(menu.slots.size());
            }
        });


        // gui.onClick(menu, function(player, slot, clickType))
        table.set("onClick", new TwoArgFunction() {
            @Override
            public LuaValue call(
                    LuaValue menuArg,
                    LuaValue function
            ) {

                AbstractContainerMenu menu =
                        (AbstractContainerMenu)
                                menuArg.checkuserdata(AbstractContainerMenu.class);


                if (!(menu instanceof DaedalusChestMenu daedalusMenu)) {
                    return error(
                            "onClick can only be used on menus created via gui.open"
                    );
                }


                if (!function.isfunction()) {
                    return error(
                            "Second argument must be a function"
                    );
                }


                daedalusMenu.setClickHandler(
                        (player, slot, clickType, button) -> {

                            String typeStr =
                                    clickTypeToString(clickType, button);

                            function.invoke(
                                    LuaValue.varargsOf(
                                            new LuaValue[]{
                                                    CoerceJavaToLua.coerce(player),
                                                    LuaValue.valueOf(slot),
                                                    LuaValue.valueOf(typeStr)
                                            }
                                    )
                            );
                        }
                );

                return NIL;
            }
        });
    }
}