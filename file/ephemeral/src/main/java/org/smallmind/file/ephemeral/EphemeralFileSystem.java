package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import org.smallmind.nutsnbolts.util.SingleItemIterable;

public class EphemeralFileSystem extends FileSystem {

  private static final EphemeralPath ROOT_PATH = new EphemeralPath("/");
  private static final String SEPARATOR = "/";
  private final EphemeralFileStore fileStore = new EphemeralFileStore();
  private final EphemeralFileSystemProvider provider;
  private volatile boolean closed;

  public EphemeralFileSystem (EphemeralFileSystemProvider provider) {

    this.provider = provider;
  }

  @Override
  public FileSystemProvider provider () {

    return provider;
  }

  @Override
  public void close () {

    closed = true;
  }

  @Override
  public boolean isOpen () {

    return !closed;
  }

  @Override
  public boolean isReadOnly () {

    return false;
  }

  @Override
  public String getSeparator () {

    return SEPARATOR;
  }

  @Override
  public Iterable<Path> getRootDirectories () {

    return ROOT_PATH;
  }

  @Override
  public Iterable<FileStore> getFileStores () {

    return new SingleItemIterable<>(fileStore);
  }

  @Override
  public Set<String> supportedFileAttributeViews () {

    return null;
  }

  @Override
  public Path getPath (String first, String... more) {

    return null;
  }

  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    return null;
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return null;
  }

  @Override
  public WatchService newWatchService () throws IOException {

    return null;
  }
}
