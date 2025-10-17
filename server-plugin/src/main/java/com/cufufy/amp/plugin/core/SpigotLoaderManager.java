package com.cufufy.amp.plugin.core;

import java.util.Collection;
import java.util.List;

import com.cufufy.amp.core.loader.LoaderManagerService;
import com.cufufy.amp.core.utils.FileInspection;

public final class SpigotLoaderManager implements LoaderManagerService {
    private final ModPlatform platform;
    private final String loaderVersion;

    public SpigotLoaderManager(ModPlatform platform, String loaderVersion) {
        this.platform = platform;
        this.loaderVersion = loaderVersion;
    }

    @Override
    public ModPlatform getPlatformType() {
        return platform;
    }

    @Override
    public Collection<FileInspection.Mod> getModList() {
        return List.of();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return false;
    }

    @Override
    public String getLoaderVersion() {
        return loaderVersion;
    }

    @Override
    public EnvironmentType getEnvironmentType() {
        return EnvironmentType.SERVER;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return false;
    }

    @Override
    public String getModVersion(String modId) {
        return "unknown";
    }
}
