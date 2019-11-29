package tech.flightdeck.android.revtet.relay.network;

import tech.flightdeck.android.revtet.relay.entity.Client;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ICMPConnection extends AbstractConnection {
    public static final int IDLE_TIMEOUT = 10 * 1000;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private final Packetizer networkToClient;
    private final InetAddress destination;

    private long idleSince;

    public ICMPConnection(ConnectionId id, Client client, IPv4Header iPv4Header, ICMPHeader icmpHeader) {
        super(id, client);

        networkToClient = new Packetizer(iPv4Header, icmpHeader);
        networkToClient.getResponseIPv4Header().swapSourceAndDestination();

        destination = Net.toInetAddress(iPv4Header.getDestination());

        touch();
    }

    private void touch() {
        this.idleSince = System.currentTimeMillis();
    }

    @Override
    public void sendToNetwork(IPv4Packet packet) {
        touch();
        executorService.submit(() -> {
            try {
                if (destination.isReachable(5000)) {
                    ICMPHeader origin = (ICMPHeader) packet.getTransportHeader();
                    IPv4Packet p = networkToClient.packetize(packet.getPayload(), 0, packet.getPayloadLength());
                    ICMPHeader h = (ICMPHeader) p.getTransportHeader();
                    h.setPong();
                    h.setSeq(origin.getSeq());
                    h.computeChecksum(p.getIpv4Header(), p.getPayload());
                    sendToClient(p);
                }
            } catch (IOException e) {

            }
        });
    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() >= idleSince + IDLE_TIMEOUT;
    }
}
