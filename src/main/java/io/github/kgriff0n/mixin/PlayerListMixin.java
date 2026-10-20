package io.github.kgriff0n.mixin;

import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.play.SystemChatPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.apache.logging.log4j.core.jmx.Server;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

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
            at = @At("HEAD")
    )
    private void broadcastSystemMessage(Component message, Function<ServerPlayer, Component> playerMessages, boolean overlay, CallbackInfo ci) {
        String serialized = ComponentSerialization.CODEC
                .encodeStart(RegistryOps.create(JsonOps.INSTANCE, ServersLink.SERVER.registryAccess()), message)
                .getOrThrow()
                .toString();

        SystemChatPacket packet = new SystemChatPacket(serialized);
        ServersLinkApi.send(packet);
    }
}
