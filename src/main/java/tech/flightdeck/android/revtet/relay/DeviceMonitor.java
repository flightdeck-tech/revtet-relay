package tech.flightdeck.android.revtet.relay;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.EndianUtils;
import tech.flightdeck.android.revtet.relay.entity.Accessory;
import tech.flightdeck.android.revtet.relay.network.SelectionHandler;
import tech.flightdeck.android.revtet.relay.network.TCPConnection;
import tech.flightdeck.android.revtet.relay.network.UDPConnection;

import javax.usb.*;
import javax.usb.event.*;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DeviceMonitor implements UsbServicesListener {
    private static final short[] ANDROID_VIDS = { 0x18D1, 0x0E8D, 0x2717 };
    public static final short ACCESSORY_VID = 0x18D1;
    public static final short ACCESSORY_PID = 0x2D00;
    public static final short ACCESSORY_ADB_PID = 0x2D01;

    private static final byte AOA_GET_PROTOCOL = 51;
    private static final byte AOA_SEND_IDENT = 52;
    private static final byte AOA_START_ACCESSORY = 53;
    private static final short AOA_STRING_MAN_ID = 0;
    private static final short AOA_STRING_MOD_ID = 1;
    private static final short AOA_STRING_VER_ID = 3;
    private static final short AOA_STRING_SER_ID = 5;

    private static final int CLEANING_INTERVAL = 60 * 1000;

    private Map<UsbDevice, Accessory> accessoryMap = new ConcurrentHashMap<>();

    private Selector selector;
    private Thread selectorThread;

    static {
        Arrays.sort(ANDROID_VIDS);
    }

    public DeviceMonitor() throws UsbException, IOException {
        UsbServices services = UsbHostManager.getUsbServices();
        services.addUsbServicesListener(this);
        selector = Selector.open();
        selectorThread = new Thread(() -> {
            try {
                long nextCleaningDeadline = System.currentTimeMillis() + Math.min(UDPConnection.IDLE_TIMEOUT, TCPConnection.IDLE_TIMEOUT);
                while (true) {
                    accessoryMap.forEach(((device, accessory) -> {
                        accessory.processReceive();
                        accessory.processSend();
                    }));
                    selector.selectNow();
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();

                    long now = System.currentTimeMillis();
                    if (now >= nextCleaningDeadline) {
                        cleanUp();
                        nextCleaningDeadline = now + CLEANING_INTERVAL;
                    }
                    for (SelectionKey selectedKey : selectedKeys) {
                        SelectionHandler selectionHandler = (SelectionHandler)selectedKey.attachment();
                        selectionHandler.onReady(selectedKey);
                    }
                    selectedKeys.clear();
                }
            } catch (IOException e) {
                log.error("Error in selector thread.", e);
            }
        }, "Selector");
        selectorThread.start();
    }

    private void cleanUp() {
        accessoryMap.forEach(((device, accessory) -> {
            accessory.cleanUp();
        }));
    }

    public void usbDeviceAttached(UsbServicesEvent usbServicesEvent) {
        UsbDevice device = usbServicesEvent.getUsbDevice();
        log.info("USB device attached. {}", usbServicesEvent.getUsbDevice());
        try {
            handleDevice(device);
        } catch (UsbException e) {
            log.error("Failed to handle usb device.{}", device, e);
        }
    }

    public void usbDeviceDetached(UsbServicesEvent usbServicesEvent) {
        UsbDevice device = usbServicesEvent.getUsbDevice();
        log.info("USB device detached. {}", device);
        if (isAccessory(device)) {
            accessoryMap.remove(device);
        }
    }

    private void handleDevice(UsbDevice device) throws UsbException {
        if (isAndroid(device)) {
            if (isAccessory(device)) {
                Accessory accessory = new Accessory(device, selector);
                accessoryMap.put(device, accessory);
            } else {
                switchToAccessory(device);
            }
        }
    }

    private boolean isAndroid(UsbDevice device) {
        return Arrays.binarySearch(ANDROID_VIDS, device.getUsbDeviceDescriptor().idVendor()) >= 0;
    }

    private boolean isAccessory(UsbDevice device) {
        UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
        return desc.idVendor() == ACCESSORY_VID && (desc.idProduct() == ACCESSORY_PID || desc.idProduct() == ACCESSORY_ADB_PID);
    }

    private void switchToAccessory(UsbDevice device) throws UsbException {
        UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
        UsbControlIrp irp = device.createUsbControlIrp(
                (byte)(UsbConst.REQUESTTYPE_DIRECTION_IN | UsbConst.REQUESTTYPE_TYPE_VENDOR),
                AOA_GET_PROTOCOL, (short)0, (short) 0
        );
        irp.setData(new byte[2]);
        device.syncSubmit(irp);
        log.info("Device {} supports AOA {}.0", device, EndianUtils.readSwappedShort(irp.getData(), 0));
        irp = device.createUsbControlIrp(
                (byte)(UsbConst.REQUESTTYPE_DIRECTION_OUT | UsbConst.REQUESTTYPE_TYPE_VENDOR),
                AOA_SEND_IDENT, (short)0, AOA_STRING_MAN_ID
        );
        irp.setData("FlightDeck".getBytes());
        device.syncSubmit(irp);
        irp = device.createUsbControlIrp(
                (byte)(UsbConst.REQUESTTYPE_DIRECTION_OUT | UsbConst.REQUESTTYPE_TYPE_VENDOR),
                AOA_SEND_IDENT, (short)0, AOA_STRING_MOD_ID
        );
        irp.setData("Revtet".getBytes());
        device.syncSubmit(irp);
        irp = device.createUsbControlIrp(
                (byte)(UsbConst.REQUESTTYPE_DIRECTION_OUT | UsbConst.REQUESTTYPE_TYPE_VENDOR),
                AOA_SEND_IDENT, (short)0, AOA_STRING_VER_ID
        );
        irp.setData("1.0".getBytes());
        device.syncSubmit(irp);
        irp = device.createUsbControlIrp(
                (byte)(UsbConst.REQUESTTYPE_DIRECTION_OUT | UsbConst.REQUESTTYPE_TYPE_VENDOR),
                AOA_SEND_IDENT, (short)0, AOA_STRING_SER_ID
        );
        irp.setData(String.format("%s%s", desc.bcdUSB(), desc.bcdDevice()).getBytes());
        device.syncSubmit(irp);

        log.info("Turning device {} in accessory mode.", device);
        irp = device.createUsbControlIrp(
                (byte)(UsbConst.REQUESTTYPE_DIRECTION_OUT | UsbConst.REQUESTTYPE_TYPE_VENDOR),
                AOA_START_ACCESSORY, (short)0, (short)0
        );
        device.syncSubmit(irp);
    }
}
