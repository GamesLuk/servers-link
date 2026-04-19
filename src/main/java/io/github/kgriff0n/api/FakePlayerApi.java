package io.github.kgriff0n.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Method;
import java.util.UUID;

public class FakePlayerApi {

    // Checks once if the mod is present on the server
    private static final boolean IS_LOADED = FabricLoader.getInstance().isModLoaded("fakeplayerapi");

    public static boolean spawnFake(MinecraftServer server, String username) {
        if (!IS_LOADED) return false;
        try {
            Class<?> apiClass = Class.forName("de.gamesluk.fakeplayerapi.api.FakePlayerAPI");
            Method method = apiClass.getMethod("spawnFakePlayer", MinecraftServer.class, String.class);
            return (boolean) method.invoke(null, server, username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteFake(MinecraftServer server, String username) {
        if (!IS_LOADED) return false;
        try {
            Class<?> apiClass = Class.forName("de.gamesluk.fakeplayerapi.api.FakePlayerAPI");
            Method method = apiClass.getMethod("deleteFakePlayer", MinecraftServer.class, String.class);
            return (boolean) method.invoke(null, server, username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isFake(MinecraftServer server, String username) {
        if (!IS_LOADED) return false;
        try {
            Class<?> apiClass = Class.forName("de.gamesluk.fakeplayerapi.api.FakePlayerAPI");
            Method method = apiClass.getMethod("isFake", MinecraftServer.class, String.class);
            return (boolean) method.invoke(null, server, username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isFake(MinecraftServer server, UUID uuid) {
        if (!IS_LOADED || uuid == null) return false;
        try {
            Class<?> apiClass = Class.forName("de.gamesluk.fakeplayerapi.api.FakePlayerAPI");
            Method method = apiClass.getMethod("isFake", MinecraftServer.class, UUID.class);
            return (boolean) method.invoke(null, server, uuid);
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isFake(MinecraftServer server, ServerPlayerEntity player) {
        if (!IS_LOADED || player == null) return false;
        try {
            Class<?> apiClass = Class.forName("de.gamesluk.fakeplayerapi.api.FakePlayerAPI");
            Method method = apiClass.getMethod("isFake", ServerPlayerEntity.class);
            return (boolean) method.invoke(null, player);
        } catch (NoSuchMethodException ignored) {
            if (isFake(server, player.getUuid())) {
                return true;
            }
            return isFake(server, player.getName().getString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}