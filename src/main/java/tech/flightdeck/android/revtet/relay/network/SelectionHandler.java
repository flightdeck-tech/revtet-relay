package tech.flightdeck.android.revtet.relay.network;

import java.nio.channels.SelectionKey;

public interface SelectionHandler {
    void onReady(SelectionKey selectionKey);
}
