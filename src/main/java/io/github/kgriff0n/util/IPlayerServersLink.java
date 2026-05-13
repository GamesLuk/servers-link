package io.github.kgriff0n.util;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public interface IPlayerServersLink {

    void servers_link$setServerPos(String name, Vec3 pos);
    Vec3 servers_link$getServerPos(String name);
    void servers_link$removeServerPos(String name);

    void servers_link$setServerRot(String name, float yaw, float pitch);
    List<Float> servers_link$getServerRot(String name);
    void servers_link$removeServerRot(String name);

    void servers_link$setServerDim(String name, ServerLevel dim);
    ServerLevel servers_link$getServerDim(String name);
    void servers_link$removeServerDim(String name);

    void servers_link$setServerGameMode(String name, GameType gameMode);
    GameType servers_link$getServerGameMode(String name);
    void servers_link$removeServerGameMode(String name);

}
