package org.bukkit.entity;

public interface Player {
    String getName();

    void sendMessage(String message);

    void kickPlayer(String message);

    java.util.UUID getUniqueId();
}
