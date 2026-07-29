package net.zmods.daedalus;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
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
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;


public class Daedalus implements ModInitializer {

	public static final String MODID = "daedalus";
	public static final Logger LOGGER = LogUtils.getLogger();
	private static ModuleManager moduleManager;

	private static CommandDispatcher<CommandSourceStack> pendingDispatcher;
	private static CommandBuildContext pendingBuildContext;

	@Override
	public void onInitialize() {
		Config.load();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LuaApiRegistry apiRegistry = new LuaApiRegistry();

			//Load all APIs as valid Java files in the project
			apiRegistry.registerApi(new CoreApi());
			apiRegistry.registerApi(new EventApi());
			apiRegistry.registerApi(new CommandApi(server));
			apiRegistry.registerApi(new EntityApi(server));
			apiRegistry.registerApi(new DataApi());
			apiRegistry.registerApi(new BlockApi());
			apiRegistry.registerApi(new PlayerApi(server));
			apiRegistry.registerApi(new GuiApi());
			apiRegistry.registerApi(new ItemApi());
			apiRegistry.registerApi(new ServerApi(server));
			apiRegistry.registerApi(new MiniMessageApi(server));

			moduleManager = new ModuleManager(
					net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toFile(),
					apiRegistry,
					server
			);
			moduleManager.discoverAndLoadAll();
			if (pendingDispatcher != null) {
				moduleManager.setCommandContext(pendingDispatcher, pendingBuildContext);
			}
			LOGGER.info("Daedalus module system initialized");
		});

		if (Config.enableDaedalusCommand) {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
					DaedalusCommand.register(dispatcher));
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			pendingDispatcher = dispatcher;
			pendingBuildContext = registryAccess;
			if (moduleManager != null) {
				moduleManager.setCommandContext(dispatcher, registryAccess);
			}
		});

		//Event bindings
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

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			EventFirer.fireGlobalEvent(Events.BLOCK_BREAK,
					CoerceJavaToLua.coerce(player),
					LuaValue.valueOf(pos.getX()), LuaValue.valueOf(pos.getY()), LuaValue.valueOf(pos.getZ()),
					LuaValue.valueOf(blockId.toString()));
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			var pos = hitResult.getBlockPos();
			EventFirer.fireGlobalEvent(Events.BLOCK_INTERACT,
					CoerceJavaToLua.coerce(player),
					LuaValue.valueOf(pos.getX()), LuaValue.valueOf(pos.getY()), LuaValue.valueOf(pos.getZ()));
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			EventFirer.fireGlobalEvent(
					Events.ITEM_USE,
					CoerceJavaToLua.coerce(player),
					CoerceJavaToLua.coerce(player.getItemInHand(hand))
			);
			return InteractionResult.PASS;
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			EventFirer.fireGlobalEvent(
					Events.PLAYER_RESPAWN,
					CoerceJavaToLua.coerce(newPlayer)
			);
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			EventFirer.fireGlobalEvent(Events.ENTITY_DEATH, CoerceJavaToLua.coerce(entity));
			EventFirer.fireEntityEvent(entity, Events.ENTITY_DEATH);
			if (entity instanceof ServerPlayer) {
				EventFirer.fireGlobalEvent(Events.PLAYER_DEATH, CoerceJavaToLua.coerce(entity));
			}
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			EventFirer.fireGlobalEvent(
					Events.PLAYER_CHAT,
					CoerceJavaToLua.coerce(sender),
					LuaValue.valueOf(message.signedContent())
			);
		});
	}

	public static ModuleManager getModuleManager() { return moduleManager; }
}