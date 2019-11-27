package tech.flightdeck.android.revtet.relay.network;

import java.nio.ByteBuffer;

public interface TransportHeader {
    int getSourcePort();

    int getDestinationPort();

    void setSourcePort(int port);

    void setDestinationPort(int port);

    int getHeaderLength();

    void setPayloadLength(int payloadLength);

    ByteBuffer getRaw();

    TransportHeader copyTo(ByteBuffer buffer);

    void computeChecksum(IPv4Header ipv4Header, ByteBuffer payload);

    default void swapSourceAndDestination() {
        int tmp = getSourcePort();
        setSourcePort(getDestinationPort());
        setDestinationPort(tmp);
    }
}
