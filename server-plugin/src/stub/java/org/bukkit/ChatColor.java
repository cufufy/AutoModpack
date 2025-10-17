package org.bukkit;

public enum ChatColor {
    RED,
    GREEN,
    YELLOW,
    WHITE,
    RESET;

    @Override
    public String toString() {
        return name();
    }
}
