package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow
    @Final
    private Set<EnderPearlEntity> enderPearls;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    protected abstract void readEnderPearl(ReadView view);


    @Inject(at = @At("HEAD"), method = "writeEnderPearls", cancellable = true)
    private void writeEnderPearls(WriteView view, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Path path = server
                .getSavePath(WorldSavePath.ROOT)
                .resolve("data")
                .resolve("enderpearls")
                .resolve(player.getUuidAsString() + ".dat");
        NbtWriteView nbtWriteView = NbtWriteView.create(ErrorReporter.EMPTY, player.getRegistryManager());
        WriteView.ListView listView = nbtWriteView.getList("ender_pearls");
        for (EnderPearlEntity enderPearlEntity : this.enderPearls) {
            if (enderPearlEntity.isRemoved()) {
                ServersLink.LOGGER.warn("Trying to save removed ender pearl, skipping");
            } else {
                WriteView writeView = listView.add();
                enderPearlEntity.saveData(writeView);
                writeView.put("ender_pearl_dimension", World.CODEC, enderPearlEntity.getEntityWorld().getRegistryKey());
            }
        }

        NbtCompound nbtCompound = nbtWriteView.getNbt().copy();
        CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(path.getParent());
                NbtIo.writeCompressed(nbtCompound, path);
            } catch (IOException e) {
                ServersLink.LOGGER.warn("Unable to save ender pearls", e);
            }
        });

        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "readEnderPearls", cancellable = true)
    private void readEnderPearls(ReadView view, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Path path = server
                .getSavePath(WorldSavePath.ROOT)
                .resolve("data")
                .resolve("enderpearls")
                .resolve(player.getUuidAsString() + ".dat");
        try (InputStream is = Files.newInputStream(path)) {
            NbtCompound nbt = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());
            ReadView readView = NbtReadView.create(ErrorReporter.EMPTY, player.getRegistryManager(), nbt);
            readView.getListReadView("ender_pearls").forEach(this::readEnderPearl);
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to load ender pearls");
        }

        ci.cancel();

    }
}
