package io.github.kgriff0n.mixin;

import com.mojang.serialization.Codec;
import io.github.kgriff0n.util.IPlayerServersLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

@Mixin(Player.class)
public class PlayerEntityMixin implements IPlayerServersLink {

    @Unique
    private HashMap<String, Vec3> serversPos = new HashMap<>();

    @Unique
    private HashMap<String, List<Float>> serversRot = new HashMap<>();

    @Unique
    private HashMap<String, ServerLevel> serversDim = new HashMap<>();

    @Unique
    private HashMap<String, Integer> serversGameMode = new HashMap<>();

    @Inject(at = @At("HEAD"), method = "addAdditionalSaveData")
    private void writeNbt(ValueOutput view, CallbackInfo ci) {
        ValueOutput serversLink = view.child("ServersLink");
        ValueOutput posView = serversLink.child("Position");
        ValueOutput rotView = serversLink.child("Rotation");
        ValueOutput dimView = serversLink.child("Dimension");
        ValueOutput gameModeView = serversLink.child("GameMode");

        for (Map.Entry<String, Vec3> entry : serversPos.entrySet()) {
            String name = entry.getKey();
            Vec3 pos = entry.getValue();

            ValueOutput.TypedOutputList<Double> posAppender = posView.list(name, Codec.DOUBLE);
            posAppender.add(pos.x());
            posAppender.add(pos.y());
            posAppender.add(pos.z());
        }

        for (Map.Entry<String, List<Float>> entry : serversRot.entrySet()) {
            String name = entry.getKey();
            List<Float> rot = entry.getValue();

            ValueOutput.TypedOutputList<Float> rotAppender = rotView.list(name, Codec.FLOAT);
            rotAppender.add(rot.get(0));
            rotAppender.add(rot.get(1));
        }

        for (Map.Entry<String, ServerLevel> entry : serversDim.entrySet()) {
            String name = entry.getKey();
            ServerLevel dim = entry.getValue();

            dimView.putString(name, dim.dimension().identifier().toString());
        }

        for (Map.Entry<String, Integer> entry : serversGameMode.entrySet()) {
            gameModeView.putInt(entry.getKey(), entry.getValue());
        }
    }

    @Inject(at = @At("HEAD"), method = "readAdditionalSaveData")
    private void readNbt(ValueInput view, CallbackInfo ci) {
        Codec<Map<String, List<Double>>> posMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.list(Codec.DOUBLE));
        Codec<Map<String, String>> dimMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.STRING);
        Codec<Map<String, List<Float>>> rotMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.list(Codec.FLOAT));
        Codec<Map<String, Integer>> gameModeMapCodec =
                Codec.unboundedMap(Codec.STRING, Codec.INT);


        view.child("ServersLink")
            .flatMap(v -> v.read("Position", posMapCodec))
            .ifPresent(posMap -> {
                this.serversPos = new HashMap<>();
                posMap.forEach((server, coords) -> {
                    if (coords.size() >= 3) {
                        serversPos.put(server, new Vec3(coords.get(0), coords.get(1), coords.get(2)));
                    }
                });
            });

        view.child("ServersLink")
                .flatMap(v -> v.read("Rotation", rotMapCodec))
                .ifPresent(rotMap -> {
                    this.serversRot = new HashMap<>();
                    rotMap.forEach((server, rotations) -> {
                        if (rotations.size() >= 2) {
                            serversRot.put(server, List.of(rotations.get(0), rotations.get(1)));
                        }
                    });
                });

        view.child("ServersLink")
            .flatMap(v -> v.read("Dimension", dimMapCodec))
                .ifPresent(dimMap -> {
                    this.serversDim = new HashMap<>();
                    dimMap.forEach((server, dimId) -> {
                        Identifier identifier = Identifier.tryParse(dimId);
                        if (identifier == null) {
                            return;
                        }
                        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, identifier);
                        Level world = Objects.requireNonNull(((Player) (Object) this).level().getServer()).getLevel(key);
                        if (world instanceof ServerLevel serverWorld) {
                            serversDim.put(server, serverWorld);
                        }
                    });
                });

        view.child("ServersLink")
                .flatMap(v -> v.read("GameMode", gameModeMapCodec))
                .ifPresent(gameModeMap -> this.serversGameMode = new HashMap<>(gameModeMap));
    }

    @Override
    public void servers_link$setServerPos(String name, Vec3 pos) {
        this.serversPos.put(name, pos);
    }

    @Override
    public Vec3 servers_link$getServerPos(String name) {
        return this.serversPos.get(name);
    }

    @Override
    public void servers_link$removeServerPos(String name) {
        this.serversPos.remove(name);
    }

    @Override
    public void servers_link$setServerRot(String name, float yaw, float pitch) {
        List<Float> rot = List.of(yaw, pitch);
        this.serversRot.put(name, rot);
    }

    @Override
    public List<Float> servers_link$getServerRot(String name) {
        return this.serversRot.get(name);
    }

    @Override
    public void servers_link$removeServerRot(String name) {
        this.serversRot.remove(name);
    }

    @Override
    public void servers_link$setServerDim(String name, ServerLevel dim) {
        this.serversDim.put(name, dim);
    }

    @Override
    public ServerLevel servers_link$getServerDim(String name) {
        return this.serversDim.get(name);
    }

    @Override
    public void servers_link$removeServerDim(String name) {
        this.serversDim.remove(name);
    }

    @Override
    public void servers_link$setServerGameMode(String name, GameType gameMode) {
        this.serversGameMode.put(name, gameMode.getId());
    }

    @Override
    public GameType servers_link$getServerGameMode(String name) {
        Integer id = this.serversGameMode.get(name);
        return id == null ? null : GameType.byId(id);
    }

    @Override
    public void servers_link$removeServerGameMode(String name) {
        this.serversGameMode.remove(name);
    }
}
