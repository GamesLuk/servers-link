package io.github.kgriff0n.mixin;

import io.github.kgriff0n.ServersLink;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin {

    @Shadow
    @Final
    private Set<ThrownEnderpearl> enderPearls;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    protected abstract void loadAndSpawnEnderPearl(ValueInput view);


    @Inject(at = @At("HEAD"), method = "saveEnderPearls", cancellable = true)
    private void writeEnderPearls(ValueOutput view, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Path path = server
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("enderpearls")
                .resolve(player.getStringUUID() + ".dat");
        TagValueOutput nbtWriteView = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
        ValueOutput.ValueOutputList listView = nbtWriteView.childrenList("ender_pearls");
        for (ThrownEnderpearl enderPearlEntity : this.enderPearls) {
            if (enderPearlEntity.isRemoved()) {
                ServersLink.LOGGER.warn("Trying to save removed ender pearl, skipping");
            } else {
                ValueOutput writeView = listView.addChild();
                enderPearlEntity.save(writeView);
                writeView.store("ender_pearl_dimension", Level.RESOURCE_KEY_CODEC, enderPearlEntity.level().dimension());
            }
        }

        CompoundTag nbtCompound = nbtWriteView.buildResult().copy();
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

    @Inject(at = @At("HEAD"), method = "loadAndSpawnEnderPearls", cancellable = true)
    private void readEnderPearls(ValueInput view, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Path path = server
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("enderpearls")
                .resolve(player.getStringUUID() + ".dat");
        try (InputStream is = Files.newInputStream(path)) {
            CompoundTag nbt = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
            ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), nbt);
            readView.childrenListOrEmpty("ender_pearls").forEach(this::loadAndSpawnEnderPearl);
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to load ender pearls");
        }

        ci.cancel();

    }
}
