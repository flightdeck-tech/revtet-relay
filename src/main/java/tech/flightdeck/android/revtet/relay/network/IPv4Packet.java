package tech.flightdeck.android.revtet.relay.network;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Slf4j
public class IPv4Packet {
    private static final String TAG = IPv4Packet.class.getSimpleName();

    @SuppressWarnings("checkstyle:MagicNumber")
    public static final int MAX_PACKET_LENGTH = 1 << 16; // packet length is stored on 16 bits

    private final ByteBuffer raw;
    private final IPv4Header ipv4Header;
    private final TransportHeader transportHeader;

    public IPv4Packet(ByteBuffer raw) {
        this.raw = raw;
        raw.rewind();

        ipv4Header = new IPv4Header(raw.duplicate());
        if (!ipv4Header.isSupported()) {
            log.debug("Unsupported IPv4 headers");
            transportHeader = null;
            return;
        }
        transportHeader = createTransportHeader();
        raw.limit(ipv4Header.getTotalLength());
    }

    public boolean isValid() {
        return transportHeader != null;
    }

    private TransportHeader createTransportHeader() {
        IPv4Header.Protocol protocol = ipv4Header.getProtocol();
        switch (protocol) {
            case UDP:
                return new UDPHeader(getRawTransport());
            case TCP:
                return new TCPHeader(getRawTransport());
            case ICMP:
                return new ICMPHeader(getRawTransport());
            default:
                throw new AssertionError("Should be unreachable if ipv4Header.isSupported()");
        }
    }

    private ByteBuffer getRawTransport() {
        raw.position(ipv4Header.getHeaderLength());
        return raw.slice();
    }

    public IPv4Header getIpv4Header() {
        return ipv4Header;
    }

    public TransportHeader getTransportHeader() {
        return transportHeader;
    }

    public void swapSourceAndDestination() {
        ipv4Header.swapSourceAndDestination();
        transportHeader.swapSourceAndDestination();
    }

    public ByteBuffer getRaw() {
        raw.rewind();
        return raw.duplicate();
    }

    public int getRawLength() {
        return raw.limit();
    }

    public ByteBuffer getPayload() {
        int headersLength = ipv4Header.getHeaderLength() + transportHeader.getHeaderLength();
        raw.position(headersLength);
        return raw.slice();
    }

    public int getPayloadLength() {
        return raw.limit() - ipv4Header.getHeaderLength() - transportHeader.getHeaderLength();
    }

    public void computeChecksums() {
        ipv4Header.computeChecksum();
        transportHeader.computeChecksum(ipv4Header, getPayload());
    }
}
