package io.github.kgriff0n.mixin;

import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.packet.play.SystemChatPacket;
import io.github.kgriff0n.packet.server.PlayerDataPacket;
import io.github.kgriff0n.util.DummyPlayer;
import io.github.kgriff0n.api.ServersLinkApi;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import static io.github.kgriff0n.ServersLink.SERVER;
import io.github.kgriff0n.util.IPlayerServersLink;

@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {

    @Shadow public abstract void broadcastSystemMessage(Component message, boolean overlay);

    @Shadow public abstract void broadcastAll(Packet<?> packet);

    @Shadow @Final private List<ServerPlayer> players;

    @Unique
    private ServerPlayer player;

    @Unique
    private static boolean servers_link$isNonRealPlayer(ServerPlayer player) {
        return player instanceof DummyPlayer
                || player.connection == null
                || FakePlayerApi.isFake(SERVER, player);
    }

    @Inject(at = @At("HEAD"), method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V")
    private void sendSystemPacket(Component message, boolean overlay, CallbackInfo ci) {
        SystemChatPacket packet = new SystemChatPacket(ComponentSerialization.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, SERVER.registryAccess()), message).getOrThrow().toString());
        ServersLinkApi.send(packet, ServersLink.getServerInfo().getName());
    }

    @Inject(at = @At("HEAD"), method = "placeNewPlayer")
    private void getPlayer(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {
        this.player = player;
    }

    @Inject(at = @At("TAIL"), method = "placeNewPlayer")
    private void applyStoredJoinState(Connection connection, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo ci) {

        if (servers_link$isNonRealPlayer(player)) {
            return;
        }

        IPlayerServersLink data = (IPlayerServersLink) player;
        String serverName = ServersLink.getServerInfo().getName();

        Vec3 storedPos = data.servers_link$getServerPos(serverName);
        ServerLevel storedDim = data.servers_link$getServerDim(serverName);
        List<Float> storedRot = data.servers_link$getServerRot(serverName);
        GameType storedGameMode = data.servers_link$getServerGameMode(serverName);

        ServerLevel defaultWorld = player.level().getServer().overworld();
        ServerLevel targetDim = storedDim != null
                ? storedDim
                : (defaultWorld != null ? defaultWorld : (ServerLevel) player.level());
        Vec3 targetPos = storedPos != null
                ? storedPos
                : new Vec3(
                        targetDim.getRespawnData().pos().getX() + 0.5,
                        targetDim.getRespawnData().pos().getY(),
                        targetDim.getRespawnData().pos().getZ() + 0.5
                );
        float targetYaw = storedRot != null && storedRot.size() >= 2 ? storedRot.get(0) : player.getYRot();
        float targetPitch = storedRot != null && storedRot.size() >= 2 ? storedRot.get(1) : player.getXRot();

        player.teleportTo(targetDim, targetPos.x(), targetPos.y(), targetPos.z(), EnumSet.noneOf(Relative.class), targetYaw, targetPitch, false);

        GameType targetGameMode = storedGameMode != null ? storedGameMode : player.level().getServer().getDefaultGameType();
        if (targetGameMode != null) {
            player.setGameMode(targetGameMode);
        }
    }

    @Inject(at = @At("TAIL"), method = "save")
    private void sendPlayerData(ServerPlayer player, CallbackInfo ci) {
        if (servers_link$isNonRealPlayer(player)) {
            return;
        }
        try {
            ServersLinkApi.send(new PlayerDataPacket(player.getUUID()), ServersLink.getServerInfo().getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void preventConnectMessage(PlayerList instance, Component message, boolean overlay) {
        if (ServersLinkApi.getPreventConnect().contains(player.getUUID())) {
            ServersLinkApi.getPreventConnect().remove(player.getUUID());
        } else {
            this.broadcastSystemMessage(message, overlay);
        }
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void sendPlayerList(PlayerList instance, Packet<?> packet) {
        List<ServerPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(players);
        allPlayers.addAll(ServersLinkApi.getDummyPlayers());
        this.broadcastAll(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(allPlayers));
    }

    @Inject(at = @At("HEAD"), method = "save", cancellable = true)
    private void savePlayerDataThreadSafe(ServerPlayer player, CallbackInfo ci) {
        if (servers_link$isNonRealPlayer(player)) {
            ci.cancel();
            return;
        }

        String serverName = ServersLink.getServerInfo().getName();
        IPlayerServersLink data = (IPlayerServersLink) player;
        data.servers_link$setServerPos(serverName, player.position());
        data.servers_link$setServerDim(serverName, player.level());
        data.servers_link$setServerRot(serverName, player.getYRot(), player.getXRot());
        data.servers_link$setServerGameMode(serverName, player.gameMode.getGameModeForPlayer());
    }

    @Inject(at = @At("HEAD"), method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V")
    private void broadcastDummy(PlayerChatMessage message, Predicate<ServerPlayer> shouldSendFiltered, @Nullable ServerPlayer sender, ChatType.Bound params, CallbackInfo ci) {
        OutgoingChatMessage sentMessage = OutgoingChatMessage.create(message);
        for (ServerPlayer serverPlayerEntity : ServersLinkApi.getDummyPlayers()) {
            boolean bl3 = shouldSendFiltered.test(serverPlayerEntity);
            serverPlayerEntity.sendChatMessage(sentMessage, bl3, params);
        }
    }
}
