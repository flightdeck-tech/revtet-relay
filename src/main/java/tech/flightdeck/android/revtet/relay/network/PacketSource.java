package tech.flightdeck.android.revtet.relay.network;

public interface PacketSource {
    IPv4Packet get();

    void next();
}
