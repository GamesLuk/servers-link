package io.github.kgriff0n.socket;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.NewServerPacket;
import io.github.kgriff0n.packet.info.ServerStatusPacket;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class SubServer extends Thread {

    private static SubServer connection;

    private ExecutorService executor;

    public static SubServer getInstance() {
        return connection;
    }

    private String ip;
    private int port;

    @SuppressWarnings("FieldCanBeLocal")
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public SubServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        if (connection == null) {

            connection = this;
            executor  = Executors.newSingleThreadExecutor();
        } else {
            ServersLink.LOGGER.error("Connection already established");
        }
    }

    public synchronized void send(Packet packet) {
        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Attempting to send packet: {}", packet.getClass().getName());

        if (executor.isShutdown()) {
            ServersLink.LOGGER.warn("[DEBUG] SUBSERVER - FAILED: Executor is shutdown, can't send {}", packet.getClass().getName());
        } else {
            executor.submit(() -> {
                try {
                    if (out != null) {
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Writing packet {} to output stream", packet.getClass().getName());
                        out.writeObject(packet);
                        out.flush();
                        out.reset();
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Successfully sent packet: {}", packet.getClass().getName());
                    } else {
                        ServersLink.LOGGER.error("[DEBUG] SUBSERVER - FAILED: Output stream is NULL, can't send {}", packet.getClass().getName());
                    }
                } catch (IOException e) {
                    ServersLink.LOGGER.error("[DEBUG] SUBSERVER - IOException while sending {}: {}", packet.getClass().getName(), e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public void run() {
        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Thread started");
        while (IS_RUNNING) {
            try {
                if (clientSocket == null || clientSocket.isClosed() || !clientSocket.isConnected()) {
                    try {
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Connecting to gateway at {}:{}...", ip, port);
                        clientSocket = new Socket(ip, port);
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Socket connected, creating streams...");

                        out = new ObjectOutputStream(clientSocket.getOutputStream());
                        out.flush();
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Output stream created");

                        in = new ObjectInputStream(clientSocket.getInputStream());
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Input stream created");

                        // Send handshake packets
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Sending handshake packets...");
                        send(new NewServerPacket(ServersLink.getServerInfo()));
                        send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 20.0f, false));

                        ServersLink.setGatewayAvailable(true);
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Connected to gateway successfully!");

                    } catch (IOException e) {
                        ServersLink.setGatewayAvailable(false);
                        ServersLink.LOGGER.error("[DEBUG] SUBSERVER - Unable to connect to gateway: {}", e.getMessage(), e);
                        try {
                            if (clientSocket != null) clientSocket.close();
                        } catch (IOException ex) {
                            // ignore
                        }
                        clientSocket = null;
                        out = null;
                        in = null;

                        try {
                            Thread.sleep(60000); // Wait 1 min
                        } catch (InterruptedException ex) {
                            break;
                        }
                        continue;
                    }
                }

                try {
                    ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Waiting for packet from gateway...");
                    Packet pkt = ((Packet)in.readObject());
                    ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Received packet: {}", pkt.getClass().getName());
                    SERVER.execute(() -> {
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Executing onReceive for packet: {}", pkt.getClass().getName());
                        pkt.onReceive();
                        ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Finished executing packet: {}", pkt.getClass().getName());
                    });
                } catch (IOException e) {
                    ServersLink.LOGGER.error("[DEBUG] SUBSERVER - Gateway disconnected: {}", e.getMessage(), e);
                    try {
                        if (clientSocket != null) clientSocket.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                    clientSocket = null;
                    out = null;
                    in = null;

                } catch (ClassNotFoundException e) {
                    ServersLink.LOGGER.error("[DEBUG] SUBSERVER - Received invalid data (ClassNotFoundException): {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                ServersLink.LOGGER.error("Unexpected error in SubServer loop, waiting 60 seconds", e);
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        executor.shutdown();
    }
}
