package net.zmods.daedalus.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.Container;

public class DaedalusChestMenu extends ChestMenu {

    public interface ClickHandler {
        void onClick(Player player, int slot, ContainerInput clickType, int button);
    }

    private ClickHandler clickHandler;

    public DaedalusChestMenu(MenuType<ChestMenu> menuType, int syncId, Inventory playerInventory, Container container, int rows) {
        super(menuType, syncId, playerInventory, container, rows);
    }

    public void setClickHandler(ClickHandler handler) {
        this.clickHandler = handler;
    }


    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (clickHandler != null) {
            clickHandler.onClick(player, slotId, clickType, button);
        }
        // no super call: vanilla item transfer, pickup, and drag logic never runs
    }
}