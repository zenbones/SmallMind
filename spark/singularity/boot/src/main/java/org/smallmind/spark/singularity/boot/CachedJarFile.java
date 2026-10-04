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
package org.smallmind.spark.singularity.boot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

/**
 * {@link NestedJarFile} that holds the nested jar's own bytes, used when a bundle is built with {@code useJarCache} set
 * to {@code true} (the default), so that subsequent reads of the same bundled library do not need to reopen the
 * enclosing archive. Construction reads only the jar's central directory and records where
 * each entry's data lies; reading an entry inflates just that entry (or copies it, if it was stored uncompressed).
 * Nothing is decompressed ahead of use and nothing is compressed again.
 * <p>The reader understands what jar-producing tools write: stored and deflated entries, entries followed by data
 * descriptors, ZIP64 archives (more than 65,535 entries, or sizes and offsets beyond 4 GB), and archives with bytes
 * prepended to the zip data. Encrypted entries and compression methods other than stored and deflated are rejected
 * when read. Entry names are decoded as UTF-8, as jar tools write them. Signed jars are not verified; their
 * certificates were never carried into class code sources.
 */
public class CachedJarFile implements NestedJarFile {

  private static final int LOCAL_HEADER_SIGNATURE = 0x04034b50;
  private static final int CENTRAL_HEADER_SIGNATURE = 0x02014b50;
  private static final int END_SIGNATURE = 0x06054b50;
  private static final int ZIP64_END_LOCATOR_SIGNATURE = 0x07064b50;
  private static final int ZIP64_END_SIGNATURE = 0x06064b50;
  private static final int ZIP64_EXTRA_FIELD_ID = 0x0001;
  private static final int END_LENGTH = 22;
  private static final int ZIP64_END_LOCATOR_LENGTH = 20;
  private static final int MAX_COMMENT_LENGTH = 0xFFFF;
  private static final long UINT32_SATURATED = 0xFFFFFFFFL;
  private static final int METHOD_STORED = 0;
  private static final int METHOD_DEFLATED = 8;
  private static final int FLAG_ENCRYPTED = 0x0001;
  private final HashMap<String, EntryLocation> entryLocationMap = new HashMap<>();
  private final byte[] jarBytes;
  private final String entryName;

  /**
   * Indexes a nested jar from its bytes. The bytes are kept, unmodified, for the life of this snapshot.
   *
   * @param entryName identifier of the outer jar entry that provided this cache, preserved for diagnostic lookups
   * @param jarBytes  the complete bytes of the nested jar, exactly as stored in the outer jar
   * @throws ZipException if the bytes are not a well-formed zip archive
   */
  public CachedJarFile (String entryName, byte[] jarBytes)
    throws ZipException {

    long centralDirectoryOffset;
    long centralDirectorySize;
    long entryCount;
    long prependedLength;
    int endPos;
    int zip64LocatorPos;
    int position;

    this.entryName = entryName;
    this.jarBytes = jarBytes;

    endPos = findEnd();
    entryCount = readUnsignedShort(endPos + 10);
    centralDirectorySize = readUnsignedInt(endPos + 12);
    centralDirectoryOffset = readUnsignedInt(endPos + 16);

    // the central directory sits immediately before its end record (or the ZIP64 end record), which locates it even
    // when other bytes have been prepended to the archive and the recorded offsets are therefore short
    if (((zip64LocatorPos = endPos - ZIP64_END_LOCATOR_LENGTH) >= 0) && (readInt(zip64LocatorPos) == ZIP64_END_LOCATOR_SIGNATURE)) {

      int zip64EndPos = findZip64End(zip64LocatorPos, readLong(zip64LocatorPos + 8));

      entryCount = readLong(zip64EndPos + 32);
      centralDirectorySize = readLong(zip64EndPos + 40);
      centralDirectoryOffset = readLong(zip64EndPos + 48);
      prependedLength = zip64EndPos - centralDirectorySize - centralDirectoryOffset;
    } else {
      prependedLength = endPos - centralDirectorySize - centralDirectoryOffset;
    }

    if ((prependedLength < 0) || (centralDirectoryOffset + prependedLength + centralDirectorySize > jarBytes.length)) {
      throw new ZipException("Invalid central directory in nested jar(" + entryName + ")");
    }

    position = (int)(centralDirectoryOffset + prependedLength);
    for (long entryIndex = 0; entryIndex < entryCount; entryIndex++) {
      position = readCentralDirectoryEntry(position, prependedLength);
    }
  }

  /**
   * Returns the identifier of the outer jar entry that originally produced this cache.
   *
   * @return the outer entry name supplied at construction
   */
  @Override
  public String getEntryName () {

    return entryName;
  }

  /**
   * Opens a fresh stream over the named entry's content, inflating it on the fly if it was deflated.
   *
   * @param name the entry's path within the nested jar
   * @return a new stream positioned at the start of the entry's content, or {@code null} if the jar has no such entry
   * @throws ZipException if the entry is encrypted, uses an unsupported compression method, or its local header is
   *                      malformed
   */
  @Override
  public InputStream getInputStream (String name)
    throws ZipException {

    EntryLocation entryLocation;
    int dataPos;

    if ((entryLocation = entryLocationMap.get(name)) == null) {

      return null;
    }

    if ((entryLocation.flags() & FLAG_ENCRYPTED) != 0) {
      throw new ZipException("Encrypted entry(" + name + ") in nested jar(" + entryName + ") is not supported");
    }
    if ((entryLocation.localHeaderPos() + 30 > jarBytes.length) || (readInt(entryLocation.localHeaderPos()) != LOCAL_HEADER_SIGNATURE)) {
      throw new ZipException("Invalid local header for entry(" + name + ") in nested jar(" + entryName + ")");
    }

    // the local header's name and extra field lengths can differ from the central directory's, so they are read here
    dataPos = entryLocation.localHeaderPos() + 30 + readUnsignedShort(entryLocation.localHeaderPos() + 26) + readUnsignedShort(entryLocation.localHeaderPos() + 28);
    if (dataPos + entryLocation.compressedSize() > jarBytes.length) {
      throw new ZipException("Truncated data for entry(" + name + ") in nested jar(" + entryName + ")");
    }

    switch (entryLocation.method()) {
      case METHOD_STORED:

        return new ByteArrayInputStream(jarBytes, dataPos, entryLocation.compressedSize());
      case METHOD_DEFLATED:

        // the trailing zero byte is the extra input that a raw ("nowrap") inflater may ask for at the end of a stream
        return new EntryInflaterInputStream(new SequenceInputStream(new ByteArrayInputStream(jarBytes, dataPos, entryLocation.compressedSize()), new ByteArrayInputStream(new byte[1])), entryLocation.compressedSize());
      default:
        throw new ZipException("Unsupported compression method(" + entryLocation.method() + ") for entry(" + name + ") in nested jar(" + entryName + ")");
    }
  }

  /**
   * Finds the end of central directory record by scanning backwards over the largest possible archive comment. As with
   * the JDK's own zip reader, a record whose comment does not reach the end of the bytes (padding after the archive) is
   * accepted, provided the comment fits.
   *
   * @return the position of the record
   * @throws ZipException if no record is found
   */
  private int findEnd ()
    throws ZipException {

    int lowestPos = Math.max(0, jarBytes.length - END_LENGTH - MAX_COMMENT_LENGTH);

    for (int position = jarBytes.length - END_LENGTH; position >= lowestPos; position--) {
      if ((readInt(position) == END_SIGNATURE) && (position + END_LENGTH + readUnsignedShort(position + 20) <= jarBytes.length)) {

        return position;
      }
    }

    throw new ZipException("No end of central directory record in nested jar(" + entryName + ")");
  }

  /**
   * Locates the ZIP64 end of central directory record. It is normally immediately before its locator; when bytes have
   * been prepended to the archive, the locator's recorded offset is short by their length, so the record is looked
   * for first where the locator says and then immediately before the locator.
   *
   * @param zip64LocatorPos the position of the ZIP64 end locator
   * @param zip64EndOffset  the record offset carried in the locator
   * @return the position of the record
   * @throws ZipException if no record is found at either place
   */
  private int findZip64End (int zip64LocatorPos, long zip64EndOffset)
    throws ZipException {

    if ((zip64EndOffset >= 0) && (zip64EndOffset + 56 <= zip64LocatorPos) && (readInt((int)zip64EndOffset) == ZIP64_END_SIGNATURE) && (zip64EndOffset + 12 + readLong((int)zip64EndOffset + 4) == zip64LocatorPos)) {

      return (int)zip64EndOffset;
    }

    for (int position = zip64LocatorPos - 56; position >= 0; position--) {
      if ((readInt(position) == ZIP64_END_SIGNATURE) && (position + 12 + readLong(position + 4) == zip64LocatorPos)) {

        return position;
      }
    }

    throw new ZipException("No ZIP64 end of central directory record in nested jar(" + entryName + ")");
  }

  /**
   * Reads one central directory entry, records its location, and returns the position of the next entry.
   *
   * @param position        the position of the entry's central directory header
   * @param prependedLength the number of bytes prepended to the archive, added to every recorded offset
   * @return the position of the next central directory header
   * @throws ZipException if the header is malformed or points outside the archive
   */
  private int readCentralDirectoryEntry (int position, long prependedLength)
    throws ZipException {

    String name;
    long compressedSize;
    long uncompressedSize;
    long localHeaderOffset;
    int nameLength;
    int extraLength;
    int commentLength;

    if ((position + 46 > jarBytes.length) || (readInt(position) != CENTRAL_HEADER_SIGNATURE)) {
      throw new ZipException("Invalid central directory header in nested jar(" + entryName + ")");
    }

    compressedSize = readUnsignedInt(position + 20);
    uncompressedSize = readUnsignedInt(position + 24);
    nameLength = readUnsignedShort(position + 28);
    extraLength = readUnsignedShort(position + 30);
    commentLength = readUnsignedShort(position + 32);
    localHeaderOffset = readUnsignedInt(position + 42);

    if (position + 46 + nameLength + extraLength + commentLength > jarBytes.length) {
      throw new ZipException("Truncated central directory in nested jar(" + entryName + ")");
    }

    name = new String(jarBytes, position + 46, nameLength, StandardCharsets.UTF_8);

    // a ZIP64 extra field carries, in this order, only the values whose 32-bit fields are saturated
    if ((uncompressedSize == UINT32_SATURATED) || (compressedSize == UINT32_SATURATED) || (localHeaderOffset == UINT32_SATURATED)) {

      int extraPos = position + 46 + nameLength;
      int extraEnd = extraPos + extraLength;
      boolean found = false;

      while (extraPos + 4 <= extraEnd) {

        int fieldId = readUnsignedShort(extraPos);
        int fieldLength = readUnsignedShort(extraPos + 2);

        if (fieldId == ZIP64_EXTRA_FIELD_ID) {

          int valuePos = extraPos + 4;

          if (uncompressedSize == UINT32_SATURATED) {
            uncompressedSize = readLong(valuePos);
            valuePos += 8;
          }
          if (compressedSize == UINT32_SATURATED) {
            compressedSize = readLong(valuePos);
            valuePos += 8;
          }
          if (localHeaderOffset == UINT32_SATURATED) {
            localHeaderOffset = readLong(valuePos);
          }

          found = true;
          break;
        }

        extraPos += 4 + fieldLength;
      }

      if (!found) {
        throw new ZipException("Missing ZIP64 extra field for entry(" + name + ") in nested jar(" + entryName + ")");
      }
    }

    if ((localHeaderOffset + prependedLength > Integer.MAX_VALUE) || (compressedSize > Integer.MAX_VALUE)) {
      throw new ZipException("Entry(" + name + ") lies beyond the addressable range of nested jar(" + entryName + ")");
    }

    entryLocationMap.put(name, new EntryLocation((int)(localHeaderOffset + prependedLength), (int)compressedSize, readUnsignedShort(position + 10), readUnsignedShort(position + 8)));

    return position + 46 + nameLength + extraLength + commentLength;
  }

  private int readUnsignedShort (int position) {

    return (jarBytes[position] & 0xFF) | ((jarBytes[position + 1] & 0xFF) << 8);
  }

  private int readInt (int position) {

    return (jarBytes[position] & 0xFF) | ((jarBytes[position + 1] & 0xFF) << 8) | ((jarBytes[position + 2] & 0xFF) << 16) | ((jarBytes[position + 3] & 0xFF) << 24);
  }

  private long readUnsignedInt (int position) {

    return readInt(position) & UINT32_SATURATED;
  }

  private long readLong (int position) {

    return readUnsignedInt(position) | (readUnsignedInt(position + 4) << 32);
  }

  /**
   * Where an entry's data lies and how it is encoded.
   *
   * @param localHeaderPos position of the entry's local header within the jar bytes
   * @param compressedSize length of the entry's data as stored
   * @param method         the zip compression method
   * @param flags          the zip general purpose flags
   */
  private record EntryLocation(int localHeaderPos, int compressedSize, int method, int flags) {

  }

  /**
   * Inflating stream that owns its raw-mode {@link Inflater} and releases it on close, rather than leaving the native
   * memory to be reclaimed whenever the inflater becomes unreachable.
   */
  private static class EntryInflaterInputStream extends InflaterInputStream {

    private boolean closed = false;

    /**
     * @param inputStream    the entry's deflated data
     * @param compressedSize the length of that data, used to size the read buffer
     */
    private EntryInflaterInputStream (InputStream inputStream, int compressedSize) {

      super(inputStream, new Inflater(true), Math.max(64, Math.min(8192, compressedSize + 1)));
    }

    @Override
    public void close ()
      throws IOException {

      if (!closed) {
        closed = true;
        try {
          super.close();
        } finally {
          inf.end();
        }
      }
    }
  }
}
