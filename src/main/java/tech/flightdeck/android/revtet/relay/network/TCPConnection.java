package tech.flightdeck.android.revtet.relay.network;

import lombok.extern.slf4j.Slf4j;
import tech.flightdeck.android.revtet.relay.entity.Accessory;
import tech.flightdeck.android.revtet.relay.entity.Client;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;

@Slf4j
public class TCPConnection extends AbstractConnection implements PacketSource {
    public static final int IDLE_TIMEOUT = 30000;
    private static final int MTU = 0x4000;
    private static final int MAX_PAYLOAD_SIZE = MTU - 20 - 20;

    private static final Random RANDOM = new Random();

    public enum State {
        SYN_SENT,
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT,
        LAST_ACK,
        CLOSING,
        FIN_WAIT_1,
        FIN_WAIT_2;

        public boolean isConnected() {
            return this != SYN_SENT && this != SYN_RECEIVED;
        }

        public boolean isClosed() {
            return this == FIN_WAIT_1 || this == FIN_WAIT_2 || this == CLOSING || this == LAST_ACK;
        }
    }

    private final StreamBuffer clientToNetwork = new StreamBuffer(4 * IPv4Packet.MAX_PACKET_LENGTH);
    private final Packetizer networkToClient;
    private IPv4Packet packetForClient;

    private final SocketChannel channel;
    private final SelectionKey selectionKey;
    private int interests;

    private State state;
    private int synSequenceNumber;
    private int sequenceNumber;
    private int acknowledgementNumber;
    private int theirAcknowledgementNumber;
    private Integer finSequenceNumber;
    private boolean finReceived;
    private int clientWindow;

    private long idleSince;

    public TCPConnection(ConnectionId id, Client client, Selector selector, IPv4Header iPv4Header, TCPHeader tcpHeader) throws IOException {
        super(id, client);

        TCPHeader shrinkedTcpHeader = tcpHeader.copy();
        shrinkedTcpHeader.shrinkOptions();

        networkToClient = new Packetizer(iPv4Header, shrinkedTcpHeader);
        networkToClient.getResponseIPv4Header().swapSourceAndDestination();
        networkToClient.getResponseTransportHeader().swapSourceAndDestination();

        touch();

        SelectionHandler selectionHandler = selectionKey -> {
            touch();
            if (selectionKey.isValid() && selectionKey.isConnectable()) {
                processConnect();
            }
            if (selectionKey.isValid() && selectionKey.isReadable()) {
                processReceive();
            }
            if (selectionKey.isValid() && selectionKey.isWritable()) {
                processSend();
            }
            updateInterests();
        };
        channel = createChannel();
        interests = SelectionKey.OP_CONNECT;
        selectionKey = channel.register(selector, interests, selectionHandler);
    }

    private void touch() {
        this.idleSince = System.currentTimeMillis();
    }

    private SocketChannel createChannel() throws IOException {
        log.debug("Open");
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(getRewrittenDestination());
        return socketChannel;
    }

    @Override
    public void disconnect() {
        log.debug("Close");
        log.info("Closing TCP connection with state {}", state);
        selectionKey.cancel();
        try {
            channel.close();
        } catch (IOException e) {
            log.error("Cannot close connection channel", e);
        }
    }

    private void processReceive() {
        try {
            assert packetForClient == null : "The IPv4Packet shares the networkToClient buffer, it must not be corrupted";
            int remainingClientWindow = getRemainingClientWindow();
            assert remainingClientWindow > 0 : "If remainingClientWindow is 0, then processReceive() should not have been called";
            int maxPayloadSize = Math.min(remainingClientWindow, MAX_PAYLOAD_SIZE);
            updateHeaders(TCPHeader.FLAG_ACK | TCPHeader.FLAG_PSH);
            packetForClient = networkToClient.packetize(channel, maxPayloadSize);
            if (packetForClient == null) {
                eof();
                return;
            }
            consume(this);
        } catch (IOException e) {
            log.error("Cannot read", e);
            resetConnection();
        }
    }

    private void processSend() {
        try {
            int w = clientToNetwork.writeTo(channel);
            if (w > 0) {
                acknowledgementNumber += w;

                log.debug("{} bytes written to the network socket", w);

                if (finReceived && clientToNetwork.isEmpty()) {
                    log.debug("No more pending data, process the pending FIN");
                    doHandleFin();
                } else {
                    log.debug("Sending ACK {} to client", numbers());
                    sendEmptyPacketToClient(TCPHeader.FLAG_ACK);
                }
            } else {
                close();
            }
        } catch (IOException e) {
            log.error("Cannot write", e);
            resetConnection();
        }
    }

    @Override
    public void sendToNetwork(IPv4Packet packet) {
        handlePacket(packet);
        log.debug("current ack={}", acknowledgementNumber);
        updateInterests();
    }

    private void handlePacket(IPv4Packet packet) {
        TCPHeader tcpHeader = (TCPHeader)packet.getTransportHeader();
        if (state == null) {
            handleFirstPacket(packet);
            return;
        }

        if (tcpHeader.isSyn()) {
            handleDuplicateSyn(packet);
            return;
        }

        int packetSequenceNumber = tcpHeader.getSequenceNumber();
        int expectedPacket = acknowledgementNumber + clientToNetwork.size();
        if (packetSequenceNumber != expectedPacket) {
            log.warn("Ignoring packet {} (acking {}); expecting {}; flags={}", packetSequenceNumber, tcpHeader.getAcknowledgementNumber(), expectedPacket, tcpHeader.getFlags());
            return;
        }

        clientWindow = tcpHeader.getWindow();
        theirAcknowledgementNumber = tcpHeader.getAcknowledgementNumber();

        log.debug("Receiving expected packet {} (flags={})", packetSequenceNumber, tcpHeader.getFlags());

        if (tcpHeader.isRst()) {
            log.debug("Reset requested, closing");
            close();
            return;
        }

        if (tcpHeader.isAck()) {
            log.debug("Client acked {}", tcpHeader.getAcknowledgementNumber());
            handleAck(packet);
        }

        if (tcpHeader.isFin()) {
            handleFin();
        }

        if (finSequenceNumber != null && tcpHeader.getAcknowledgementNumber() == finSequenceNumber + 1) {
            log.debug("Received ACK of FIN");
            handleFinAck();
        }
    }

    private void handleFirstPacket(IPv4Packet packet) {
        log.debug("handleFirstPacket()");
        TCPHeader tcpHeader = (TCPHeader)packet.getTransportHeader();
        if (!tcpHeader.isSyn()) {
            log.warn("Unexpected first packet {}; acking {}; flags={}", tcpHeader.getSequenceNumber(), tcpHeader.getAcknowledgementNumber(), tcpHeader.getFlags());
            sequenceNumber = tcpHeader.getAcknowledgementNumber();
            resetConnection();
            return;
        }

        int theirSequenceNumber = tcpHeader.getSequenceNumber();
        acknowledgementNumber = theirSequenceNumber + 1;
        synSequenceNumber = theirSequenceNumber;

        sequenceNumber = RANDOM.nextInt();
        log.debug("initialized seqNum={}; ackNum={}", sequenceNumber, acknowledgementNumber);
        clientWindow = tcpHeader.getWindow();
        state = State.SYN_SENT;
        log.debug("State = {}", state);
    }

    private void handleDuplicateSyn(IPv4Packet packet) {
        TCPHeader tcpHeader = (TCPHeader)packet.getTransportHeader();
        int theirSequenceNumber = tcpHeader.getSequenceNumber();
        if (state == State.SYN_SENT) {
            synSequenceNumber = theirSequenceNumber;
            acknowledgementNumber = theirSequenceNumber + 1;
        } else if (theirSequenceNumber != synSequenceNumber) {
            resetConnection();
        }
    }

    private void handleAck(IPv4Packet packet) {
        log.debug("handleAck()");
        if (state == State.SYN_RECEIVED) {
            state = State.ESTABLISHED;
            log.debug("State = {}", state);
            return;
        }

        int payloadLength = packet.getPayloadLength();
        if (payloadLength == 0) {
            return;
        }

        if (clientToNetwork.remaining() < payloadLength) {
            log.warn("Not enough space, dropping packet");
            return;
        }

        clientToNetwork.readFrom(packet.getPayload());
    }

    private void handleFin() {
        log.debug("Received a FIN from the client {}", numbers());
        finReceived = true;
        if (clientToNetwork.isEmpty()) {
            log.debug("No pending data, process the FIN immediately");
            doHandleFin();
        }
    }

    private void handleFinAck() {
        switch (state) {
            case LAST_ACK:
            case CLOSING:
                close();
                break;
            case FIN_WAIT_1:
                state = State.FIN_WAIT_2;
                log.debug("State = {}", state);
                break;
            case FIN_WAIT_2:
                break;
            default:
                log.warn("Received FIN ACK while state was {}", state);
        }
    }

    private void doHandleFin() {
        ++acknowledgementNumber;

        switch (state) {
            case ESTABLISHED:
                sendEmptyPacketToClient(TCPHeader.FLAG_FIN | TCPHeader.FLAG_ACK);
                finSequenceNumber = sequenceNumber;
                ++sequenceNumber;

                state = State.LAST_ACK;
                log.debug("State = {}", state);
                break;
            case FIN_WAIT_1:
                sendEmptyPacketToClient(TCPHeader.FLAG_ACK);
                state = State.CLOSING;
                log.debug("State = {}", state);
                break;
            case FIN_WAIT_2:
                sendEmptyPacketToClient(TCPHeader.FLAG_ACK);
                close();
                break;
            default:
                log.warn("Received FIN while state was {}", state);
        }
    }

    private void processConnect() {
        log.debug("processConnect()");
        if (!finishConnect()) {
            close();
            return;
        }
        log.debug("SYN_RECEIVED, acking {}", numbers());
        state = State.SYN_RECEIVED;
        log.debug("State = {}", state);
        sendEmptyPacketToClient(TCPHeader.FLAG_SYN | TCPHeader.FLAG_ACK);
        ++sequenceNumber;
    }

    private boolean finishConnect() {
        try {
            return channel.finishConnect();
        } catch (IOException e) {
            log.error("Cannot finish connect", e);
            return false;
        }
    }

    private void eof() {
        sendEmptyPacketToClient(TCPHeader.FLAG_FIN | TCPHeader.FLAG_ACK);

        finSequenceNumber = sequenceNumber;
        ++sequenceNumber;
        if (state == State.CLOSE_WAIT) {
            state = State.LAST_ACK;
        } else {
            state = State.FIN_WAIT_1;
        }
        log.debug("State = {}", state);
    }

    private void resetConnection() {
        log.debug("Resetting connection");
        state = null;
        sendEmptyPacketToClient(TCPHeader.FLAG_RST);
        close();
    }

    private void sendEmptyPacketToClient(int flags) {
        sendToClient(createEmptyResponsePacket(flags));
    }

    private IPv4Packet createEmptyResponsePacket(int flags) {
        updateHeaders(flags);
        IPv4Packet packet = networkToClient.packetizeEmptyPayload();
        log.debug("Forging empty response (flags={}) {}", flags, numbers());
        if ((flags & TCPHeader.FLAG_ACK) != 0) {
            log.debug("Acking {}", numbers());
        }
        return packet;
    }

    private void updateHeaders(int flags) {
        TCPHeader tcpHeader = (TCPHeader)networkToClient.getResponseTransportHeader();
        tcpHeader.setFlags(flags);
        tcpHeader.setSequenceNumber(sequenceNumber);
        tcpHeader.setAcknowledgementNumber(acknowledgementNumber);
    }

    protected void updateInterests() {
        if (!selectionKey.isValid()) {
            return;
        }
        int interestOps = 0;
        if (mayRead()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (mayWrite()) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        if (mayConnect()) {
            interestOps |= SelectionKey.OP_CONNECT;
        }
        if (interests != interestOps) {
            interests = interestOps;
            selectionKey.interestOps(interestOps);
        }
    }

    private boolean mayRead() {
        if (!state.isConnected() || state.isClosed()) {
            return false;
        }
        if (packetForClient != null) {
            return false;
        }
        return getRemainingClientWindow() > 0;
    }

    private boolean mayWrite() {
        return !clientToNetwork.isEmpty();
    }

    private boolean mayConnect() {
        return state == State.SYN_SENT;
    }

    @Override
    public IPv4Packet get() {
        updateAcknowledgementNumber(packetForClient);
        return packetForClient;
    }

    private void updateAcknowledgementNumber(IPv4Packet packet) {
        TCPHeader tcpHeader = (TCPHeader)packet.getTransportHeader();
        tcpHeader.setAcknowledgementNumber(acknowledgementNumber);
        packet.computeChecksums();
    }

    @Override
    public void next() {
        log.debug("Packet ({} bytes) sent to client {}", packetForClient.getPayloadLength(), numbers());
        sequenceNumber += packetForClient.getPayloadLength();
        packetForClient = null;
        updateInterests();
    }

    private int getRemainingClientWindow() {
        int remaining = theirAcknowledgementNumber + clientWindow - sequenceNumber;
        if (remaining < 0 || remaining > clientWindow) {
            return 0;
        }
        return remaining;
    }

    @Override
    public boolean isExpired() {
        return System.currentTimeMillis() >= idleSince + IDLE_TIMEOUT && !available();
    }

    private boolean available() {
        return this.channel.isOpen() && this.channel.isConnected() && this.state == State.ESTABLISHED;
    }

    private String numbers() {
        return String.format("(seq=%d, ack=%d)", sequenceNumber, acknowledgementNumber);
    }
}
