package tech.flightdeck.android.revtet.relay.usb;

import tech.flightdeck.android.revtet.relay.DeviceMonitor;
import tech.flightdeck.android.revtet.relay.entity.Accessory;

import javax.usb.UsbDeviceDescriptor;

public class MockAccessoryUsbDeviceDescriptor implements UsbDeviceDescriptor {
    @Override
    public short bcdUSB() {
        return 0;
    }

    @Override
    public byte bDeviceClass() {
        return 0;
    }

    @Override
    public byte bDeviceSubClass() {
        return 0;
    }

    @Override
    public byte bDeviceProtocol() {
        return 0;
    }

    @Override
    public byte bMaxPacketSize0() {
        return 0;
    }

    @Override
    public short idVendor() {
        return DeviceMonitor.ACCESSORY_VID;
    }

    @Override
    public short idProduct() {
        return DeviceMonitor.ACCESSORY_PID;
    }

    @Override
    public short bcdDevice() {
        return 0;
    }

    @Override
    public byte iManufacturer() {
        return 0;
    }

    @Override
    public byte iProduct() {
        return 0;
    }

    @Override
    public byte iSerialNumber() {
        return 0;
    }

    @Override
    public byte bNumConfigurations() {
        return 0;
    }

    @Override
    public byte bLength() {
        return 0;
    }

    @Override
    public byte bDescriptorType() {
        return 0;
    }
}
