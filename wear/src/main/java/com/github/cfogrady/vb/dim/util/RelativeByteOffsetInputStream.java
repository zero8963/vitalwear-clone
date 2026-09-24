package com.github.cfogrady.vb.dim.util;

import com.example.vitalwearclonev1.common.InputStreamSafe;
import java.io.IOException;
import java.io.InputStream;

public class RelativeByteOffsetInputStream implements ByteOffsetInputStream {
    private final InputStream inputStream;
    private final ByteOffsetInputStream byteOffsetInputStream;
    private int location = 0;

    public RelativeByteOffsetInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.byteOffsetInputStream = null;
    }

    public RelativeByteOffsetInputStream(ByteOffsetInputStream byteOffsetInputStream) {
        this.inputStream = null;
        this.byteOffsetInputStream = byteOffsetInputStream;
    }

    @Override
    public byte[] readNBytes(int n) throws IOException {
        byte[] bytes;
        if (byteOffsetInputStream != null) {
            bytes = byteOffsetInputStream.readNBytes(n);
        } else {
            bytes = InputStreamSafe.INSTANCE.readNBytes(inputStream, n);
        }
        location += n;
        return bytes;
    }

    @Override
    public byte[] readToOffset(int offset) throws IOException {
        return this.readNBytes(offset - location);
    }
}
