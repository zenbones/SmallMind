package org.smallmind.file.ephemeral;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class EphemeralBasicFileAttributes implements BasicFileAttributes {

  @Override
  public FileTime lastModifiedTime () {

    return null;
  }

  @Override
  public FileTime lastAccessTime () {

    return null;
  }

  @Override
  public FileTime creationTime () {

    return null;
  }

  @Override
  public boolean isRegularFile () {

    return false;
  }

  @Override
  public boolean isDirectory () {

    return false;
  }

  @Override
  public boolean isSymbolicLink () {

    return false;
  }

  @Override
  public boolean isOther () {

    return false;
  }

  @Override
  public long size () {

    return 0;
  }

  @Override
  public Object fileKey () {

    return null;
  }
}
