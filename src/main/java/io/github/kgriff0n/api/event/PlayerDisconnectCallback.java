package io.github.kgriff0n.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Callback for when a player disconnects from the server.
 * This event provides information about whether the player is being transferred
 * to another server or is leaving the network entirely.
 */
public interface PlayerDisconnectCallback {

    Event<PlayerDisconnectCallback> EVENT = EventFactory.createArrayBacked(PlayerDisconnectCallback.class,
            (listeners) -> (player, uuid, isTransfer) -> {
                for (PlayerDisconnectCallback listener : listeners) {
                    listener.onPlayerDisconnect(player, uuid, isTransfer);
                }
            });

    /**
     * Called when a player disconnects from the server.
     *
     * @param player the player who is disconnecting (may be null if already disconnected)
     * @param uuid the UUID of the disconnecting player
     * @param isTransfer true if the player is being transferred to another server,
     *                   false if the player is leaving the network entirely
     */
    void onPlayerDisconnect(ServerPlayerEntity player, UUID uuid, boolean isTransfer);
}

