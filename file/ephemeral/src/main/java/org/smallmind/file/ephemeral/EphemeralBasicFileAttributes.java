package org.smallmind.file.ephemeral;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import org.smallmind.file.ephemeral.heap.HeapNode;
import org.smallmind.file.ephemeral.heap.HeapNodeType;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class EphemeralBasicFileAttributes implements BasicFileAttributes {

  private final String id = SnowflakeId.newInstance().generateHexEncoding();
  private final HeapNode heapNode;
  private final FileTime lastModifiedTime;
  private final FileTime lastAccessTime;
  private final FileTime creationTime;
  private final boolean regularFile;
  private final boolean directory;
  private final boolean symbolicLink;
  private final boolean other;

  public EphemeralBasicFileAttributes (HeapNode heapNode) {

    this.heapNode = heapNode;

    creationTime = FileTime.fromMillis(System.currentTimeMillis());
    lastModifiedTime = FileTime.fromMillis(System.currentTimeMillis());
    lastAccessTime = FileTime.fromMillis(System.currentTimeMillis());

    directory = HeapNodeType.DIRECTORY.equals(heapNode.getType());
    regularFile = HeapNodeType.FILE.equals(heapNode.getType());
    symbolicLink = false;
    other = false;
  }

  @Override
  public FileTime lastModifiedTime () {

    return lastModifiedTime;
  }

  @Override
  public FileTime lastAccessTime () {

    return lastAccessTime;
  }

  @Override
  public FileTime creationTime () {

    return creationTime;
  }

  @Override
  public boolean isRegularFile () {

    return regularFile;
  }

  @Override
  public boolean isDirectory () {

    return directory;
  }

  @Override
  public boolean isSymbolicLink () {

    return symbolicLink;
  }

  @Override
  public boolean isOther () {

    return other;
  }

  @Override
  public long size () {

    return heapNode.size();
  }

  @Override
  public Object fileKey () {

    return id;
  }
}
