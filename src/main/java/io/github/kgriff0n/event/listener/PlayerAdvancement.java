package io.github.kgriff0n.event.listener;

import com.mojang.serialization.JsonOps;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.play.SystemChatPacket;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.RegistryOps;

public final class PlayerAdvancement {

    public static void register() {
        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            if (!(message.getContents() instanceof TranslatableContents contents)) {
                return;
            }

            if (!contents.getKey().startsWith("chat.type.advancement.")) {
                return;
            }

            String serialized = ComponentSerialization.CODEC.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()),
                    (Component) message
            ).getOrThrow().toString();

            ServersLinkApi.send(new SystemChatPacket(serialized));
        });
    }
}
