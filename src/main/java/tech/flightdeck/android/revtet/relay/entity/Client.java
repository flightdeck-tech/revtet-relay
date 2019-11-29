package tech.flightdeck.android.revtet.relay.entity;

import lombok.extern.slf4j.Slf4j;
import tech.flightdeck.android.revtet.relay.listener.CloseListener;
import tech.flightdeck.android.revtet.relay.network.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class Client {
    private static int nextId = 0;

    private final int id;
    private final Accessory clientChannel;
    private final CloseListener<Client> closeListener;

    private final IPv4PacketBuffer clientToNetwork = new IPv4PacketBuffer();
    private final StreamBuffer networkToClient = new StreamBuffer(16 * IPv4Packet.MAX_PACKET_LENGTH);
    private final Router router;

    private final List<PacketSource> pendingPacketSources = new ArrayList<>();

    private ByteBuffer pendingIdBuffer;

    public Client(Accessory clientChannel, Selector selector, CloseListener<Client> closeListener) {
        id = nextId++;
        this.clientChannel = clientChannel;
        router = new Router(this, selector);
        pendingIdBuffer = createIntBuffer(id);

        this.closeListener = closeListener;
    }

    private static ByteBuffer createIntBuffer(int value) {
        final int intSize = 4;
        ByteBuffer buffer = ByteBuffer.allocate(intSize);
        buffer.putInt(value);
        buffer.flip();
        return buffer;
    }

    public int getId() {
        return id;
    }

    public void processReceive() {
        if (!read()) {
            close();
            return;
        }
        pushToNetwork();
    }

    public void processSend() {
        if (!write()) {
            close();
            return;
        }
        processPending();
    }

    private void processPending() {
        Iterator<PacketSource> iterator = pendingPacketSources.iterator();
        while (iterator.hasNext()) {
            PacketSource packetSource = iterator.next();
            IPv4Packet packet = packetSource.get();
            if (sendToClient(packet)) {
                packetSource.next();
                log.debug("Pending packet sent to client ({})", packet.getRawLength());
                iterator.remove();
            } else {
                log.warn("Pending packet not sent to client ({}), client buffer full again", packet.getRawLength());
                return;
            }
        }
    }

    private void pushToNetwork() {
        IPv4Packet packet;
        while((packet = clientToNetwork.asIPv4Packet()) != null) {
            router.sendToNetwork(packet);
            clientToNetwork.next();
        }
    }

    private boolean read() {
        try {
            return clientToNetwork.readFrom(clientChannel) != -1;
        } catch (IOException e) {
            log.error("Cannot read", e);
            return false;
        }
    }

    private boolean write() {
        try {
            return networkToClient.writeTo(clientChannel) != -1;
        } catch (IOException e) {
            log.error("Cannot write", e);
            return false;
        }
    }

    private void close() {
        try {
            clientChannel.close();
        } catch (IOException e) {
            log.error("Cannot close client connection", e);
        }
    }

    public boolean sendToClient(IPv4Packet packet) {
        synchronized (networkToClient) {
            if (networkToClient.remaining() < packet.getRawLength()) {
                log.warn("Client buffer full");
                return false;
            }
            networkToClient.readFrom(packet.getRaw());
        }
        //interests
        return true;
    }

    public void consume(PacketSource source) {
        IPv4Packet packet = source.get();
        if (sendToClient(packet)) {
            source.next();
            return;
        }
        assert !pendingPacketSources.contains(source);
        pendingPacketSources.add(source);
    }

    public Router getRouter() {
        return router;
    }

    public void cleanExpiredConnections() {
        router.cleanExpiredConnections();
    }
}
