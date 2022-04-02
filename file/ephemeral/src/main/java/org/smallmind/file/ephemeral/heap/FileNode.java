package org.smallmind.file.ephemeral.heap;

import java.io.ByteArrayOutputStream;

public class FileNode extends HeapNode {

  private ByteArrayOutputStream buffer;

  @Override
  public long size () {

    return buffer.size();
  }
}
