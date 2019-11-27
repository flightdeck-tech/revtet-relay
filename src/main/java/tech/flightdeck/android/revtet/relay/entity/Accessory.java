package tech.flightdeck.android.revtet.relay.entity;

import lombok.extern.slf4j.Slf4j;

import javax.usb.UsbDevice;

@Slf4j
public class Accessory {
    public Accessory(UsbDevice device) {
        log.info("Accessory {} initialized.", device);
    }
}
