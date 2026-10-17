package io.github.kgriff0n.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.impl.event.interaction.FakePlayerPacketListener;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;

import static io.github.kgriff0n.ServersLink.SERVER;

public class DummyPlayer extends ServerPlayer {
    public DummyPlayer(GameProfile profile) {
        super(SERVER, SERVER.overworld(), profile, ClientInformation.createDefault());
        this.entityData.set(
                DATA_PLAYER_MODE_CUSTOMISATION,
                (byte) PlayerModelPart.HAT.getMask()
        );
        this.connection = new FakePlayerPacketListener(this);
    }
}
