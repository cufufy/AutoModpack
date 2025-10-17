package com.cufufy.amp.core.loader;

import com.cufufy.amp.core.utils.FileInspection;

import java.nio.file.Path;
import java.util.List;

public interface ModpackLoaderService {
    void loadModpack(List<Path> modpackMods);
    List<FileInspection.Mod> getModpackNestedConflicts(Path modpackDir); // Returns list of mods from the modpack Dir that are conflicting with the mods from standard mods dir
}
