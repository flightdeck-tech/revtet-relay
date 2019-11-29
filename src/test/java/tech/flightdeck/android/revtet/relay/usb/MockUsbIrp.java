package tech.flightdeck.android.revtet.relay.usb;

import javax.usb.UsbException;
import javax.usb.UsbIrp;
import java.util.Arrays;
import java.util.BitSet;

public class MockUsbIrp implements UsbIrp {
    private byte[] data;

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public int getActualLength() {
        return 0;
    }

    @Override
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void setData(byte[] data, int offset, int length) {
        this.data = Arrays.copyOfRange(data, offset, offset + length);
    }

    @Override
    public void setOffset(int offset) {

    }

    @Override
    public void setLength(int length) {

    }

    @Override
    public void setActualLength(int length) {

    }

    @Override
    public boolean isUsbException() {
        return false;
    }

    @Override
    public UsbException getUsbException() {
        return null;
    }

    @Override
    public void setUsbException(UsbException usbException) {

    }

    @Override
    public boolean getAcceptShortPacket() {
        return false;
    }

    @Override
    public void setAcceptShortPacket(boolean accept) {

    }

    @Override
    public boolean isComplete() {
        return false;
    }

    @Override
    public void setComplete(boolean complete) {

    }

    @Override
    public void complete() {

    }

    @Override
    public void waitUntilComplete() {

    }

    @Override
    public void waitUntilComplete(long timeout) {

    }
}
