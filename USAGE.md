# Servers Link - API Usage Guide

> **Note**: Parts of this documentation were generated with AI assistance.

Diese Dokumentation erklärt alle verfügbaren API-Funktionen von Servers Link für Drittanbieter-Mods.

## Inhaltsverzeichnis

1. [API Übersicht](#api-übersicht)
2. [Server-Verwaltung](#server-verwaltung)
3. [Spieler-Verwaltung](#spieler-verwaltung)
4. [Transfer-Detection](#transfer-detection)
5. [Events](#events)
6. [Dummy Players](#dummy-players)
7. [Erweiterte Funktionen](#erweiterte-funktionen)
8. [Integration in eigene Mods](#integration-in-eigene-mods)

---

## API Übersicht

Die Hauptklasse für die API ist `io.github.kgriff0n.api.ServersLinkApi`. Alle Methoden sind statisch und können direkt aufgerufen werden.

```java
import io.github.kgriff0n.api.ServersLinkApi;
```

---

## Server-Verwaltung

### Server-Liste abrufen

```java
// Alle Server als ServerInfo-Objekte
ArrayList<ServerInfo> servers = ServersLinkApi.getServerList();

// Nur Server-Namen
ArrayList<String> serverNames = ServersLinkApi.getServerNames();

// Server nach Gruppen-ID filtern
ArrayList<ServerInfo> survivalServers = ServersLinkApi.getServers("survival");
```

### Spezifischen Server finden

```java
// Server nach Name finden
ServerInfo server = ServersLinkApi.getServer("Hub");

if (server != null) {
    System.out.println("Server gefunden: " + server.getName());
    System.out.println("IP: " + server.getIp());
    System.out.println("Port: " + server.getPort());
    System.out.println("Gruppe: " + server.getGroupId());
    System.out.println("TPS: " + server.getTps());
    System.out.println("Status: " + (server.isDown() ? "Offline" : "Online"));
}
```

### Aktuellen Server-Namen abrufen

```java
// Name des aktuellen Servers aus der Config
String currentServerName = ServersLinkApi.getCurrentServerName();
System.out.println("Du bist auf: " + currentServerName);
```

### Laufende Server zählen

```java
// Anzahl der verbundenen Sub-Server
int runningServers = ServersLinkApi.getRunningSubServers();
System.out.println(runningServers + " Server sind online");
```

---

## Spieler-Verwaltung

### Spieler lokalisieren

```java
import java.util.UUID;

// Finde heraus auf welchem Server ein Spieler ist
UUID playerUuid = player.getUuid();
String serverName = ServersLinkApi.whereIs(playerUuid);

if (serverName != null) {
    player.sendMessage(Text.literal("Spieler ist auf: " + serverName));
} else {
    player.sendMessage(Text.literal("Spieler ist offline"));
}
```

### Spieler transferieren

```java
import net.minecraft.server.network.ServerPlayerEntity;

// Transferiere einen Spieler zu einem anderen Server
ServerPlayerEntity player = // ... dein Spieler
String currentServer = ServersLinkApi.getCurrentServerName();
String targetServer = "Survival-1";

ServersLinkApi.transferPlayer(player, currentServer, targetServer);
```

### Spieler-Listen eines Servers

```java
ServerInfo server = ServersLinkApi.getServer("Hub");
if (server != null) {
    HashMap<UUID, String> players = server.getPlayersList();
    
    players.forEach((uuid, name) -> {
        System.out.println("Spieler: " + name + " (" + uuid + ")");
    });
}
```

---

## Transfer-Detection

### Erkennen ob Spieler transferiert wird oder Network verlässt

#### Methode 1: Event-basiert (Empfohlen)

```java
import io.github.kgriff0n.api.event.PlayerDisconnectCallback;
import net.fabricmc.api.ModInitializer;

public class MyMod implements ModInitializer {
    
    @Override
    public void onInitialize() {
        PlayerDisconnectCallback.EVENT.register((player, uuid, isTransfer) -> {
            if (isTransfer) {
                // Spieler wird zu einem anderen Server transferiert
                System.out.println("Transfer: " + player.getName().getString());
                // Temporäre Daten speichern, die erhalten bleiben sollen
                saveTransferData(uuid);
            } else {
                // Spieler verlässt das Network komplett
                System.out.println("Network-Verlassen: " + player.getName().getString());
                // Finale Cleanup-Aktionen
                cleanupPlayerData(uuid);
            }
        });
    }
    
    private void saveTransferData(UUID uuid) {
        // Deine Transfer-Logik
    }
    
    private void cleanupPlayerData(UUID uuid) {
        // Deine Cleanup-Logik
    }
}
```

#### Methode 2: Direkte Abfrage

```java
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class MyEventHandler implements ServerPlayConnectionEvents.Disconnect {
    
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        UUID playerUuid = handler.player.getUuid();
        
        if (ServersLinkApi.isPlayerBeingTransferred(playerUuid)) {
            // Spieler wird transferiert
            handleTransfer(handler.player);
        } else {
            // Spieler verlässt Network
            handleNetworkLeave(handler.player);
        }
    }
    
    private void handleTransfer(ServerPlayerEntity player) {
        // Transfer-Logik
    }
    
    private void handleNetworkLeave(ServerPlayerEntity player) {
        // Disconnect-Logik
    }
}
```

---

## Events

### PlayerDisconnectCallback

Dieses Event wird gefeuert, wenn ein Spieler vom Server disconnected:

```java
import io.github.kgriff0n.api.event.PlayerDisconnectCallback;

PlayerDisconnectCallback.EVENT.register((player, uuid, isTransfer) -> {
    String serverName = ServersLinkApi.getCurrentServerName();
    
    if (isTransfer) {
        System.out.println("[" + serverName + "] " + player.getName().getString() 
            + " wird transferiert");
    } else {
        System.out.println("[" + serverName + "] " + player.getName().getString() 
            + " hat Network verlassen");
    }
});
```

**Parameter:**
- `player` (ServerPlayerEntity): Der Spieler der disconnected
- `uuid` (UUID): Die UUID des Spielers
- `isTransfer` (boolean): `true` bei Transfer, `false` bei Network-Verlassen

---

## Dummy Players

Dummy Players sind Spieler-Objekte, die andere Server repräsentieren und in der Spielerliste angezeigt werden.

### Dummy Player hinzufügen

```java
import com.mojang.authlib.GameProfile;

// Erstelle ein GameProfile mit UUID, Name und Textures
GameProfile profile = new GameProfile(uuid, playerName);
// Füge Properties/Textures hinzu...

ServersLinkApi.addDummyPlayer(profile);
```

### Dummy Players abrufen

```java
// Alle Dummy Players
List<DummyPlayer> dummies = ServersLinkApi.getDummyPlayers();

// Spezifischen Dummy Player nach UUID
ServerPlayerEntity dummy = ServersLinkApi.getDummyPlayer(playerUuid);

// Spezifischen Dummy Player nach Name
ServerPlayerEntity dummy = ServersLinkApi.getDummyPlayer("PlayerName");
```

---

## Erweiterte Funktionen

### Nachrichten an Operatoren senden

```java
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

Text message = Text.literal("Server-Warnung!").formatted(Formatting.RED);
ServersLinkApi.broadcastToOp(message);
```

### Pakete senden

```java
import io.github.kgriff0n.packet.Packet;

// Sende ein Packet
// Vom Hub: wird an alle Sub-Server gesendet
// Von Sub-Server: wird an Hub gesendet
Packet myPacket = new MyCustomPacket();
String sourceServer = ServersLinkApi.getCurrentServerName();
ServersLinkApi.send(myPacket, sourceServer);
```

### Prevent-Listen (Intern)

Diese Listen werden intern verwendet, um Join/Disconnect-Nachrichten bei Transfers zu unterdrücken:

```java
// Nur für fortgeschrittene Nutzung - normalerweise nicht direkt benötigt
HashSet<UUID> preventConnect = ServersLinkApi.getPreventConnect();
HashSet<UUID> preventDisconnect = ServersLinkApi.getPreventDisconnect();
```

---

## Integration in eigene Mods

### Dependencies hinzufügen

**build.gradle:**
```gradle
repositories {
    maven { url "https://dein-maven-repo.com/releases" }
}

dependencies {
    modImplementation "io.github.kgriff0n:servers-link:VERSION"
    include "io.github.kgriff0n:servers-link:VERSION"
}
```

**fabric.mod.json:**
```json
{
  "depends": {
    "servers-link": ">=2.0.0"
  }
}
```

### Praktisches Beispiel: Statistik-Tracker

```java
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.api.event.PlayerDisconnectCallback;
import net.fabricmc.api.ModInitializer;

public class PlayerStatsTracker implements ModInitializer {
    
    private final HashMap<UUID, Long> sessionStartTimes = new HashMap<>();
    
    @Override
    public void onInitialize() {
        // Registriere Disconnect-Handler
        PlayerDisconnectCallback.EVENT.register(this::onPlayerDisconnect);
    }
    
    private void onPlayerDisconnect(ServerPlayerEntity player, UUID uuid, boolean isTransfer) {
        String currentServer = ServersLinkApi.getCurrentServerName();
        Long startTime = sessionStartTimes.get(uuid);
        
        if (startTime != null) {
            long playTime = System.currentTimeMillis() - startTime;
            
            if (isTransfer) {
                // Spieler wechselt Server - speichere Session temporär
                System.out.println("[" + currentServer + "] " 
                    + player.getName().getString() 
                    + " wechselt Server (Spielzeit: " + playTime + "ms)");
                
                // Session-Daten bleiben erhalten für Transfer
                // sessionStartTimes.remove(uuid); <- NICHT löschen bei Transfer!
                
            } else {
                // Spieler verlässt Network - finale Speicherung
                System.out.println("[" + currentServer + "] " 
                    + player.getName().getString() 
                    + " verlässt Network (Spielzeit: " + playTime + "ms)");
                
                // Jetzt komplett aufräumen
                sessionStartTimes.remove(uuid);
                savePlayerStats(uuid, playTime);
            }
        }
    }
    
    private void savePlayerStats(UUID uuid, long playTime) {
        // Speichere in Datenbank, File, etc.
    }
}
```

### Beispiel: Server-Status Display

```java
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.ServerInfo;

public class ServerStatusCommand {
    
    public void showServerStatus(ServerPlayerEntity player) {
        ArrayList<ServerInfo> servers = ServersLinkApi.getServerList();
        
        player.sendMessage(Text.literal("=== Server Status ===")
            .formatted(Formatting.GOLD, Formatting.BOLD));
        
        for (ServerInfo server : servers) {
            int playerCount = server.getPlayersList().size();
            float tps = server.getTps();
            boolean isDown = server.isDown();
            
            String status = isDown ? "Offline" : "Online";
            Formatting color = isDown ? Formatting.RED : Formatting.GREEN;
            
            player.sendMessage(Text.literal(
                server.getName() + ": " + status + 
                " | Spieler: " + playerCount + 
                " | TPS: " + String.format("%.1f", tps)
            ).formatted(color));
        }
    }
}
```

---

## API-Referenz - Alle Methoden

### ServersLinkApi Methoden

| Methode | Rückgabe | Beschreibung |
|---------|----------|--------------|
| `getServerList()` | `ArrayList<ServerInfo>` | Alle Server |
| `getServerNames()` | `ArrayList<String>` | Alle Server-Namen |
| `getServers(String groupId)` | `ArrayList<ServerInfo>` | Server einer Gruppe |
| `getServer(String serverName)` | `ServerInfo` | Server nach Name |
| `getCurrentServerName()` | `String` | Name des aktuellen Servers |
| `getRunningSubServers()` | `int` | Anzahl verbundener Sub-Server |
| `whereIs(UUID uuid)` | `String` | Server auf dem ein Spieler ist |
| `isPlayerBeingTransferred(UUID uuid)` | `boolean` | Ob Spieler transferiert wird |
| `transferPlayer(...)` | `void` | Transferiere Spieler zu anderem Server |
| `addDummyPlayer(GameProfile)` | `void` | Füge Dummy Player hinzu |
| `getDummyPlayers()` | `List<DummyPlayer>` | Alle Dummy Players |
| `getDummyPlayer(UUID)` | `ServerPlayerEntity` | Dummy Player nach UUID |
| `getDummyPlayer(String)` | `ServerPlayerEntity` | Dummy Player nach Name |
| `broadcastToOp(Text)` | `void` | Nachricht an alle Ops |
| `send(Packet, String)` | `void` | Sende Packet |

### ServerInfo Methoden

| Methode | Rückgabe | Beschreibung |
|---------|----------|--------------|
| `getName()` | `String` | Server-Name |
| `getIp()` | `String` | Server-IP |
| `getPort()` | `int` | Server-Port |
| `getGroupId()` | `String` | Gruppen-ID |
| `getTps()` | `float` | Server TPS |
| `isDown()` | `boolean` | Ob Server offline ist |
| `getPlayersList()` | `HashMap<UUID, String>` | Spieler auf diesem Server |
| `getGameProfile()` | `List<GameProfile>` | GameProfiles aller Spieler |

---

## Support

Bei Fragen oder Problemen:
- Discord: https://discord.com/invite/ZeHm57BEyt
- GitHub Issues: [Repository Issues]

---

**Letzte Aktualisierung**: 2025-10-27


