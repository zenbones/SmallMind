package org.smallmind.file.ephemeral;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class EphemeralSeekableByteChannel implements SeekableByteChannel {

  @Override
  public int read (ByteBuffer dst) {

    return 0;
  }

  @Override
  public int write (ByteBuffer src) {

    return 0;
  }

  @Override
  public long position () {

    return 0;
  }

  @Override
  public SeekableByteChannel position (long newPosition) {

    return null;
  }

  @Override
  public long size () {

    return 0;
  }

  @Override
  public SeekableByteChannel truncate (long size) {

    return null;
  }

  @Override
  public boolean isOpen () {

    return false;
  }

  @Override
  public void close () {

  }
}
