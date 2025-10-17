package com.cufufy.amp.core.loader;

import com.cufufy.amp.core.utils.FileInspection;

import java.nio.file.Path;
import java.util.List;

public class NullModpackLoader implements ModpackLoaderService {

    @Override
    public void loadModpack(List<Path> modpackMods) {
        throw new AssertionError("Loader class not found");
    }

    @Override
    public List<FileInspection.Mod> getModpackNestedConflicts(Path modpackDir) {
        throw new AssertionError("Loader class not found");
    }
}
