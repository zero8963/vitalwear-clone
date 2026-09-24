package com.github.cfogrady.vb.dim.sprite;

import com.github.cfogrady.vb.dim.util.ByteOffsetInputStream;
import java.io.IOException;

public class SpritePackageInputStream implements ByteOffsetInputStream {
    private final ByteOffsetInputStream generalInputStream;
    private final SpriteChecksumBuilder spriteChecksumBuilder;
    private int location = 0;

    public SpritePackageInputStream(ByteOffsetInputStream generalInputStream, SpriteChecksumBuilder spriteChecksumBuilder) {
        this.generalInputStream = generalInputStream;
        this.spriteChecksumBuilder = spriteChecksumBuilder;
    }

    @Override
    public byte[] readNBytes(int n) throws IOException {
        byte[] bytes = generalInputStream.readNBytes(n);
        spriteChecksumBuilder.addBytes(bytes, location);
        location += n;
        return bytes;
    }

    @Override
    public byte[] readToOffset(int offset) throws IOException {
        return readNBytes(offset - location);
    }

    public java.util.ArrayList getSpriteChecksums() {
        return spriteChecksumBuilder.getChecksums();
    }
}
