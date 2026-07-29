package net.zmods.daedalus;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec3;
import net.zmods.daedalus.api.LuaApiRegistry;
import net.zmods.daedalus.api.apis.*;
import net.zmods.daedalus.command.DaedalusCommand;
import net.zmods.daedalus.event.EventBindingRegistry;
import net.zmods.daedalus.event.EventFirer;
import net.zmods.daedalus.event.Events;
import net.zmods.daedalus.event.TickTracker;
import net.zmods.daedalus.module.ModuleManager;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class Daedalus implements ModInitializer {

	public static final String MODID = "daedalus";
	public static final Logger LOGGER = LogUtils.getLogger();
	private static ModuleManager moduleManager;

	private static CommandDispatcher<CommandSourceStack> pendingDispatcher;
	private static CommandBuildContext pendingBuildContext;

	// Tracks last-known position per player for ENTITY_MOVE - scoped to players only and
	// gated by a displacement threshold to keep the per-tick cost bounded. Do not extend this
	// to all entities without a much stricter opt-in/throttling mechanism.
	private static final Map<UUID, Vec3> lastPlayerPositions = new HashMap<>();
	private static final double ENTITY_MOVE_THRESHOLD_SQ = 0.0025; // ~0.05 blocks, filters jitter

	@Override
	public void onInitialize() {
		Config.load();

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
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

		// Server lifecycle
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
				EventFirer.fireGlobalEvent(Events.SERVER_START));

		ServerLifecycleEvents.SERVER_STOPPING.register(server ->
				EventFirer.fireGlobalEvent(Events.SERVER_STOP));

		//Event bindings
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TickTracker.increment();
			EventFirer.fireGlobalEvent(Events.TICK);

			// ENTITY_MOVE - players only, threshold-gated. Skip the position-diff work entirely
			// if nothing is listening (globally or on any specific entity).
			EventBindingRegistry registry = EventBindingRegistry.getInstance();
			if (registry.hasGlobalListeners(Events.ENTITY_MOVE)
					|| registry.hasAnyEntityListenersForEvent(Events.ENTITY_MOVE)) {
				for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
					Vec3 current = sp.position();
					Vec3 last = lastPlayerPositions.get(sp.getUUID());

					if (last == null) {
						// First time we've seen this player - just record the baseline, don't fire
						lastPlayerPositions.put(sp.getUUID(), current);
						continue;
					}

					if (last.distanceToSqr(current) >= ENTITY_MOVE_THRESHOLD_SQ) {
						lastPlayerPositions.put(sp.getUUID(), current);
						EventFirer.fireGlobalEvent(Events.ENTITY_MOVE,
								CoerceJavaToLua.coerce(sp),
								LuaValue.valueOf(current.x), LuaValue.valueOf(current.y), LuaValue.valueOf(current.z));
						EventFirer.fireEntityEvent(sp, Events.ENTITY_MOVE,
								LuaValue.valueOf(current.x), LuaValue.valueOf(current.y), LuaValue.valueOf(current.z));
					}
				}
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				EventFirer.fireGlobalEvent(Events.PLAYER_JOIN, CoerceJavaToLua.coerce(handler.getPlayer())));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			EventFirer.fireGlobalEvent(Events.PLAYER_LEAVE, CoerceJavaToLua.coerce(handler.getPlayer()));
			// Clean up so we don't leak an entry per player that's ever connected
			if (handler.getPlayer() != null) {
				lastPlayerPositions.remove(handler.getPlayer().getUUID());
			}
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
			EventFirer.fireGlobalEvent(Events.ENTITY_DAMAGE,
					CoerceJavaToLua.coerce(entity), LuaValue.valueOf(damageTaken));
			EventFirer.fireEntityEvent(entity, Events.ENTITY_DAMAGE,
					LuaValue.valueOf(damageTaken));
		});

		// ENTITY_HURT - fires before damage is applied and is cancellable.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			boolean allowGlobal = EventFirer.fireCancellableGlobalEvent(Events.ENTITY_HURT,
					CoerceJavaToLua.coerce(entity), LuaValue.valueOf(amount));
			boolean allowEntity = EventFirer.fireCancellableEntityEvent(entity, Events.ENTITY_HURT,
					LuaValue.valueOf(amount));
			return allowGlobal && allowEntity;
		});

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
			EventFirer.fireGlobalEvent(Events.BLOCK_BREAK,
					CoerceJavaToLua.coerce(player),
					LuaValue.valueOf(pos.getX()), LuaValue.valueOf(pos.getY()), LuaValue.valueOf(pos.getZ()),
					LuaValue.valueOf(blockId.toString()));
		});

		// BLOCK_PLACE is fired from BlockItemPlaceMixin, since Fabric has no built-in
		// placement-side equivalent of PlayerBlockBreakEvents.AFTER.

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				var pos = hitResult.getBlockPos();
				EventFirer.fireGlobalEvent(Events.BLOCK_INTERACT,
						CoerceJavaToLua.coerce(serverPlayer),
						LuaValue.valueOf(pos.getX()), LuaValue.valueOf(pos.getY()), LuaValue.valueOf(pos.getZ()));
			}
			return InteractionResult.PASS;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				EventFirer.fireGlobalEvent(
						Events.ITEM_USE,
						CoerceJavaToLua.coerce(serverPlayer),
						CoerceJavaToLua.coerce(player.getItemInHand(hand))
				);
			}
			return InteractionResult.PASS;
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				EventFirer.fireGlobalEvent(Events.PLAYER_ATTACK_ENTITY,
						CoerceJavaToLua.coerce(serverPlayer),
						CoerceJavaToLua.coerce(entity));
			}
			return InteractionResult.PASS;
		});

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				EventFirer.fireGlobalEvent(Events.PLAYER_INTERACT_ENTITY,
						CoerceJavaToLua.coerce(serverPlayer),
						CoerceJavaToLua.coerce(entity));
			}
			return InteractionResult.PASS;
		});

		// Note: fires on chunk load as well as fresh spawns - not spawn-exclusive.
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
				EventFirer.fireGlobalEvent(Events.ENTITY_LOAD, CoerceJavaToLua.coerce(entity)));

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