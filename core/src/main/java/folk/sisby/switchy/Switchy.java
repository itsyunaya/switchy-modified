package folk.sisby.switchy;

import folk.sisby.switchy.api.SwitchyEvents;
import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;

/**
 * Initializes core addons by invoking {@link SwitchyEvents.Init}.
 * Responsible for logging initial modules.
 * Works around limitations on init-time entrypoints by using two post-init entrypoints.
 *
 * @author Sisby folk
 * @see SwitchyPlayConnectionListener
 * @since 1.0.0
 */
public class Switchy implements DedicatedServerModInitializer, ClientModInitializer {
	/**
	 * The switchy namespace.
	 */
	public static final String ID = "switchy";

	/**
	 * The switchy logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	private static Path logFile;
	private static final int MAX_ENTRIES = 1000;
	private static final ArrayDeque<String> buffer = new ArrayDeque<>(MAX_ENTRIES);

	/**
	 * The config object for switchy, containing the current state of {@code /config/switchy/config.toml}.
	 */
	public static final SwitchyConfig CONFIG = SwitchyConfig.createToml(FabricLoader.getInstance().getConfigDir(), ID, "config", SwitchyConfig.class);

	private void setupActionLogger() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		logFile = configDir.resolve("switchyActionLog.log");

		try {
			if (!Files.exists(configDir)) {
				Files.createDirectories(configDir);
			}
			if (!Files.exists(logFile)) {
				Files.createFile(logFile);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to create log file", e);
		}
	}

	public static void logAction(ServerPlayerEntity player, String action) {
		String line = "[" + System.currentTimeMillis() + "] " + "Player " + player.getName() + " ran " + action + ". Enabled modules: " + SwitchyModuleRegistry.getModules();

		if (buffer.size() >= MAX_ENTRIES) {
			buffer.removeFirst();
		}
		buffer.addLast(line);

		try {
			Files.write(logFile, buffer, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("Failed to write action log", e);
		}
	}

	public void onInitialize() {
		setupActionLogger();
		CommandRegistrationCallback.EVENT.register(SwitchyCommands::registerCommands);
		ServerPlayConnectionEvents.JOIN.register(SwitchyPlayConnectionListener::onPlayReady);
		ServerPlayConnectionEvents.DISCONNECT.register(SwitchyPlayConnectionListener::onPlayDisconnect);
		SwitchyEvents.registerEntrypointListeners();
		SwitchyEvents.INIT.invoker().onInitialize();
		Switchy.LOGGER.info("[Switchy] Initialized! Registered Modules: {}", SwitchyModuleRegistry.getModules());
	}

	@Override
	public void onInitializeClient() {
		onInitialize();
	}

	@Override
	public void onInitializeServer() {
		onInitialize();
	}
}
