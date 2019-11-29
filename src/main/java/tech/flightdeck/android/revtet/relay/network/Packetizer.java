package tech.flightdeck.android.revtet.relay.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class Packetizer {
    private final ByteBuffer buffer = ByteBuffer.allocate(IPv4Packet.MAX_PACKET_LENGTH);
    private final ByteBuffer payloadBuffer;

    private final IPv4Header responseIPv4Header;
    private final TransportHeader responseTransportHeader;

    public Packetizer(IPv4Header ipv4Header, TransportHeader transportHeader) {
        responseIPv4Header = ipv4Header.copyTo(buffer);
        responseTransportHeader = transportHeader.copyTo(buffer);
        payloadBuffer = buffer.slice();
    }

    public IPv4Header getResponseIPv4Header() {
        return responseIPv4Header;
    }

    public TransportHeader getResponseTransportHeader() {
        return responseTransportHeader;
    }

    public IPv4Packet packetizeEmptyPayload() {
        payloadBuffer.limit(0).position(0);
        return inflate();
    }

    public IPv4Packet packetize(ReadableByteChannel channel, int maxChunkSize) throws IOException {
        payloadBuffer.limit(maxChunkSize).position(0);
        int payloadLength = channel.read(payloadBuffer);
        if (payloadLength == -1) {
            return null;
        }
        payloadBuffer.flip();
        return inflate();
    }

    public IPv4Packet packetize(ReadableByteChannel channel) throws IOException {
        return packetize(channel, payloadBuffer.capacity());
    }

    private IPv4Packet inflate() {
        int payloadLength = payloadBuffer.remaining();
        buffer.limit(payloadBuffer.arrayOffset() + payloadBuffer.limit()).position(0);

        int ipv4HeaderLength = responseIPv4Header.getHeaderLength();
        int transportHeaderLength = responseTransportHeader.getHeaderLength();
        int totalLength = ipv4HeaderLength + transportHeaderLength + payloadLength;

        responseIPv4Header.setTotalLength(totalLength);
        responseTransportHeader.setPayloadLength(payloadLength);

        // In order to avoid copies, buffer is shared with this IPv4Packet instance that is returned.
        // Don't use it after another call to packetize()!
        IPv4Packet packet = new IPv4Packet(buffer);
        packet.computeChecksums();
        return packet;
    }
}
