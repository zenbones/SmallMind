package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class EphemeralPath implements Path {

  protected static final char SEPARATOR = '/';

  @Override
  public FileSystem getFileSystem () {

    return null;
  }

  @Override
  public boolean isAbsolute () {

    return false;
  }

  @Override
  public Path getRoot () {

    return null;
  }

  @Override
  public Path getFileName () {

    return null;
  }

  @Override
  public Path getParent () {

    return null;
  }

  @Override
  public int getNameCount () {

    return 0;
  }

  @Override
  public Path getName (int index) {

    return null;
  }

  @Override
  public Path subpath (int beginIndex, int endIndex) {

    return null;
  }

  @Override
  public boolean startsWith (Path other) {

    return false;
  }

  @Override
  public boolean endsWith (Path other) {

    return false;
  }

  @Override
  public Path normalize () {

    return null;
  }

  @Override
  public Path resolve (Path other) {

    return null;
  }

  @Override
  public Path relativize (Path other) {

    return null;
  }

  @Override
  public URI toUri () {

    return null;
  }

  @Override
  public Path toAbsolutePath () {

    return null;
  }

  @Override
  public Path toRealPath (LinkOption... options) throws IOException {

    return null;
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {

    return null;
  }

  @Override
  public int compareTo (Path other) {

    return 0;
  }
}
