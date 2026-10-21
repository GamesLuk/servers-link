package io.github.kgriff0n.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.UUID;
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
            at = @At("HEAD"),
            cancellable = true
    )
    private void broadcastSystemMessage(Component message, Function<ServerPlayer, Component> playerMessages, boolean overlay, CallbackInfo ci) {
        String serialized = ComponentSerialization.CODEC
                .encodeStart(RegistryOps.create(JsonOps.INSTANCE, ServersLink.SERVER.registryAccess()), message)
                .getOrThrow()
                .toString();

        if (isMessagePrevented(serialized)) {
            ci.cancel();
            return;
        }

        SystemChatPacket packet = new SystemChatPacket(serialized);
        ServersLinkApi.send(packet);
    }

    @Unique
    private static boolean isMessagePrevented(String serializedMessage) {
        try {
            JsonObject json = JsonParser.parseString(serializedMessage).getAsJsonObject();

            if (json.has("translate")) {
                String translateKey = json.get("translate").getAsString();

                boolean isJoin = translateKey.contains("multiplayer.player.joined");
                boolean isLeave = translateKey.contains("multiplayer.player.left");

                if ((isJoin || isLeave) && json.has("with")) {
                    JsonArray withArray = json.getAsJsonArray("with");
                    if (!withArray.isEmpty()) {
                        JsonObject playerObj = withArray.get(0).getAsJsonObject();

                        UUID uuid = null;

                        if (playerObj.has("hover_event")) {
                            JsonObject hoverEvent = playerObj.getAsJsonObject("hover_event");
                            if (hoverEvent.has("uuid")) {
                                JsonArray uuidArray = hoverEvent.getAsJsonArray("uuid");
                                if (uuidArray.size() == 4) {
                                    long mostSig = ((long) uuidArray.get(0).getAsInt() << 32) | (uuidArray.get(1).getAsInt() & 0xFFFFFFFFL);
                                    long leastSig = ((long) uuidArray.get(2).getAsInt() << 32) | (uuidArray.get(3).getAsInt() & 0xFFFFFFFFL);
                                    uuid = new UUID(mostSig, leastSig);
                                }
                            }
                        }

                        if (uuid != null) {
                            boolean isPrevented = (isJoin && ServersLinkApi.getPreventConnect().contains(uuid))
                                    || (isLeave && ServersLinkApi.getPreventDisconnect().contains(uuid));

                            // Remove the player from the Message Prevention lists after the join/leave message
                            // is send to not influence the lists before the message
                            ServersLinkApi.getPreventConnect().remove(uuid);
                            ServersLinkApi.getPreventDisconnect().remove(uuid);
                            return isPrevented;
                        }
                    }
                }
            }
        } catch (Exception e) {
            ServersLink.LOGGER.error("Failed to parse system chat json", e);
        }
        return false;
    }
}
