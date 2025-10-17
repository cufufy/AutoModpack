package com.cufufy.amp.plugin.core;

import java.net.SocketAddress;

import com.cufufy.amp.core.loader.GameCallService;

public final class PluginGameCall implements GameCallService {
    @Override
    public boolean isPlayerAuthorized(SocketAddress address, String id) {
        return true;
    }
}
