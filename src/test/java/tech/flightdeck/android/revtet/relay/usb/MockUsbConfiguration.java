package tech.flightdeck.android.revtet.relay.usb;

import javax.usb.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MockUsbConfiguration implements UsbConfiguration {
    private List<UsbInterface> interfaces = new ArrayList<>();

    public MockUsbConfiguration() {
        this.interfaces.add(new MockUsbInterface());
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public List getUsbInterfaces() {
        return interfaces;
    }

    @Override
    public UsbInterface getUsbInterface(byte number) {
        return interfaces.get(0);
    }

    @Override
    public boolean containsUsbInterface(byte number) {
        return true;
    }

    @Override
    public UsbDevice getUsbDevice() {
        return null;
    }

    @Override
    public UsbConfigurationDescriptor getUsbConfigurationDescriptor() {
        return null;
    }

    @Override
    public String getConfigurationString() throws UsbException, UnsupportedEncodingException, UsbDisconnectedException {
        return null;
    }
}
