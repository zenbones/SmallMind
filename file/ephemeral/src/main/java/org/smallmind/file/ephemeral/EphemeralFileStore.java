package org.smallmind.file.ephemeral;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileStore;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.util.Map;
import java.util.Set;
import org.smallmind.file.ephemeral.heap.DirectoryNode;
import org.smallmind.file.ephemeral.heap.HeapNode;

public class EphemeralFileStore extends FileStore {

  private static final Map<String, Class<? extends FileAttributeView>> SUPPORTED_FILE_VIEW_MAP = Map.of("basic", BasicFileAttributeView.class);
  private final EphemeralFileStoreAttributeView fileStoreAttributeView = new EphemeralFileStoreAttributeView();
  private final HeapNode rootNode = new DirectoryNode();
  private final long capacity;
  private final long blockSize;

  public EphemeralFileStore (long capacity, long blockSize) {

    this.capacity = capacity;
    this.blockSize = blockSize;
  }

  @Override
  public String name () {

    return EphemeralFileStore.class.getSimpleName();
  }

  @Override
  public String type () {

    return name();
  }

  @Override
  public boolean isReadOnly () {

    return false;
  }

  @Override
  public long getTotalSpace () {

    return getUsableSpace();
  }

  @Override
  public long getUsableSpace () {

    return capacity;
  }

  @Override
  public long getUnallocatedSpace () {

    return capacity - rootNode.size();
  }

  @Override
  public boolean supportsFileAttributeView (Class<? extends FileAttributeView> type) {

    return SUPPORTED_FILE_VIEW_MAP.containsValue(type);
  }

  @Override
  public boolean supportsFileAttributeView (String name) {

    return SUPPORTED_FILE_VIEW_MAP.containsKey(name);
  }

  public Set<String> getSupportedFileAttributeViewNames () {

    return SUPPORTED_FILE_VIEW_MAP.keySet();
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView (Class<V> type) {

    return EphemeralFileStoreAttributeView.class.equals(type) ? type.cast(fileStoreAttributeView) : null;
  }

  @Override
  public Object getAttribute (String attribute)
    throws IOException {

    int colonPos;

    if ((colonPos = attribute.indexOf(':')) < 0) {
      throw new IllegalArgumentException(attribute);
    } else {

      if (fileStoreAttributeView.name().equals(attribute.substring(0, colonPos))) {
        try {

          return fileStoreAttributeView.getClass().getDeclaredField(attribute.substring(colonPos + 1)).get(fileStoreAttributeView);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
          throw new IOException(exception);
        }
      } else {

        return null;
      }
    }
  }

  public SeekableByteChannel newByteChannel (Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) {

  }
}
