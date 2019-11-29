package tech.flightdeck.android.revtet.relay.usb;

import javax.usb.*;
import javax.usb.event.UsbDeviceListener;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MockUsbDevice implements UsbDevice {
    private List<UsbConfiguration> configurations = new ArrayList<>();
    private UsbDeviceDescriptor descriptor;

    public MockUsbDevice(UsbDeviceDescriptor descriptor) {
        configurations.add(new MockUsbConfiguration());
        this.descriptor = descriptor;
    }

    @Override
    public UsbPort getParentUsbPort() throws UsbDisconnectedException {
        return null;
    }

    @Override
    public boolean isUsbHub() {
        return false;
    }

    @Override
    public String getManufacturerString() throws UsbException, UnsupportedEncodingException, UsbDisconnectedException {
        return null;
    }

    @Override
    public String getSerialNumberString() throws UsbException, UnsupportedEncodingException, UsbDisconnectedException {
        return null;
    }

    @Override
    public String getProductString() throws UsbException, UnsupportedEncodingException, UsbDisconnectedException {
        return null;
    }

    @Override
    public Object getSpeed() {
        return null;
    }

    @Override
    public List getUsbConfigurations() {
        return configurations;
    }

    @Override
    public UsbConfiguration getUsbConfiguration(byte number) {
        return configurations.get(0);
    }

    @Override
    public boolean containsUsbConfiguration(byte number) {
        return true;
    }

    @Override
    public byte getActiveUsbConfigurationNumber() {
        return 0;
    }

    @Override
    public UsbConfiguration getActiveUsbConfiguration() {
        return configurations.get(0);
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public UsbDeviceDescriptor getUsbDeviceDescriptor() {
        return descriptor;
    }

    @Override
    public UsbStringDescriptor getUsbStringDescriptor(byte index) throws UsbException, UsbDisconnectedException {
        return null;
    }

    @Override
    public String getString(byte index) throws UsbException, UnsupportedEncodingException, UsbDisconnectedException {
        return null;
    }

    @Override
    public void syncSubmit(UsbControlIrp irp) throws UsbException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void asyncSubmit(UsbControlIrp irp) throws UsbException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void syncSubmit(List list) throws UsbException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void asyncSubmit(List list) throws UsbException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public UsbControlIrp createUsbControlIrp(byte bmRequestType, byte bRequest, short wValue, short wIndex) {
        return null;
    }

    @Override
    public void addUsbDeviceListener(UsbDeviceListener listener) {

    }

    @Override
    public void removeUsbDeviceListener(UsbDeviceListener listener) {

    }
}
