package tech.flightdeck.android.revtet.relay.entity;

import lombok.extern.slf4j.Slf4j;

import javax.usb.*;
import javax.usb.event.UsbDeviceDataEvent;
import javax.usb.event.UsbDeviceErrorEvent;
import javax.usb.event.UsbDeviceEvent;
import javax.usb.event.UsbDeviceListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

@Slf4j
public class Accessory implements UsbDeviceListener, ReadableByteChannel, WritableByteChannel {
    private static final byte ACCESSORY_EP_IN = (byte)0x81;
    private static final byte ACCESSORY_EP_OUT = (byte)0x01;

    private UsbDevice device;
    private UsbInterface usbInterface;
    private UsbEndpoint epIn;
    private UsbEndpoint epOut;

    public Accessory(UsbDevice device) throws UsbException {
        log.info("Accessory {} initialized.", device);
        this.device = device;
        UsbConfiguration conf = device.getActiveUsbConfiguration();
        usbInterface = conf.getUsbInterface((byte)0);
        usbInterface.claim(new UsbInterfacePolicy() {
            @Override
            public boolean forceClaim(UsbInterface usbInterface) {
                return true;
            }
        });
        epIn = usbInterface.getUsbEndpoint(ACCESSORY_EP_IN);
        epOut = usbInterface.getUsbEndpoint(ACCESSORY_EP_OUT);
        device.addUsbDeviceListener(this);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return 0;
    }

    @Override
    public boolean isOpen() {
        return usbInterface.isActive() && usbInterface.isClaimed();
    }

    @Override
    public void close() throws IOException {
        try {
            usbInterface.release();
        } catch (UsbException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void usbDeviceDetached(UsbDeviceEvent usbDeviceEvent) {

    }

    @Override
    public void errorEventOccurred(UsbDeviceErrorEvent usbDeviceErrorEvent) {

    }

    @Override
    public void dataEventOccurred(UsbDeviceDataEvent usbDeviceDataEvent) {
        log.info("USB Received {} bytes.", usbDeviceDataEvent.getData().length);
    }

    public interface AccessoryListener {
        void onReceive(byte[] buffer, int len);
    }
}
