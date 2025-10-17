package com.cufufy.amp.plugin.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PluginSettings {
    private boolean forceMod;
    private boolean nagMissingMod;
    private String nagMessage;
    private String nagLinkText;
    private String nagLinkUrl;
    private List<String> acceptedLoaders = new ArrayList<>();
    private String modpackName;
    private String automodpackVersion;
    private String minecraftVersion;
    private final HostSettings hostSettings = new HostSettings();

    public boolean forceMod() {
        return forceMod;
    }

    public void setForceMod(boolean forceMod) {
        this.forceMod = forceMod;
    }

    public boolean nagMissingMod() {
        return nagMissingMod;
    }

    public void setNagMissingMod(boolean nagMissingMod) {
        this.nagMissingMod = nagMissingMod;
    }

    public String nagMessage() {
        return nagMessage;
    }

    public void setNagMessage(String nagMessage) {
        this.nagMessage = nagMessage;
    }

    public String nagLinkText() {
        return nagLinkText;
    }

    public void setNagLinkText(String nagLinkText) {
        this.nagLinkText = nagLinkText;
    }

    public String nagLinkUrl() {
        return nagLinkUrl;
    }

    public void setNagLinkUrl(String nagLinkUrl) {
        this.nagLinkUrl = nagLinkUrl;
    }

    public List<String> acceptedLoaders() {
        return acceptedLoaders;
    }

    public void setAcceptedLoaders(List<String> acceptedLoaders) {
        this.acceptedLoaders = new ArrayList<>(Objects.requireNonNullElseGet(acceptedLoaders, List::of));
    }

    public String modpackName() {
        return modpackName;
    }

    public void setModpackName(String modpackName) {
        this.modpackName = modpackName;
    }

    public String automodpackVersion() {
        return automodpackVersion;
    }

    public void setAutomodpackVersion(String automodpackVersion) {
        this.automodpackVersion = automodpackVersion;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public HostSettings host() {
        return hostSettings;
    }

    public static final class HostSettings {
        private String bindAddress = "";
        private int bindPort = -1;
        private String addressToSend = "";
        private int portToSend = -1;
        private boolean disableInternalTls;
        private int bandwidthLimit;

        public String bindAddress() {
            return bindAddress;
        }

        public void setBindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
        }

        public int bindPort() {
            return bindPort;
        }

        public void setBindPort(int bindPort) {
            this.bindPort = bindPort;
        }

        public String addressToSend() {
            return addressToSend;
        }

        public void setAddressToSend(String addressToSend) {
            this.addressToSend = addressToSend;
        }

        public int portToSend() {
            return portToSend;
        }

        public void setPortToSend(int portToSend) {
            this.portToSend = portToSend;
        }

        public boolean disableInternalTls() {
            return disableInternalTls;
        }

        public void setDisableInternalTls(boolean disableInternalTls) {
            this.disableInternalTls = disableInternalTls;
        }

        public int bandwidthLimit() {
            return bandwidthLimit;
        }

        public void setBandwidthLimit(int bandwidthLimit) {
            this.bandwidthLimit = bandwidthLimit;
        }
    }
}
