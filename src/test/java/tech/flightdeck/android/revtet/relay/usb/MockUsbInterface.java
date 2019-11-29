package tech.flightdeck.android.revtet.relay.usb;

import javax.usb.*;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class MockUsbInterface implements UsbInterface {
    private boolean claimed;

    @Override
    public void claim() throws UsbClaimException, UsbException, UsbNotActiveException, UsbDisconnectedException {
        claimed = true;
    }

    @Override
    public void claim(UsbInterfacePolicy policy) throws UsbClaimException, UsbException, UsbNotActiveException, UsbDisconnectedException {
        claimed = true;
    }

    @Override
    public void release() throws UsbClaimException, UsbException, UsbNotActiveException, UsbDisconnectedException {
        claimed = false;
    }

    @Override
    public boolean isClaimed() {
        return claimed;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public int getNumSettings() {
        return 0;
    }

    @Override
    public byte getActiveSettingNumber() throws UsbNotActiveException {
        return 0;
    }

    @Override
    public UsbInterface getActiveSetting() throws UsbNotActiveException {
        return null;
    }

    @Override
    public UsbInterface getSetting(byte number) {
        return null;
    }

    @Override
    public boolean containsSetting(byte number) {
        return false;
    }

    @Override
    public List getSettings() {
        return null;
    }

    @Override
    public List getUsbEndpoints() {
        return null;
    }

    @Override
    public UsbEndpoint getUsbEndpoint(byte address) {
        return null;
    }

    @Override
    public boolean containsUsbEndpoint(byte address) {
        return false;
    }

    @Override
    public UsbConfiguration getUsbConfiguration() {
        return null;
    }

    @Override
    public UsbInterfaceDescriptor getUsbInterfaceDescriptor() {
        return null;
    }

    @Override
    public String getInterfaceString() throws UsbException, UnsupportedEncodingException, UsbDisconnectedException {
        return null;
    }
}
