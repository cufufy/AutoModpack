package org.bukkit.entity;

public interface Player extends OfflinePlayer {
    String getName();

    void sendMessage(String message);

    void kickPlayer(String message);

    java.util.UUID getUniqueId();
}
