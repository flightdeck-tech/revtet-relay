package tech.flightdeck.android.revtet.relay.entity;

import lombok.extern.slf4j.Slf4j;
import tech.flightdeck.android.revtet.relay.listener.CloseListener;

import javax.usb.*;
import javax.usb.event.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class Accessory implements ByteChannel {
    private static final int BUF_SIZE = 1048576;

    private static final byte ACCESSORY_EP_IN = (byte)0x81;
    private static final byte ACCESSORY_EP_OUT = (byte)0x01;

    private UsbDevice device;
    private UsbInterface usbInterface;
    private UsbPipe pipeIn;
    private UsbPipe pipeOut;

    private Client client;

    private Thread readThread;
    private Thread writeThread;
    private volatile boolean running;

    private ByteBuffer readBuffer = ByteBuffer.allocate(BUF_SIZE);
    private ByteBuffer writeBuffer = ByteBuffer.allocate(BUF_SIZE);
    private BlockingQueue<UsbIrp> writeQueue = new LinkedBlockingQueue<>();

    public Accessory(UsbDevice device, Selector selector) throws UsbException {
        log.info("Accessory {} initialized.", device);
        this.device = device;
        UsbConfiguration conf = device.getActiveUsbConfiguration();
        usbInterface = conf.getUsbInterface((byte)0);
        usbInterface.claim(usbInterface -> true);
        for (UsbEndpoint endpoint : (List<UsbEndpoint>)usbInterface.getUsbEndpoints()) {
            switch (endpoint.getDirection()) {
                case UsbConst.ENDPOINT_DIRECTION_IN:
                    pipeIn = endpoint.getUsbPipe();
                    break;
                case UsbConst.ENDPOINT_DIRECTION_OUT:
                    pipeOut = endpoint.getUsbPipe();
                    pipeOut.open();
                    break;
            }
        }

        client = new Client(this, selector, new CloseListener<Client>() {
            @Override
            public void onClosed(Client object) {

            }
        });

        readThread = new Thread(() -> {
            running = true;
            byte[] buf = new byte[BUF_SIZE];
            try {
                pipeIn.open();
                while (running) {
                    UsbIrp irp = pipeIn.createUsbIrp();
                    irp.setData(buf);
                    pipeIn.syncSubmit(irp);
                    log.debug("USB received {} bytes.", irp.getActualLength());
                    synchronized (readBuffer) {
                        readBuffer.put(irp.getData(), 0, irp.getActualLength());
                    }
                }
            } catch (UsbException e) {
                try {
                    log.error("Read error, closing.", e);
                    close();
                } catch (IOException ioe) {
                    log.error("Failed to close usb.", ioe);
                }
            }
        }, String.format("USB Reader %d", client.getId()));
        readThread.start();

        writeThread = new Thread(() -> {
            try {
                while (running) {
                    UsbIrp irp = writeQueue.take();
                    pipeOut.syncSubmit(irp);
                }
            } catch (Exception e) {
                log.error("Write error, closing.", e);
                try {
                    close();
                } catch (IOException e1) {
                    log.error("Failed to close usb.", e1);
                }
            }

//            byte[] buf;
//            int len;
//            while (running) {
//                buf = null;
//                synchronized (writeBuffer) {
//                    writeBuffer.flip();
//                    if (writeBuffer.hasRemaining()) {
//                        buf = new byte[writeBuffer.remaining()];
//                        writeBuffer.get(buf);
//                    }
//                    writeBuffer.clear();
//                    writeBuffer.notify();
//                }
//                if (buf != null) {
//                    try {
//                        pipeOut.syncSubmit(buf);
//                    } catch (UsbException e) {
//                        log.error("Failed to write usb", e);
//                        try {
//                            close();
//                        } catch (IOException e1) {
//
//                        }
//                    }
//                }
//            }
        }, String.format("USB Writer %d", client.getId()));
        writeThread.start();
    }

    public void cleanUp() {
        client.cleanExpiredConnections();
    }

    @Override
    public boolean isOpen() {
        return pipeIn.isOpen() && pipeOut.isOpen();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int count = 0;
        synchronized (readBuffer) {
            readBuffer.flip();
            while (readBuffer.hasRemaining()) {
                dst.put(readBuffer.get());
                count++;
            }
            readBuffer.compact();
        }
        return count;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
//        int count = 0;
//        if (src.hasRemaining()) {
//            try {
//                synchronized (writeBuffer) {
//                    if (writeBuffer.hasRemaining()) {
//                        writeBuffer.wait();
//                    }
//                    while (src.hasRemaining()) {
//                        try {
//                            writeBuffer.put(src.get());
//                        } catch (Exception e) {
//                            log.error("src {} buf {}", src, writeBuffer, e);
//                        }
//                        count++;
//                    }
//                }
//            } catch (Exception e) {
//                log.error("Failed to write to usb buffer, drop.", e);
//            } finally {
//                src.clear();
//            }
//        }
//        return count;
        int len;
        if (src.hasRemaining()) {
            len = src.remaining();
            byte[] buf = new byte[len];
            src.get(buf);
            UsbIrp irp = pipeOut.createUsbIrp();
            irp.setData(buf);
            irp.setActualLength(len);
//            writeQueue.add(irp);
            try {
                pipeOut.asyncSubmit(irp);
            } catch (UsbException e) {
                log.error("Failed to async write to usb.", e);
            }
        } else {
            len = 0;
        }
        return len;
    }

    public void processReceive() {
        if (readBuffer.hasRemaining()) {
            client.processReceive();
        }
    }

    public void processSend() {
        client.processSend();
    }

    @Override
    public void close() throws IOException {
        running = false;
        try {
            usbInterface.release();
        } catch (UsbException e) {
            throw new IOException(e);
        }
    }
}
