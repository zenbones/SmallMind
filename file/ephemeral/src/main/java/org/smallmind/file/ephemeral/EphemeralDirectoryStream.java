package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Iterator;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;

public class EphemeralDirectoryStream implements SecureDirectoryStream<Path> {

  private final EphemeralFileSystemProvider provider;
  private final EphemeralPath streamPath;
  private final DirectoryNode directoryNode;
  private final DirectoryStream.Filter<? super Path> filter;
  private boolean closed = false;

  public EphemeralDirectoryStream (EphemeralFileSystemProvider provider, EphemeralPath streamPath, DirectoryNode directoryNode, DirectoryStream.Filter<? super Path> filter) {

    this.provider = provider;
    this.streamPath = streamPath;
    this.directoryNode = directoryNode;
    this.filter = filter;
  }

  @Override
  public synchronized void close () {

    closed = true;
  }

  @Override
  public synchronized Iterator<Path> iterator () {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return directoryNode.iterator(streamPath, filter);
    }
  }

  @Override
  public synchronized SecureDirectoryStream<Path> newDirectoryStream (Path path, LinkOption... options)
    throws NoSuchFileException, NotDirectoryException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.newDirectoryStream(path.isAbsolute() ? path : streamPath.resolve(path), null, options);
    }
  }

  @Override
  public synchronized SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
    throws IOException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

      return provider.newByteChannel(path.isAbsolute() ? path : streamPath.resolve(path), options, attrs);
    }
  }

  @Override
  public synchronized void deleteFile (Path path)
    throws NoSuchFileException, DirectoryNotEmptyException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.delete(path.isAbsolute() ? path : streamPath.resolve(path));
    }
  }

  @Override
  public synchronized void deleteDirectory (Path path)
    throws NoSuchFileException, DirectoryNotEmptyException {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {
      provider.delete(path.isAbsolute() ? path : streamPath.resolve(path));
    }
  }

  @Override
  public synchronized void move (Path src, SecureDirectoryStream<Path> target, Path targetpath) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

    }
  }

  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Class<V> type) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

    }
    return null;
  }

  @Override
  public synchronized <V extends FileAttributeView> V getFileAttributeView (Path path, Class<V> type, LinkOption... options) {

    if (closed) {
      throw new ClosedDirectoryStreamException();
    } else {

    }
    return null;
  }
}
