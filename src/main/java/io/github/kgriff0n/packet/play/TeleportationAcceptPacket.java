package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.util.IPlayerServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class TeleportationAcceptPacket implements Packet {

    private final double targetX;
    private final double targetY;
    private final double targetZ;
    private final UUID senderUuid;

    private final String originServer;
    private final String destinationServer;

    public TeleportationAcceptPacket(double targetX, double targetY, double targetZ, UUID senderUuid, String originServer, String destinationServer) {
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.senderUuid = senderUuid;

        this.originServer = originServer;
        this.destinationServer = destinationServer;
    }

    @Override
    public void onReceive() {
        ServerPlayer player = ServersLink.SERVER.getPlayerList().getPlayer(senderUuid);
        if (this.originServer.equals(ServersLink.getServerInfo().getName()) && player != null) {
            /* We are in the correct server */
            ((IPlayerServersLink) player).servers_link$setServerPos(this.destinationServer, new Vec3(targetX, targetY, targetZ));
            ServersLinkApi.transferPlayer(player, this.originServer, this.destinationServer);
        }
    }

    @Override
    public void onGatewayReceive(String sender) {
        Packet.super.onGatewayReceive(sender);
        if (!this.originServer.equals(ServersLink.getServerInfo().getName())) {
            /* Redirect the packet to the other server */
            Gateway.getInstance().sendTo(this, this.destinationServer);
        }
    }
}
