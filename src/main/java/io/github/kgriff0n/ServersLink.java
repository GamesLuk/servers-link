package io.github.kgriff0n;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kgriff0n.api.PlaceholderIntegration;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.command.ServerCommand;
import io.github.kgriff0n.event.listener.*;
import io.github.kgriff0n.server.ServerInfo;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServersLink implements ModInitializer {
	public static final String MOD_ID = "servers-link";
	public static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("servers-link");
	public static boolean isGateway;

	private static ServerInfo serverInfo;
	private static String gatewayIp;
	private static int gatewayPort;
    private static String gatewayGameIp;
    private static int gatewayGamePort;
    private static boolean gatewayAvailable = true;

    private static String commandName;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static MinecraftServer SERVER;
	public static boolean IS_RUNNING = true;
	public static boolean CONFIG_ERROR = false;

	@Override
	public void onInitialize() {
        LOGGER.info("=== ServersLink Initialization Started ===");
        LOGGER.info("DEBUG: Registering PlayerData keys for synchronization...");

        ServersLinkApi.addPlayerDataKey("Dimension");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: Dimension");
        ServersLinkApi.addPlayerDataKey("ender_pearls");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: ender_pearls");
        ServersLinkApi.addPlayerDataKey("entered_nether_pos");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: entered_nether_pos");
        ServersLinkApi.addPlayerDataKey("LastDeathLocation");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: LastDeathLocation");
        ServersLinkApi.addPlayerDataKey("Pos");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: Pos");
        ServersLinkApi.addPlayerDataKey("respawn");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: respawn");
        ServersLinkApi.addPlayerDataKey("RootVehicle");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: RootVehicle");
        ServersLinkApi.addPlayerDataKey("Rotation");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: Rotation");
        ServersLinkApi.addPlayerDataKey("ShoulderEntityLeft");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: ShoulderEntityLeft");
        ServersLinkApi.addPlayerDataKey("ShoulderEntityRight");
        LOGGER.debug("DEBUG: Registered PlayerDataKey: ShoulderEntityRight");

        LOGGER.info("DEBUG: All PlayerData keys registered successfully");

		LOGGER.info("DEBUG: Loading server configuration from info.json...");
		loadServerInfo();
		LOGGER.info("DEBUG: Server configuration loaded. Gateway mode: {}", isGateway);

		LOGGER.info("DEBUG: Registering server command...");
		ServerCommand.register();
		LOGGER.info("DEBUG: Server command registered with name: {}", getCommandName());

        LOGGER.info("DEBUG: Registering event listeners...");
        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register(new PlayerJoin());
        LOGGER.debug("DEBUG: Registered BEFORE_CONFIGURE event listener");
		ServerLifecycleEvents.SERVER_STARTED.register(new ServerStart());
		LOGGER.debug("DEBUG: Registered SERVER_STARTED event listener");
		ServerLifecycleEvents.SERVER_STOPPING.register(new ServerStopping());
		LOGGER.debug("DEBUG: Registered SERVER_STOPPING event listener");
		ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStopped());
		LOGGER.debug("DEBUG: Registered SERVER_STOPPED event listener");
		ServerPlayConnectionEvents.JOIN.register(new PlayerJoin());
		LOGGER.debug("DEBUG: Registered JOIN event listener");
		ServerPlayConnectionEvents.DISCONNECT.register(new PlayerDisconnect());
		LOGGER.debug("DEBUG: Registered DISCONNECT event listener");
		ServerTickEvents.START_SERVER_TICK.register(new ServerTick());
		LOGGER.debug("DEBUG: Registered SERVER_TICK event listener");

		if (FabricLoader.getInstance().isModLoaded("placeholder-api")) {
		    LOGGER.info("DEBUG: PlaceholderAPI detected, registering integration...");
		    PlaceholderIntegration.register();
		} else {
		    LOGGER.info("DEBUG: PlaceholderAPI not loaded, skipping integration");
		}

		LOGGER.info("=== ServersLink Initialization Complete ===");
    }

	public static ServerInfo getServerInfo() {
		return serverInfo;
	}

	public static String getGatewayIp() {
		return gatewayIp;
	}
	public static int getGatewayPort() {
		return gatewayPort;
	}

    public static String getGatewayGameIp() {
        return gatewayGameIp;
    }
    public static int getGatewayGamePort() {
        return gatewayGamePort;
    }

    public static boolean isGatewayAvailable() {
        return gatewayAvailable;
    }
    public static void setGatewayAvailable(boolean isGatewayAvailable) {
        if (ServersLink.gatewayAvailable != isGatewayAvailable) {
            LOGGER.warn("DEBUG: Gateway availability changed from {} to {}", 
                ServersLink.gatewayAvailable, isGatewayAvailable);
        }
        if (ServersLink.gatewayAvailable != isGatewayAvailable) {
            LOGGER.warn("DEBUG: Gateway availability changed from {} to {}", 
                ServersLink.gatewayAvailable, isGatewayAvailable);
        }
        ServersLink.gatewayAvailable = isGatewayAvailable;
    }

    public static String getCommandName() {
        if (commandName == null) return "server";
        return commandName;
    }

    private void loadServerInfo() {
		Path path = CONFIG.resolve("info.json");
		LOGGER.info("DEBUG: Loading configuration from: {}", path.toAbsolutePath());

		try {
			if (!Files.exists(path)) {
			    LOGGER.error("DEBUG: Configuration file does not exist at: {}", path.toAbsolutePath());
			    CONFIG_ERROR = true;
			    return;
			}

			String jsonContent = Files.readString(path);
			LOGGER.debug("DEBUG: Configuration file content length: {} characters", jsonContent.length());
			LOGGER.debug("DEBUG: Raw JSON content: {}", jsonContent);

			Gson gson = new Gson();
			JsonObject jsonObject = gson.fromJson(jsonContent, JsonObject.class);
			LOGGER.debug("DEBUG: JSON parsed successfully");

            isGateway = jsonObject.get("gateway").getAsBoolean();
            LOGGER.info("DEBUG: Loaded gateway mode: {}", isGateway);

			gatewayIp = jsonObject.get("gateway-ip").getAsString();
			LOGGER.info("DEBUG: Loaded gateway-ip: {}", gatewayIp);

			gatewayPort = jsonObject.get("gateway-port").getAsInt();
			LOGGER.info("DEBUG: Loaded gateway-port: {}", gatewayPort);

			String group = jsonObject.get("group").getAsString();
			String serverName = jsonObject.get("server-name").getAsString();
			String serverIp = jsonObject.get("server-ip").getAsString();
			int serverPort = jsonObject.get("server-port").getAsInt();

			LOGGER.info("DEBUG: Loaded group: {}", group);
			LOGGER.info("DEBUG: Loaded server-name: {}", serverName);
			LOGGER.info("DEBUG: Loaded server-ip: {}", serverIp);
			LOGGER.info("DEBUG: Loaded server-port: {}", serverPort);

			serverInfo = new ServerInfo(group, serverName, serverIp, serverPort);
			LOGGER.info("DEBUG: ServerInfo object created successfully");

            // Optional Values
            JsonElement gatewayGameIpElement = jsonObject.get("gateway-game-ip");
            gatewayGameIp = gatewayGameIpElement != null ? gatewayGameIpElement.getAsString() : null;
            LOGGER.info("DEBUG: Loaded gateway-game-ip (optional): {}", gatewayGameIp != null ? gatewayGameIp : "NOT SET");

            JsonElement gatewayGamePortElement = jsonObject.get("gateway-game-port");
            gatewayGamePort = gatewayGamePortElement != null ? gatewayGamePortElement.getAsInt() : 25565;
            LOGGER.info("DEBUG: Loaded gateway-game-port (optional): {}", gatewayGamePort);

            JsonElement commandNameElement = jsonObject.get("command-name");
            commandName = commandNameElement != null ? commandNameElement.getAsString() : "server";
            LOGGER.info("DEBUG: Loaded command-name (optional): {}", commandName);

            LOGGER.info("DEBUG: Configuration loaded successfully!");
            LOGGER.info("DEBUG: Final config summary - Gateway: {}, Server: {}/{}, Gateway endpoint: {}:{}", 
                isGateway, group, serverName, gatewayIp, gatewayPort);

		} catch (IOException e) {
			CONFIG_ERROR = true;
			LOGGER.error("DEBUG: FAILED to read info.json - IOException occurred", e);
			LOGGER.error("DEBUG: Error message: {}", e.getMessage());
			LOGGER.error("DEBUG: Config path attempted: {}", path.toAbsolutePath());
		} catch (Exception e) {
			CONFIG_ERROR = true;
			LOGGER.error("DEBUG: FAILED to parse info.json - Unexpected error", e);
			LOGGER.error("DEBUG: Error type: {}", e.getClass().getName());
			LOGGER.error("DEBUG: Error message: {}", e.getMessage());
		}
	}
}