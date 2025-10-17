package io.netty.channel;

public interface ChannelPipeline {
    ChannelPipeline addBefore(String baseName, String name, Object handler);

    Object get(String name);
}
