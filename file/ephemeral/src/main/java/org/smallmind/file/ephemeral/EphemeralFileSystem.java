package org.smallmind.file.ephemeral;

import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;
import java.util.regex.Pattern;
import org.smallmind.nutsnbolts.util.SingleItemIterable;

public class EphemeralFileSystem extends FileSystem {

  private static final EphemeralUserPrincipalLookupService USER_PRINCIPAL_LOOKUP_SERVICE = new EphemeralUserPrincipalLookupService();
  private static final EphemeralPath ROOT_PATH = new EphemeralPath("/");
  private static final String SEPARATOR = String.valueOf(EphemeralPath.getSeparator());
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

  public EphemeralFileStore getFileStore () {

    return fileStore;
  }

  @Override
  public Iterable<FileStore> getFileStores () {

    return new SingleItemIterable<>(fileStore);
  }

  @Override
  public Set<String> supportedFileAttributeViews () {

    return fileStore.getSupportedFileAttributeViewNames();
  }

  @Override
  public Path getPath (String first, String... more) {

    return new EphemeralPath(first, more);
  }

  @Override
  public PathMatcher getPathMatcher (String syntaxAndPattern) {

    int colonPos;

    if ((colonPos = syntaxAndPattern.indexOf(':')) < 0) {
      throw new IllegalArgumentException(syntaxAndPattern);
    } else {

      String syntax;

      switch (syntax = syntaxAndPattern.substring(0, colonPos)) {
        case "glob":

          return new RegexPathMatcher(Glob.toRegexPattern(EphemeralPath.getSeparator(), syntaxAndPattern.substring(colonPos + 1)));
        case "regex":

          return new RegexPathMatcher(Pattern.compile(syntaxAndPattern.substring(colonPos + 1)));
        default:
          throw new UnsupportedOperationException(syntax);
      }
    }
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService () {

    return USER_PRINCIPAL_LOOKUP_SERVICE;
  }

  @Override
  public WatchService newWatchService () {

    return new EphemeralWatchService();
  }
}
