package pl.skidam.automodpack.plugin.core;

import java.net.SocketAddress;

import pl.skidam.automodpack_core.loader.GameCallService;

public final class PluginGameCall implements GameCallService {
    @Override
    public boolean isPlayerAuthorized(SocketAddress address, String id) {
        return true;
    }
}
