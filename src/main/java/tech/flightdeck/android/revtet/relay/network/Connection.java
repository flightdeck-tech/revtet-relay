package tech.flightdeck.android.revtet.relay.network;

public interface Connection {
    ConnectionId getId();
    void sendToNetwork(IPv4Packet packet);
    void disconnect();
    boolean isExpired();
}
