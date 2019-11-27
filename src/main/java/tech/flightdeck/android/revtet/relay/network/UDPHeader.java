package tech.flightdeck.android.revtet.relay.network;

import java.nio.ByteBuffer;

public class UDPHeader implements TransportHeader {
    private static final int UDP_HEADER_LENGTH = 8;

    private final ByteBuffer raw;
    private int sourcePort;
    private int destinationPort;

    public UDPHeader(ByteBuffer raw) {
        this.raw = raw;
        raw.limit(UDP_HEADER_LENGTH);
        sourcePort = Short.toUnsignedInt(raw.getShort(0));
        destinationPort = Short.toUnsignedInt(raw.getShort(2));
    }

    @Override
    public int getSourcePort() {
        return sourcePort;
    }

    @Override
    public int getDestinationPort() {
        return destinationPort;
    }

    @Override
    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
        raw.putShort(0, (short) sourcePort);
    }

    @Override
    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
        raw.putShort(2, (short) destinationPort);
    }

    @Override
    public int getHeaderLength() {
        return UDP_HEADER_LENGTH;
    }

    @Override
    public void setPayloadLength(int payloadLength) {
        int length = getHeaderLength() + payloadLength;
        raw.putShort(4, (short) length);
    }

    @Override
    public ByteBuffer getRaw() {
        raw.rewind();
        return raw.slice();
    }

    @Override
    public UDPHeader copyTo(ByteBuffer target) {
        raw.rewind();
        ByteBuffer slice = Binary.slice(target, target.position(), getHeaderLength());
        target.put(raw);
        return new UDPHeader(slice);
    }

    @Override
    public void computeChecksum(IPv4Header ipv4Header, ByteBuffer payload) {
        // disable checksum validation
        raw.putShort(6, (short) 0);
    }
}
