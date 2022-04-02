package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class EphemeralBasicFileAttributeView implements BasicFileAttributeView {

  @Override
  public String name () {

    return null;
  }

  @Override
  public BasicFileAttributes readAttributes () throws IOException {

    return null;
  }

  @Override
  public void setTimes (FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {

  }
}
