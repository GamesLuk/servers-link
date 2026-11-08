package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class PlayerTransferRequestPacket extends PacketHeader implements Packet {

    private static final byte OVERRIDE_POSITION = 1 << 0;
    private static final byte PREVENT_JOIN_MESSAGE = 1 << 1;
    private static final byte TO_SPAWN = 1 << 2;
    private byte flags;

    private final UUID uuid;
    private double targetX;
    private double targetY;
    private double targetZ;
    private float yaw;
    private float pitch;
    private String world;

    public PlayerTransferRequestPacket(UUID uuid, String sender, String recipient) {
        super(sender, recipient);
        this.uuid = uuid;
    }

    public PlayerTransferRequestPacket(UUID uuid, String sender, String recipient, double targetX, double targetY, double targetZ, float yaw, float pitch, String world) {
        this(uuid, sender, recipient);
        this.flags |= OVERRIDE_POSITION;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    public PlayerTransferRequestPacket(UUID uuid, String sender, String recipient, boolean toWorldSpawn) {
        this(uuid, sender, recipient);
        if (toWorldSpawn) this.flags |= TO_SPAWN;
    }

    @Override
    public void onReceive() {
        ServersLinkApi.addWaitingPlayer(uuid);

        boolean preventJoin = (flags & PREVENT_JOIN_MESSAGE) != 0;
        boolean override = (flags & OVERRIDE_POSITION) != 0;
        boolean toSpawn = (flags & TO_SPAWN) != 0;

        if (preventJoin) {
            boolean added = ServersLinkApi.getPreventConnect().add(uuid);

            ServersLink.LOGGER.info(
                    "[JoinLeaveDebug][{}] Arm JOIN suppression "
                            + "uuid={} flag=true added={} pending={}",
                    ServersLinkApi.getServerName(),
                    uuid,
                    added,
                    ServersLinkApi.getPreventConnect().contains(uuid)
            );
        } else {
            ServersLink.LOGGER.info(
                    "[JoinLeaveDebug][{}] JOIN suppression flag=false uuid={}",
                    ServersLinkApi.getServerName(),
                    uuid
            );
        }

        ServersLinkApi.send(
                new PlayerTransferResponsePacket(
                        uuid,
                        ServersLinkApi.getServerName(),
                        sender
                )
        );

        if (toSpawn) {
            MinecraftServer minecraftServer = ServersLinkApi.getServer();
            String dimensionId = minecraftServer.getRespawnData().dimension().identifier().toString();
            BlockPos spawnPos = minecraftServer.getRespawnData().pos();
            float spawnAngle = minecraftServer.getRespawnData().yaw();
            ServersLinkApi.addPositionOverride(
                    uuid,
                    new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5),
                    new Vec2(spawnAngle, 0f),
                    dimensionId
            );
        }

        if (override) {
            ServersLinkApi.addPositionOverride(
                    uuid,
                    new Vec3(targetX + 0.5, targetY, targetZ + 0.5),
                    new Vec2(yaw, pitch),
                    world
            );
        }
    }

    @Override
    public void gatewayLogic() {
        Gateway gateway = Gateway.getInstance();
        Settings settings = gateway.getSettings(ServersLinkApi.getServer(sender).getGroupId(), ServersLinkApi.getServer(recipient).getGroupId());
        if (settings.isPlayerListSynced()) {
            flags |= PREVENT_JOIN_MESSAGE;
        }
    }
}
