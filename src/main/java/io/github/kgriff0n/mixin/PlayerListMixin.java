package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    // Cancel saving if player is fake
    @Inject(
            method = "save",
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true
    )
    private void conditionalSave(ServerPlayer player, CallbackInfo ci) {
        ServersLink.LOGGER.info("Checking if " + player.getDisplayName().getString() + " is fake:");
        if (FakePlayerApi.isFake(ServersLink.SERVER, player)) {
            ServersLink.LOGGER.info(player.getDisplayName().getString() + " is fake!");
            ci.cancel();
        } else ServersLink.LOGGER.info(player.getName() + " is real!");
    }
}
