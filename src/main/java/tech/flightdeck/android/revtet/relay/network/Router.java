package tech.flightdeck.android.revtet.relay.network;

import lombok.extern.slf4j.Slf4j;
import tech.flightdeck.android.revtet.relay.entity.Client;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Router {
    private final Client client;
    private final Selector selector;

    private final List<Connection> connections = new ArrayList<>();

    public Router(Client client, Selector selector) {
        this.client = client;
        this.selector = selector;
    }

    public void sendToNetwork(IPv4Packet packet) {
        if (!packet.isValid()) {
            log.warn("Dropping invalid packet");
            return;
        }
        try {
            Connection connection = getConnection(packet.getIpv4Header(), packet.getTransportHeader());
            connection.sendToNetwork(packet);
        } catch (IOException e) {
            log.error("Cannot create connection, dropping packet", e);
        }
    }

    private Connection getConnection(IPv4Header iPv4Header, TransportHeader transportHeader) throws IOException {
        ConnectionId id = ConnectionId.from(iPv4Header, transportHeader);
        Connection connection = find(id);
        if (connection == null) {
            connection = createConnection(id, iPv4Header, transportHeader);
            connections.add(connection);
        }
        return connection;
    }

    private Connection find(ConnectionId id) {
        for (Connection connection : connections) {
            if (id.equals(connection.getId())) {
                return connection;
            }
        }
        return null;
    }

    private Connection createConnection(ConnectionId id, IPv4Header iPv4Header, TransportHeader transportHeader) throws IOException {
        IPv4Header.Protocol protocol = id.getProtocol();
        if (protocol == IPv4Header.Protocol.UDP) {
            return new UDPConnection(id, client, selector, iPv4Header, (UDPHeader) transportHeader);
        }
        if (protocol == IPv4Header.Protocol.TCP) {
            return new TCPConnection(id, client, selector, iPv4Header, (TCPHeader) transportHeader);
        }
        if (protocol == IPv4Header.Protocol.ICMP) {
            return new ICMPConnection(id, client, iPv4Header, (ICMPHeader) transportHeader);
        }
        throw new UnsupportedOperationException("Unsupported protocol: " + protocol);
    }

    public void remove(Connection connection) {
        if (!connections.remove(connection)) {
            throw new AssertionError("Removed a connection unknown from the router");
        }
    }

    public void cleanExpiredConnections() {
        for (int i = connections.size() - 1; i >= 0; --i) {
            Connection connection = connections.get(i);
            if (connection.isExpired()) {
                log.info("Remove expired connection: {}", connection.getId());
                connection.disconnect();
                connections.remove(i);
            }
        }
    }
}
