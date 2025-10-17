package com.cufufy.amp.core.protocol.compression;

import static com.cufufy.amp.core.protocol.NetUtils.COMPRESSION_NONE;

/**
 * None compression codec implementation.
 * Input == Output (no compression).
 */
public class NoneCompression implements CompressionCodec {

    @Override
    public byte[] compress(byte[] input) {
        return input;
    }

    @Override
    public byte[] decompress(byte[] compressed, int originalLength) {
        return compressed;
    }

    @Override
    public byte getCompressionType() {
        return COMPRESSION_NONE;
    }
}
