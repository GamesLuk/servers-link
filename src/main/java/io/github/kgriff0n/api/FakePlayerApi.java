package io.github.kgriff0n.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

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
}