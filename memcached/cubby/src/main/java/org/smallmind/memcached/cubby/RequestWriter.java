package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RequestWriter {

  private final byte[] buffer;
  private int index = 0;

  public RequestWriter (byte[] buffer) {

    this.buffer = buffer;
  }

  public int write (SocketChannel socketChannel, ByteBuffer byteBuffer)
    throws IOException {

    int bytesWritten;

    byteBuffer.clear();
    byteBuffer.put(buffer, index, Math.min(byteBuffer.remaining(), buffer.length - index));

    byteBuffer.flip();
    index += (bytesWritten = socketChannel.write(byteBuffer));

    return (index == buffer.length) ? bytesWritten : -bytesWritten;
  }
}
