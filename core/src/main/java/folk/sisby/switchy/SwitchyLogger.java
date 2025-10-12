package folk.sisby.switchy;

import folk.sisby.switchy.api.module.SwitchyModuleRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;

import static folk.sisby.switchy.Switchy.webhook;

public class SwitchyLogger {
	private static Path logFile;
	private static final int MAX_ENTRIES = 1000;
	private static final ArrayDeque<String> buffer = new ArrayDeque<>(MAX_ENTRIES);

	public static void setupActionLogger() {
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
			Switchy.LOGGER.error("Failed to create log file", e);
		}
	}

	public static void logAction(ServerPlayerEntity player, String action) {
		String line = "[" + System.currentTimeMillis() + "] " + "Player " + player.getName().getString() + " ran " + action + ". Enabled modules: " + SwitchyModuleRegistry.getModules();
		webhook.sendMessage(line);

		if (buffer.size() >= MAX_ENTRIES) {
			buffer.removeFirst();
		}
		buffer.addLast(line);

		try {
			Files.write(logFile, buffer, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			Switchy.LOGGER.error("Failed to write action log", e);
		}
	}
}
