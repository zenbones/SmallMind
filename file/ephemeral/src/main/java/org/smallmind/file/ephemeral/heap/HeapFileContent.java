/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.file.ephemeral.heap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.util.SnowflakeId;

/**
 * Random-access byte storage for a single ephemeral file.
 *
 * <p>Content is held as a list of fixed-size segments, grown on demand, so that a large file does
 * not require one contiguous array. Every operation addresses the content by absolute position:
 * this class holds <em>no</em> cursor of its own.
 *
 * <p>That is the essential property. File content was previously read and written through a
 * {@code ByteArrayIOStream}, which owns a single input position and a single output position. Since
 * every channel opened on a file wrapped the same underlying buffer, two channels could not hold
 * independent positions, a channel could not be opened for reading <em>and</em> writing, and
 * concurrent appends overwrote one another. Keeping position state in the channel and addressing
 * content absolutely removes all three problems at once.
 *
 * <p>Instances are thread-safe. Bytes at or beyond {@link #size()} are always zero, which is what
 * lets a write past the end of the file read back as a zero-filled gap.
 *
 * <p>A single instance may be shared by more than one {@link FileNode}, which is how hard links are
 * represented.
 */
public class HeapFileContent {

  private final HeapSpaceGovernor spaceGovernor;
  private final ArrayList<byte[]> segmentList = new ArrayList<>();
  private final AtomicReference<String> fileKeyRef = new AtomicReference<>();
  private final int blockSize;
  private long size;
  private int linkCount = 1;

  /**
   * Creates empty content.
   *
   * @param spaceGovernor the accountant consulted before the content grows
   * @param blockSize     the size of each allocated segment; must be &gt; 0
   */
  public HeapFileContent (HeapSpaceGovernor spaceGovernor, int blockSize) {

    this.spaceGovernor = spaceGovernor;
    this.blockSize = blockSize;
  }

  /**
   * Creates a deep copy of existing content, which must be accounted for as a fresh allocation.
   *
   * @param content the content to copy
   * @throws IOException if the copy would exceed the capacity of the store
   */
  public HeapFileContent (HeapFileContent content)
    throws IOException {

    this(content.spaceGovernor, content.blockSize);

    byte[] transferBuffer = new byte[content.blockSize];
    long position = 0;
    int bytesRead;

    // copied a segment at a time so that content larger than an int can be duplicated
    while ((bytesRead = content.read(position, transferBuffer, 0, transferBuffer.length)) > 0) {
      write(position, transferBuffer, 0, bytesRead);
      position += bytesRead;
    }
  }

  /**
   * Returns the number of bytes in this content.
   *
   * @return the size in bytes; never negative
   */
  public synchronized long size () {

    return size;
  }

  /**
   * Returns the identity of this content, which every {@link FileNode} referring to it shares.
   *
   * <p>The identifier is generated on first use rather than at construction, because generating it
   * loads {@code SecureRandom}, and this class can be instantiated while
   * {@link java.nio.file.FileSystems} is still resolving its default provider.
   *
   * @return a hex-encoded identifier, stable for the life of this content
   */
  public String getFileKey () {

    String fileKey;

    if ((fileKey = fileKeyRef.get()) == null) {
      fileKeyRef.compareAndSet(null, SnowflakeId.newInstance().generateHexEncoding());
      fileKey = fileKeyRef.get();
    }

    return fileKey;
  }

  /**
   * Reads up to {@code length} bytes starting at an absolute position.
   *
   * @param position the absolute position to read from; must not be negative
   * @param buffer   the array to read into
   * @param offset   the offset within {@code buffer} at which to start writing
   * @param length   the maximum number of bytes to read
   * @return the number of bytes read, or {@code -1} if {@code position} is at or beyond the end of
   * the content
   */
  public synchronized int read (long position, byte[] buffer, int offset, int length) {

    if (position < 0) {
      throw new IllegalArgumentException("Negative position");
    } else if (position >= size) {

      return -1;
    } else {

      int remaining = (int)Math.min(length, size - position);
      int copied = 0;

      while (copied < remaining) {

        int segmentIndex = (int)((position + copied) / blockSize);
        int byteIndex = (int)((position + copied) % blockSize);
        int chunk = Math.min(remaining - copied, blockSize - byteIndex);

        System.arraycopy(segmentList.get(segmentIndex), byteIndex, buffer, offset + copied, chunk);
        copied += chunk;
      }

      return copied;
    }
  }

  /**
   * Writes {@code length} bytes at an absolute position, extending the content as required. A
   * position beyond the current end leaves the intervening bytes zero.
   *
   * @param position the absolute position to write at; must not be negative
   * @param buffer   the array to write from
   * @param offset   the offset within {@code buffer} at which to start reading
   * @param length   the number of bytes to write
   * @throws IOException if the write would exceed the capacity of the store
   */
  public synchronized void write (long position, byte[] buffer, int offset, int length)
    throws IOException {

    if (position < 0) {
      throw new IllegalArgumentException("Negative position");
    } else if (length > 0) {

      long endPosition = position + length;

      if (endPosition > size) {
        spaceGovernor.reserve(endPosition - size);
      }

      // allocate through the segment holding the final byte, leaving any gap zero-filled
      int lastSegmentIndex = (int)((endPosition - 1) / blockSize);

      while (segmentList.size() <= lastSegmentIndex) {
        segmentList.add(new byte[blockSize]);
      }

      int copied = 0;

      while (copied < length) {

        int segmentIndex = (int)((position + copied) / blockSize);
        int byteIndex = (int)((position + copied) % blockSize);
        int chunk = Math.min(length - copied, blockSize - byteIndex);

        System.arraycopy(buffer, offset + copied, segmentList.get(segmentIndex), byteIndex, chunk);
        copied += chunk;
      }

      if (endPosition > size) {
        size = endPosition;
      }
    }
  }

  /**
   * Appends {@code length} bytes to the end of the content, resolving the end position and writing
   * under a single lock acquisition.
   *
   * <p>Atomicity here is the whole point. Resolving the end with {@link #size()} and then calling
   * {@link #write(long, byte[], int, int)} would take the lock twice, letting a second appender
   * resolve the same end position in between and overwrite the first one's bytes.
   *
   * @param buffer the array to write from
   * @param offset the offset within {@code buffer} at which to start reading
   * @param length the number of bytes to write
   * @return the position immediately after the appended bytes
   * @throws IOException if the write would exceed the capacity of the store
   */
  public synchronized long append (byte[] buffer, int offset, int length)
    throws IOException {

    long startPosition = size;

    write(startPosition, buffer, offset, length);

    return startPosition + length;
  }

  /**
   * Shrinks the content to {@code newSize}. A size at or beyond the current size has no effect, as
   * {@link java.nio.channels.SeekableByteChannel#truncate(long)} requires.
   *
   * @param newSize the size to truncate to; must not be negative
   */
  public synchronized void truncate (long newSize) {

    if (newSize < 0) {
      throw new IllegalArgumentException("Negative size");
    } else if (newSize < size) {

      long discarded = size - newSize;
      int retainedSegments = (int)((newSize + blockSize - 1) / blockSize);
      int byteIndex = (int)(newSize % blockSize);

      // zero the discarded tail of the last retained segment so that later growth reads back as a zero-filled gap
      if ((byteIndex != 0) && (retainedSegments <= segmentList.size())) {
        Arrays.fill(segmentList.get(retainedSegments - 1), byteIndex, blockSize, (byte)0);
      }
      while (segmentList.size() > retainedSegments) {
        segmentList.remove(segmentList.size() - 1);
      }

      size = newSize;
      spaceGovernor.release(discarded);
    }
  }

  /**
   * Discards all content, returning its bytes to the store.
   */
  public synchronized void clear () {

    truncate(0);
  }

  /**
   * Records that an additional name now refers to this content.
   *
   * @see #unlink()
   */
  public synchronized void link () {

    linkCount++;
  }

  /**
   * Records that one fewer name refers to this content, discarding the bytes once none do.
   *
   * <p>This is what keeps a hard link honest: deleting one of two names for a file must leave the
   * content reachable through the other, and must not return its bytes to the store.
   */
  public synchronized void unlink () {

    if (--linkCount <= 0) {
      truncate(0);
    }
  }
}
