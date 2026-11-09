package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.PacketHeader;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class ServerStopPacket extends PacketHeader implements Packet {

    public ServerStopPacket(String sender, String recipient) {
        super(sender, recipient);
    }

    @Override
    public void onReceive() {
        SERVER.halt(false); // onReceive is always executed on server thread
        IS_RUNNING = false;
    }
}
