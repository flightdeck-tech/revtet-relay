package tech.flightdeck.android.revtet.relay.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public final class Net {
    private Net() {
        // not instantiable
    }

    public static InetAddress[] toInetAddresses(String... addresses) {
        InetAddress[] result = new InetAddress[addresses.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = toInetAddress(addresses[i]);
        }
        return result;
    }

    public static InetAddress toInetAddress(String address) {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static InetAddress toInetAddress(byte[] raw) {
        try {
            return InetAddress.getByAddress(raw);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public static InetAddress toInetAddress(int ipAddr) {
        byte[] ip = {
                (byte) (ipAddr >>> 24),
                (byte) ((ipAddr >> 16) & 0xff),
                (byte) ((ipAddr >> 8) & 0xff),
                (byte) (ipAddr & 0xff)
        };
        return toInetAddress(ip);
    }

    public static String toString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    public static String toString(int ip, short port) {
        return toString(new InetSocketAddress(Net.toInetAddress(ip), Short.toUnsignedInt(port)));
    }
}
