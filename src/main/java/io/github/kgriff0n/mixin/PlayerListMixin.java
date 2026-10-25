package io.github.kgriff0n.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.play.SystemChatPacket;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            ),
            require = 1
    )
    private void serversLink$filterJoinBroadcast(
            PlayerList playerList,
            Component message,
            boolean overlay,
            Connection connection,
            ServerPlayer joiningPlayer,
            CommonListenerCookie cookie
    ) {
        UUID uuid = joiningPlayer.getUUID();

        boolean pending =
                ServersLinkApi.getPreventConnect().contains(uuid);

        boolean suppress =
                ServersLinkApi.getPreventConnect().remove(uuid);

        ServersLink.LOGGER.info(
                "[JoinLeaveDebug][{}] JOIN callsite player={} uuid={} "
                        + "pendingBefore={} result={}",
                ServersLinkApi.getServerName(),
                joiningPlayer.getName().getString(),
                uuid,
                pending,
                suppress ? "SUPPRESS" : "ALLOW"
        );

        if (!suppress) {
            playerList.broadcastSystemMessage(message, overlay);
        }
    }

    // Cancel saving if player is fake
    @Inject(
            method = "save",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true
    )
    private void conditionalSave(ServerPlayer player, CallbackInfo ci) {
        if (FakePlayerApi.isFake(ServersLink.SERVER, player)) {
            ci.cancel();
        }
    }

    // Send system messages to other servers
    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void broadcastSystemMessage(
            Component message,
            Function<ServerPlayer, Component> playerMessages,
            boolean overlay,
            CallbackInfo ci
    ) {
        String serialized = ComponentSerialization.CODEC
                .encodeStart(
                        RegistryOps.create(
                                JsonOps.INSTANCE,
                                ServersLink.SERVER.registryAccess()
                        ),
                        message
                )
                .getOrThrow()
                .toString();

        ServersLinkApi.send(new SystemChatPacket(serialized));
    }
}
