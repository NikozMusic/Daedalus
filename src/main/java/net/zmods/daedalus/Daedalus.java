package net.zmods.daedalus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.api.apis.*;
import net.zmods.daedalus.command.DaedalusCommand;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import net.zmods.daedalus.module.ModuleManager;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class Daedalus implements ModInitializer {
	public static final String MODID = "daedalus";
	private static final Logger LOGGER = LogUtils.getLogger();
	private static ModuleManager moduleManager;

	@Override
	public void onInitialize() {
		Config.load();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			LuaApiRegistry apiRegistry = new LuaApiRegistry();

			//Load all APIs as valid Java files in the project
			apiRegistry.registerApi(new CoreApi());
			apiRegistry.registerApi(new EventApi());
			apiRegistry.registerApi(new CommandApi(server));
			apiRegistry.registerApi(new EntityApi());
			apiRegistry.registerApi(new DataApi());
			apiRegistry.registerApi(new BlockApi());
			apiRegistry.registerApi(new PlayerApi(server));
			apiRegistry.registerApi(new GuiApi());
			apiRegistry.registerApi(new ItemApi());

			moduleManager = new ModuleManager(
					server.getServerDirectory().toFile(),
					apiRegistry,
					server
			);
			moduleManager.discoverAndLoadAll();
			LOGGER.info("Daedalus module system initialized");
		});

		if (Config.enableDaedalusCommand) {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
					DaedalusCommand.register(dispatcher));
		}

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			net.zmods.daedalus.event.TickTracker.increment();
			EventFirer.fireGlobalEvent(Events.TICK);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				EventFirer.fireGlobalEvent(Events.PLAYER_JOIN, CoerceJavaToLua.coerce(handler.getPlayer())));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				EventFirer.fireGlobalEvent(Events.PLAYER_LEAVE, CoerceJavaToLua.coerce(handler.getPlayer())));

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			EventFirer.fireGlobalEvent(Events.ENTITY_DAMAGE,
					CoerceJavaToLua.coerce(entity), LuaValue.valueOf(damageTaken));
			EventFirer.fireEntityEvent(entity, Events.ENTITY_DAMAGE,
					LuaValue.valueOf(damageTaken));
		});
	}

	public static ModuleManager getModuleManager() { return moduleManager; }
}