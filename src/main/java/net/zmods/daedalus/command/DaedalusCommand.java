package net.zmods.daedalus.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import net.zmods.daedalus.Daedalus;
import net.zmods.daedalus.module.DaedalusState;
import net.zmods.daedalus.module.LoadedModule;
import net.zmods.daedalus.module.ModuleManager;

import java.util.Collection;

public class DaedalusCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("daedalus")
                        .then(Commands.literal("reload").executes(ctx -> {
                            ModuleManager manager = Daedalus.getModuleManager();
                            manager.reloadAll();
                            ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r Reloaded all modules."), true);
                            return 1;
                        }))
                        .then(Commands.literal("modules").executes(ctx -> {
                            ModuleManager manager = Daedalus.getModuleManager();
                            Collection<LoadedModule> modules = manager.getAllModules();
                            if (modules.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r No modules loaded."), false);
                            } else {
                                ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r Loaded modules (" + modules.size() + "):"), false);
                                for (LoadedModule module : modules) {
                                    ctx.getSource().sendSuccess(() -> Component.literal(" - " + module.metadata.data.id
                                            + " §e(" + module.metadata.info.name + ")§r"), false);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§7§o   " + module.metadata.info.description), false);
                                }
                            }
                            return 1;
                        }))
                        .then(Commands.literal("debug").executes(ctx -> {
                            boolean newState = !DaedalusState.isDebug();
                            DaedalusState.setDebug(newState);
                            ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r Debug mode " + (newState ? "§aENABLED§r" : "§cDISABLED§r")), true);
                            return 1;
                        }))
                        .then(Commands.literal("pause").executes(ctx -> {
                            DaedalusState.setRunning(false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r Runtime paused."), true);
                            return 1;
                        }))
                        .then(Commands.literal("resume").executes(ctx -> {
                            DaedalusState.setRunning(true);
                            ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r Runtime resumed."), true);
                            return 1;
                        }))
                        .then(Commands.literal("info").executes(ctx -> {
                            String daedalusVersion = FabricLoader.getInstance()
                                    .getModContainer("daedalus")
                                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                                    .orElse("Unknown");

                            ctx.getSource().sendSuccess(() -> Component.literal("§9§l[Daedalus]§r Runtime Information:"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(" §7Daedalus Version: §f" + daedalusVersion), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(" §7Lua Version: §f5.2"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(" §7Lua§6J §7Version: §f3.0.1"), false);
                            return 1;
                        }))
        );
    }
}