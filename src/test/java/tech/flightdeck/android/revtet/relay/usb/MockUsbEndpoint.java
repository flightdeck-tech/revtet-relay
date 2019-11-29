package tech.flightdeck.android.revtet.relay.usb;

import javax.usb.UsbEndpoint;
import javax.usb.UsbEndpointDescriptor;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;

public class MockUsbEndpoint implements UsbEndpoint {
    private byte direction;
    private UsbPipe pipe;

    public MockUsbEndpoint(byte direction) {
        this.direction = direction;
        this.pipe = new MockUsbPipe();
    }

    @Override
    public UsbInterface getUsbInterface() {
        return null;
    }

    @Override
    public UsbEndpointDescriptor getUsbEndpointDescriptor() {
        return null;
    }

    @Override
    public byte getDirection() {
        return direction;
    }

    @Override
    public byte getType() {
        return 0;
    }

    @Override
    public UsbPipe getUsbPipe() {
        return pipe;
    }
}
