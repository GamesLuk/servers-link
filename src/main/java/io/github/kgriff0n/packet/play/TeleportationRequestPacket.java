package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class TeleportationRequestPacket implements Packet {

    private final UUID targetUuid;
    private final UUID senderUuid;

    private final String originServer;
    private final String destinationServer;

    public TeleportationRequestPacket(UUID targetUuid, UUID senderUuid, String originServer, String destinationServer) {
        this.targetUuid = targetUuid;
        this.senderUuid = senderUuid;

        this.originServer = originServer;
        this.destinationServer = destinationServer;
    }

    @Override
    public void onReceive() {
        ServerPlayer player = ServersLink.SERVER.getPlayerList().getPlayer(targetUuid);
        Vec3 pos = player != null ? player.position() : null;

        if (ServersLink.isGateway) { //FIXME
            if (this.destinationServer.equals(ServersLink.getServerInfo().getName())) {
                /* Execute packet from hub */
                if (pos != null) {
                    Gateway.getInstance().sendTo(new TeleportationAcceptPacket(pos.x(), pos.y(), pos.z(), this.senderUuid, this.originServer, this.destinationServer), this.originServer);
                }
            } else {
                /* Redirect the packet to the other server */
                Gateway.getInstance().sendTo(this, this.destinationServer);
            }
        } else {
            /* Sub-server receive the packet */
            if (pos != null) {
                SubServer.getInstance().send(new TeleportationAcceptPacket(pos.x(), pos.y(), pos.z(), this.senderUuid, this.originServer, this.destinationServer));
            }
        }
    }
}
