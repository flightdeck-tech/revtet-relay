package tech.flightdeck.android.revtet.relay.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class ICMPHeader implements TransportHeader {
    private static final int ICMP_HEADER_LENGTH = 8;

    private final ByteBuffer raw;
    private int sourcePort;
    private int destinationPort;
    private int payloadLength;

    public ICMPHeader(ByteBuffer raw) {
        this.raw = raw;
        payloadLength = raw.limit() - getHeaderLength();
        raw.limit(ICMP_HEADER_LENGTH);
        this.sourcePort = raw.getShort(4);
    }

    public void setPong() {
        raw.put(0, (byte)0);
        raw.put(1, (byte)0);
    }

    public boolean isPing() {
        return raw.get(0) == 8;
    }

    public short getSeq() {
        return raw.getShort(6);
    }

    public void setSeq(short seq) {
        raw.putShort(6, seq);
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
    public void setSourcePort(int port) {
        sourcePort = port;
    }

    @Override
    public void setDestinationPort(int port) {
        destinationPort = port;
    }

    @Override
    public int getHeaderLength() {
        return ICMP_HEADER_LENGTH;
    }

    @Override
    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    @Override
    public ByteBuffer getRaw() {
        return raw;
    }

    @Override
    public TransportHeader copyTo(ByteBuffer target) {
        raw.rewind();
        ByteBuffer slice = Binary.slice(target, target.position(), getHeaderLength());
        target.put(raw);
        return new ICMPHeader(slice);
    }

    @Override
    public void computeChecksum(IPv4Header ipv4Header, ByteBuffer payload) {
        // checksum computation is the most CPU-intensive task in gnirehtet
        // prefer optimization over readability
        byte[] rawArray = raw.array();
        int rawOffset = raw.arrayOffset();

        byte[] payloadArray = payload.array();
        int payloadOffset = payload.arrayOffset();

        // reset checksum field
        setChecksum((short) 0);

        int sum = 0;

        for (int i = 0; i < getHeaderLength() / 2; ++i) {
            // compute a 16-bit value from two 8-bit values manually
            sum += ((rawArray[rawOffset + 2 * i] & 0xff) << 8) | (rawArray[rawOffset + 2 * i + 1] & 0xff);
        }

        int payloadLength = ipv4Header.getTotalLength() - ipv4Header.getHeaderLength() - ICMP_HEADER_LENGTH;
        assert payloadLength == payload.limit() : "Payload length does not match";
        for (int i = 0; i < payloadLength / 2; ++i) {
            // compute a 16-bit value from two 8-bit values manually
            sum += ((payloadArray[payloadOffset + 2 * i] & 0xff) << 8) | (payloadArray[payloadOffset + 2 * i + 1] & 0xff);
        }
        if (payloadLength % 2 != 0) {
            sum += (payloadArray[payloadOffset + payloadLength - 1] & 0xff) << 8;
        }

        while ((sum & ~0xffff) != 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        setChecksum((short) ~sum);
    }

    private void setChecksum(short checksum) {
        raw.putShort(2, checksum);
    }

    public short getChecksum() {
        return raw.getShort(2);
    }
}
