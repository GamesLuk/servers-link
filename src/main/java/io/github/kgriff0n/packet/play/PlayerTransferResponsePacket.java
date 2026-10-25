package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.event.listener.ServerTick;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import io.github.kgriff0n.packet.server.PlayerDataPacket;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.github.kgriff0n.socket.SubServer;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;
import java.io.IOException;

public class PlayerTransferResponsePacket extends PacketHeader implements Packet {

    private static final byte PREVENT_LEAVE_MESSAGE = 1 << 0;
    /* Gives the PlayerDataPacket a head start before the client reconnects to the target */
    private static final long TRANSFER_DELAY_MS = 500;
    private byte flags;

    private final UUID uuid;

    protected PlayerTransferResponsePacket(UUID uuid, String sender, String recipient) {
        super(sender, recipient);
        this.uuid = uuid;
    }

    @Override
    public void onReceive() {
        ServerInfo server = ServersLinkApi.getServer(sender);
        ServerPlayer player = ServersLink.SERVER.getPlayerList().getPlayer(uuid);
        ServersLink.LOGGER.info("[DEBUG] TransferResponse - uuid={}, target={}, playerFound={}", uuid, sender, player != null);
        if ((flags & PREVENT_LEAVE_MESSAGE) != 0) {
            ServersLinkApi.getPreventDisconnect().add(uuid);
        }
        if (server != null && player != null) {
            /* 1. Write the current player state to disk and send it to the target server */
            sendPlayerData(uuid);

            /* 2. Transfer the client slightly delayed so the data arrives first */
            final String targetIp = server.getIp();
            final int targetPort = server.getPort();
            CompletableFuture.runAsync(() -> ServersLink.SERVER.execute(() -> {
                ServerPlayer current = ServersLink.SERVER.getPlayerList().getPlayer(uuid);
                if (current == null) {
                    ServersLink.LOGGER.warn("[DEBUG] TransferResponse - player {} left before transfer", uuid);
                    return;
                }
                ServersLink.LOGGER.info("[DEBUG] TransferResponse - transferring {} to {}:{}", uuid, targetIp, targetPort);
                current.connection.send(new ClientboundTransferPacket(targetIp, targetPort));
                ServerTick.scheduleDisconnect(current.getUUID(), 20);
            }), CompletableFuture.delayedExecutor(TRANSFER_DELAY_MS, TimeUnit.MILLISECONDS));
        } else {
            ServersLink.LOGGER.warn("[DEBUG] TransferResponse - transfer skipped (serverInfo={}, player={})", server, player);
        }
    }

    private static void sendPlayerData(UUID uuid) {
        if (FakePlayerApi.isFake(ServersLink.SERVER, ServersLink.SERVER.getPlayerList().getPlayer(uuid))) return;
        try {
            /* Make sure the files on disk contain the latest state */
            ServersLink.SERVER.getPlayerList().saveAll();

            PlayerDataPacket dataPacket = new PlayerDataPacket(uuid);
            if (ServersLink.isGateway) {
                Gateway.getInstance().sendToAllFrom(ServersLink.getServerInfo().getName(), dataPacket);
            } else {
                SubServer.getInstance().send(dataPacket);
            }
            ServersLink.LOGGER.info("[DEBUG] TransferResponse - PlayerDataPacket sent for {}", uuid);
        } catch (IOException e) {
            ServersLink.LOGGER.error("[DEBUG] TransferResponse - could not create PlayerDataPacket for {}", uuid, e);
        }
    }

    @Override
    public void gatewayLogic() {
        PlayersInformation.setLastServer(uuid, sender);
        Gateway gateway = Gateway.getInstance();
        Settings settings = gateway.getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(recipient).getGroupId());
        if (settings.isPlayerListSynced()) {
            flags |= PREVENT_LEAVE_MESSAGE;
        }
    }

}
