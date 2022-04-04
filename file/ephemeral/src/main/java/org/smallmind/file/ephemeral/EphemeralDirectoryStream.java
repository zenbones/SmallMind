package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.Set;

public class EphemeralDirectoryStream implements SecureDirectoryStream<Path> {

  @Override
  public Iterator<Path> iterator () {

    return null;
  }

  @Override
  public void close () throws IOException {

  }

  @Override
  public SecureDirectoryStream<Path> newDirectoryStream (Path path, LinkOption... options) throws IOException {

    return null;
  }

  @Override
  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

    return null;
  }

  @Override
  public void deleteFile (Path path) throws IOException {

  }

  @Override
  public void deleteDirectory (Path path) throws IOException {

  }

  @Override
  public void move (Path srcpath, SecureDirectoryStream<Path> targetdir, Path targetpath) throws IOException {

  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Class<V> type) {

    return null;
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    return null;
  }

}
