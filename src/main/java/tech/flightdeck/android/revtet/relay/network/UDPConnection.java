package tech.flightdeck.android.revtet.relay.network;

import lombok.extern.slf4j.Slf4j;
import tech.flightdeck.android.revtet.relay.entity.Client;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

@Slf4j
public class UDPConnection extends AbstractConnection {
    public static final long IDLE_TIMEOUT = 2 * 60 * 1000;

    private final DatagramBuffer clientToNetwork = new DatagramBuffer(4 * IPv4Packet.MAX_PACKET_LENGTH);
    private final Packetizer networkToClient;

    private final DatagramChannel channel;
    private final SelectionKey selectionKey;
    private int interests;

    private long idleSince;

    protected UDPConnection(ConnectionId id, Client client, Selector selector, IPv4Header iPv4Header, UDPHeader udpHeader) throws IOException {
        super(id, client);

        networkToClient = new Packetizer(iPv4Header, udpHeader);
        networkToClient.getResponseIPv4Header().swapSourceAndDestination();
        networkToClient.getResponseTransportHeader().swapSourceAndDestination();

        touch();

        SelectionHandler selectionHandler = selectionKey -> {
            touch();
            if (selectionKey.isValid() && selectionKey.isReadable()) {
                processReceive();
            }
            if (selectionKey.isValid() && selectionKey.isWritable()) {
                processSend();
            }
            updateInterests();
        };
        channel = createChannel();
        interests = SelectionKey.OP_READ;
        selectionKey = channel.register(selector, interests, selectionHandler);
    }

    private void touch() {
        idleSince = System.currentTimeMillis();
    }

    private DatagramChannel createChannel() throws IOException {
        log.debug("Open");
        DatagramChannel datagramChannel = DatagramChannel.open();
        datagramChannel.configureBlocking(false);
        datagramChannel.connect(getRewrittenDestination());
        return datagramChannel;
    }

    private void processReceive() {
        IPv4Packet packet = read();
        if (packet == null) {
            close();
            return;
        }
        pushToClient(packet);
    }

    private void processSend() {
        if (!write()) {
            close();
        }
    }

    private IPv4Packet read() {
        try {
            return networkToClient.packetize(channel);
        } catch (IOException e) {
            log.error("Cannot read", e);
            return null;
        }
    }

    private boolean write() {
        try {
            return clientToNetwork.writeTo(channel);
        } catch (IOException e) {
            log.error("Cannot write", e);
            return false;
        }
    }

    private void pushToClient(IPv4Packet packet) {
        if (!sendToClient(packet)) {
            log.warn("Cannot send to client, dropping packet");
            return;
        }
        log.debug("Packet ({} bytes) sent to client", packet.getPayloadLength());
    }

    private void updateInterests() {
        if (!selectionKey.isValid()) {
            return;
        }
        int interestOps = SelectionKey.OP_READ;
        if (mayWrite()) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (interests != interestOps) {
            interests = interestOps;
            selectionKey.interestOps(interestOps);
        }
    }

    private boolean mayWrite() {
        return !clientToNetwork.isEmpty();
    }

    @Override
    public void sendToNetwork(IPv4Packet packet) {
        if (!clientToNetwork.readFrom(packet.getPayload())) {
            log.warn("Cannot send to network, dropping packet");
            return;
        }
        updateInterests();
    }

    @Override
    public void disconnect() {
        log.debug("Close");
        selectionKey.cancel();
        try {
            channel.close();
        } catch (IOException e) {
            log.error("Cannot close connection channel", e);
        }
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() >= idleSince + IDLE_TIMEOUT;
    }
}
