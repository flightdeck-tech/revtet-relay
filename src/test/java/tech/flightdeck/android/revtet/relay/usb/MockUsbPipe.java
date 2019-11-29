package tech.flightdeck.android.revtet.relay.usb;

import javax.usb.*;
import javax.usb.event.UsbPipeListener;
import java.util.List;

public class MockUsbPipe implements UsbPipe {
    private boolean open;

    @Override
    public void open() throws UsbException, UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException {
        open = true;
    }

    @Override
    public void close() throws UsbException, UsbNotActiveException, UsbNotOpenException, UsbDisconnectedException {
        open = false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public UsbEndpoint getUsbEndpoint() {
        return null;
    }

    @Override
    public int syncSubmit(byte[] data) throws UsbException, UsbNotActiveException, UsbNotOpenException, IllegalArgumentException, UsbDisconnectedException {
        return 0;
    }

    @Override
    public UsbIrp asyncSubmit(byte[] data) throws UsbException, UsbNotActiveException, UsbNotOpenException, IllegalArgumentException, UsbDisconnectedException {
        return null;
    }

    @Override
    public void syncSubmit(UsbIrp irp) throws UsbException, UsbNotActiveException, UsbNotOpenException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void asyncSubmit(UsbIrp irp) throws UsbException, UsbNotActiveException, UsbNotOpenException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void syncSubmit(List list) throws UsbException, UsbNotActiveException, UsbNotOpenException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void asyncSubmit(List list) throws UsbException, UsbNotActiveException, UsbNotOpenException, IllegalArgumentException, UsbDisconnectedException {

    }

    @Override
    public void abortAllSubmissions() throws UsbNotActiveException, UsbNotOpenException, UsbDisconnectedException {

    }

    @Override
    public UsbIrp createUsbIrp() {
        return new MockUsbIrp();
    }

    @Override
    public UsbControlIrp createUsbControlIrp(byte bmRequestType, byte bRequest, short wValue, short wIndex) {
        return null;
    }

    @Override
    public void addUsbPipeListener(UsbPipeListener listener) {

    }

    @Override
    public void removeUsbPipeListener(UsbPipeListener listener) {

    }
}
