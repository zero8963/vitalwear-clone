package com.github.cfogrady.vb.dim.util;

import com.example.vitalwearclonev1.common.InputStreamSafe;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamWithNot implements ByteOffsetInputStream {
    private final InputStream inputStream;
    private final DIMChecksumBuilder checksumBuilder;
    private int location = 0;

    public InputStreamWithNot(InputStream inputStream, DIMChecksumBuilder checksumBuilder) {
        this.inputStream = inputStream;
        this.checksumBuilder = checksumBuilder;
    }

    public static InputStreamWithNot wrap(InputStream inputStream, DIMChecksumBuilder checksumBuilder) {
        return new InputStreamWithNot(inputStream, checksumBuilder);
    }

    @Override
    public byte[] readNBytes(int n) throws IOException {
        byte[] bytes = InputStreamSafe.INSTANCE.readNBytes(inputStream, n);
        bytes = ByteUtils.applyNotOperation(bytes);
        checksumBuilder.addBytes(bytes, location);
        location += n;
        return bytes;
    }

    @Override
    public byte[] readToOffset(int offset) throws IOException {
        int amountToRead = offset - location;
        if (amountToRead < 0) {
            throw new IllegalArgumentException("Cannot read to an offset that has already been passed. Current location: " + location + " Offset: " + offset);
        }
        return readNBytes(amountToRead);
    }

    public int getLocation() {
        return location;
    }

    public int getChecksum() {
        return checksumBuilder.getCheckSum();
    }
}
