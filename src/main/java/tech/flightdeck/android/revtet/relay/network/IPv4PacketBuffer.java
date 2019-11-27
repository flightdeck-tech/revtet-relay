package tech.flightdeck.android.revtet.relay.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class IPv4PacketBuffer {
    private final ByteBuffer buffer = ByteBuffer.allocate(IPv4Packet.MAX_PACKET_LENGTH);

    public int readFrom(ReadableByteChannel channel) throws IOException {
        return channel.read(buffer);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private int getAvailablePacketLength() {
        int length = IPv4Header.readLength(buffer);
        assert length == -1 || IPv4Header.readVersion(buffer) == 4 : "This function must not be called when the packet is not IPv4";
        if (length == -1) {
            // no packet
            return 0;
        }
        if (length > buffer.remaining()) {
            // no full packet available
            return 0;
        }
        return length;
    }

    public IPv4Packet asIPv4Packet() {
        buffer.flip();
        int length = getAvailablePacketLength();
        if (length == 0) {
            buffer.compact();
            return null;
        }
        int limit = buffer.limit();
        buffer.limit(length).position(0);
        ByteBuffer packetBuffer = buffer.slice();
        buffer.limit(limit).position(length);
        // In order to avoid copies, packetBuffer is shared with this IPv4Packet instance that is returned.
        // Don't use it after another call to next()!
        return new IPv4Packet(packetBuffer);
    }

    public void next() {
        buffer.compact();
    }
}
