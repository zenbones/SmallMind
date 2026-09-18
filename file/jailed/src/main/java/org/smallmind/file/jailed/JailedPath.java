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
package org.smallmind.file.jailed;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * A {@link Path} implementation that operates within the confines of a {@link JailedFileSystem}.
 *
 * <p>Path text is stored as a raw {@code char[]} and decomposed into {@link Segment} index
 * pairs on construction to enable efficient, allocation-free segment operations. The separator
 * is always the forward slash ({@code '/'}).
 *
 * <p>Jail space is closed: {@link #resolve} and {@link #relativize} require the other path to
 * belong to the same {@link JailedFileSystem} and throw {@link ProviderMismatchException} if it
 * does not, as does {@link #compareTo}, while {@link #startsWith} and {@link #endsWith} simply
 * report {@code false} for a path of any other file system, as their contracts require.
 *
 * <p>Name elements are compared character-for-character, so jail space is case-sensitive
 * regardless of how the backing native file system compares names, and so
 * {@link #equals(Object)} reports whether two paths are the same path rather than whether they
 * locate the same file.
 *
 * @see JailedFileSystem
 * @see JailedFileSystemProvider
 */
public class JailedPath implements Path {

  /**
   * The path separator character used by all jailed paths, regardless of the underlying
   * operating system.
   */
  protected static final char SEPARATOR = '/';

  /**
   * The file system that owns this path.
   */
  private final JailedFileSystem jailedFileSystem;

  /**
   * Parsed segment boundaries within {@link #text}.
   */
  private final Segment[] segments;

  /**
   * The raw character representation of the full path string.
   */
  private final char[] text;

  /**
   * Whether this path is absolute (starts with the separator).
   */
  private final boolean hasRoot;

  /**
   * Low-level constructor used internally when segment boundaries have already been
   * computed, avoiding a redundant parse.
   *
   * @param jailedFileSystem the owning {@link JailedFileSystem}
   * @param text             the raw path characters
   * @param hasRoot          {@code true} if the path is absolute
   * @param segments         pre-parsed segment boundaries within {@code text}
   */
  protected JailedPath (JailedFileSystem jailedFileSystem, char[] text, boolean hasRoot, Segment... segments) {

    this.jailedFileSystem = jailedFileSystem;
    this.text = text;
    this.hasRoot = hasRoot;
    this.segments = segments;
  }

  /**
   * Creates a path by parsing the raw character array.
   *
   * <p>Absoluteness is determined by whether the first character is the separator.
   * Segments are extracted by scanning for separator-delimited tokens.
   *
   * @param jailedFileSystem the owning {@link JailedFileSystem}
   * @param text             the raw path characters to parse
   */
  public JailedPath (JailedFileSystem jailedFileSystem, char... text) {

    this.jailedFileSystem = jailedFileSystem;
    this.text = text;

    hasRoot = ((text.length > 0) && (text[0] == SEPARATOR));
    segments = divideAndConquer();
  }

  /**
   * Creates a path by parsing a string.
   *
   * <p>The string is converted to a {@code char[]} and delegated to
   * {@link #JailedPath(JailedFileSystem, char...)}.
   *
   * @param jailedFileSystem the owning {@link JailedFileSystem}
   * @param text             the string representation of the path
   */
  public JailedPath (JailedFileSystem jailedFileSystem, String text) {

    this(jailedFileSystem, text.toCharArray());
  }

  /**
   * Parses {@link #text} into an array of {@link Segment} boundaries.
   *
   * <p>Consecutive separators are collapsed and leading/trailing separators are ignored
   * for segment purposes.
   *
   * @return an array of {@link Segment} objects describing each path component
   */
  private Segment[] divideAndConquer () {

    Segment[] segments;
    boolean slash = true;
    int count = 0;
    int begin = 0;

    for (char singleChar : text) {
      if (singleChar == SEPARATOR) {
        slash = true;
      } else if (slash) {
        slash = false;
        count++;
      }
    }

    segments = new Segment[count];

    if (count > 0) {
      slash = true;
      count = 0;
      for (int index = 0; index < text.length; index++) {
        if (text[index] == SEPARATOR) {
          if (!slash) {
            segments[count++] = new Segment(begin, index);
          }
          slash = true;
        } else if (slash) {
          slash = false;
          begin = index;
        }
      }
      if (!slash) {
        segments[count] = new Segment(begin, text.length);
      }
    }

    return segments;
  }

  /**
   * Returns the raw character array backing this path.
   *
   * @return the raw path characters
   */
  private char[] getText () {

    return text;
  }

  /**
   * Returns the parsed segment array for this path.
   *
   * @return the array of {@link Segment} boundaries; never {@code null}
   */
  private Segment[] getSegments () {

    return segments;
  }

  /**
   * Tests whether the segment at {@code segmentIndex} in this path is character-for-character
   * equal to the given segment in another path's text buffer.
   *
   * @param otherText    the raw text of the other path
   * @param otherSegment the segment within {@code otherText} to compare against
   * @param segmentIndex the index of the segment in this path to compare
   * @return {@code true} if the two segments are identical
   */
  private boolean sameSegment (char[] otherText, Segment otherSegment, int segmentIndex) {

    Segment segment = segments[segmentIndex];
    int segmentLength = segment.length();

    if (segmentLength == otherSegment.length()) {
      for (int charIndex = 0; charIndex < segmentLength; charIndex++) {
        if (!(text[segment.getBegin() + charIndex] == otherText[otherSegment.getBegin() + charIndex])) {

          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Convenience overload that builds a new path from text and segments without a prologue.
   *
   * @param text     raw text for the new path
   * @param hasRoot  whether the new path should be absolute
   * @param segments segment boundaries within {@code text}
   * @return the constructed {@link JailedPath}
   */
  private Path constructPath (char[] text, boolean hasRoot, Segment... segments) {

    return constructPath(null, null, text, hasRoot, segments);
  }

  /**
   * Builds a new {@link JailedPath} by concatenating an optional prologue (text and
   * segments already accumulated) with additional segments extracted from {@code text}.
   *
   * <p>Separator characters are inserted between segments as required to produce a valid
   * path string.
   *
   * @param prologueText     optional text that forms the prefix of the new path, or
   *                         {@code null} if there is no prefix
   * @param prologueSegments optional pre-parsed segments for the prologue portion, or
   *                         {@code null} if there is no prefix
   * @param text             the text from which {@code segments} are drawn
   * @param hasRoot          whether the resulting path should be absolute
   * @param segments         the segments from {@code text} to append after the prologue
   * @return a new {@link JailedPath} combining the prologue and the given segments
   */
  private Path constructPath (char[] prologueText, Segment[] prologueSegments, char[] text, boolean hasRoot, Segment... segments) {

    int prologueSegmentCount = (prologueSegments == null) ? 0 : prologueSegments.length;
    Segment[] translatedSegments = new Segment[prologueSegmentCount + segments.length];
    StringBuilder translatedTextBuilder = (prologueText == null) ? new StringBuilder() : new StringBuilder(String.copyValueOf(prologueText));
    char[] translatedText;

    if (prologueSegments != null) {
      System.arraycopy(prologueSegments, 0, translatedSegments, 0, prologueSegments.length);
    }

    for (int segmentIndex = 0; segmentIndex < segments.length; segmentIndex++) {

      int segmentLength = segments[segmentIndex].length();

      if (hasRoot || (!translatedTextBuilder.isEmpty())) {
        translatedTextBuilder.append(SEPARATOR);
        translatedSegments[prologueSegmentCount + segmentIndex] = new Segment(translatedTextBuilder.length(), translatedTextBuilder.length() + segmentLength);
      } else {
        translatedSegments[prologueSegmentCount + segmentIndex] = new Segment(0, segmentLength);
      }

      for (int charIndex = 0; charIndex < segmentLength; charIndex++) {
        translatedTextBuilder.append(text[segments[segmentIndex].getBegin() + charIndex]);
      }
    }

    if (hasRoot && translatedTextBuilder.isEmpty()) {
      translatedTextBuilder.append(SEPARATOR);
    }

    translatedText = new char[translatedTextBuilder.length()];
    translatedTextBuilder.getChars(0, translatedTextBuilder.length(), translatedText, 0);

    return new JailedPath(jailedFileSystem, translatedText, hasRoot, translatedSegments);
  }

  /**
   * Returns the file system that created this path.
   *
   * @return the {@link JailedFileSystem} that owns this path
   */
  @Override
  public FileSystem getFileSystem () {

    return jailedFileSystem;
  }

  /**
   * Indicates whether this path is absolute.
   *
   * @return {@code true} if this path starts with the separator character
   */
  @Override
  public boolean isAbsolute () {

    return hasRoot;
  }

  /**
   * Returns the root component of this path.
   *
   * @return a {@link JailedPath} representing {@code "/"} if this path is absolute,
   * or {@code null} if it is relative
   */
  @Override
  public Path getRoot () {

    return hasRoot ? new JailedPath(jailedFileSystem, new char[] {SEPARATOR}, true) : null;
  }

  /**
   * Returns the last segment of this path as a relative single-element path, or
   * {@code null} if this path has no segments (e.g., the root path).
   *
   * @return the last path element, or {@code null}
   */
  @Override
  public Path getFileName () {

    return (segments.length == 0) ? null : constructPath(text, false, segments[segments.length - 1]);
  }

  /**
   * Returns the parent of this path: this path with the last segment removed.
   * Returns the root if this path has exactly one segment and is absolute, or
   * {@code null} if there is no parent.
   *
   * @return the parent path, the root, or {@code null}
   */
  @Override
  public Path getParent () {

    if (segments.length == 0) {

      return null;
    } else if (segments.length == 1) {

      return getRoot();
    } else {

      Segment[] allButOne = new Segment[segments.length - 1];

      System.arraycopy(segments, 0, allButOne, 0, segments.length - 1);

      return constructPath(text, hasRoot, allButOne);
    }
  }

  /**
   * Returns the number of name elements (segments) in this path.
   *
   * @return the segment count; {@code 0} for the root or empty path
   */
  @Override
  public int getNameCount () {

    return segments.length;
  }

  /**
   * Returns the segment at position {@code index} as a relative single-element path.
   *
   * @param index zero-based index of the segment to retrieve
   * @return the path segment at the given index
   * @throws IllegalArgumentException if {@code index} is out of range
   */
  @Override
  public Path getName (int index) {

    if ((index < 0) || (index >= segments.length)) {
      throw new IllegalArgumentException("The requested element does not exist");
    } else {

      return constructPath(text, false, segments[index]);
    }
  }

  /**
   * Returns a relative path consisting of the segments in the range
   * [{@code beginIndex}, {@code endIndex}).
   *
   * @param beginIndex the index of the first segment, inclusive
   * @param endIndex   the index of the last segment, exclusive
   * @return the sub-path
   * @throws IllegalArgumentException if the range is invalid
   */
  @Override
  public Path subpath (int beginIndex, int endIndex) {

    if ((beginIndex < 0) || (beginIndex >= segments.length) || (endIndex <= beginIndex) || (endIndex > segments.length)) {
      throw new IllegalArgumentException("The subpath specified does not exist");
    } else {

      Segment[] subSegments = new Segment[endIndex - beginIndex];

      System.arraycopy(segments, beginIndex, subSegments, 0, endIndex - beginIndex);

      return constructPath(text, false, subSegments);
    }
  }

  /**
   * Returns {@code true} if this path begins with {@code other}, meaning the leading
   * segments are identical to all segments of {@code other} and their absoluteness agrees.
   *
   * <p>As required by {@link Path#startsWith(Path)}, a path belonging to any other file system
   * simply does not start with this one, and so yields {@code false} rather than an exception.
   *
   * @param other the path to test against
   * @return {@code true} if this path starts with {@code other}
   */
  @Override
  public boolean startsWith (Path other) {

    if (!((other instanceof JailedPath) && jailedFileSystem.equals(other.getFileSystem()))) {

      return false;
    } else if ((hasRoot == other.isAbsolute()) && (other.getNameCount() <= segments.length)) {

      int segmentIndex = 0;

      for (Segment otherSegment : ((JailedPath)other).getSegments()) {
        if (!sameSegment(((JailedPath)other).getText(), otherSegment, segmentIndex++)) {

          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Returns {@code true} if this path ends with {@code other}. If {@code other} is
   * absolute, this path must also be absolute with the same segments.
   *
   * <p>As required by {@link Path#endsWith(Path)}, a path belonging to any other file system
   * simply does not end this one, and so yields {@code false} rather than an exception.
   *
   * @param other the path to test against
   * @return {@code true} if this path ends with {@code other}
   */
  @Override
  public boolean endsWith (Path other) {

    if (!((other instanceof JailedPath) && jailedFileSystem.equals(other.getFileSystem()))) {

      return false;
    } else if (((!other.isAbsolute()) || (hasRoot && (segments.length == other.getNameCount()))) && (segments.length >= other.getNameCount())) {

      int segmentIndex = segments.length - other.getNameCount();

      for (Segment otherSegment : ((JailedPath)other).getSegments()) {
        if (!sameSegment(((JailedPath)other).getText(), otherSegment, segmentIndex++)) {

          return false;
        }
      }

      return true;
    }

    return false;
  }

  /**
   * Returns a path with {@code "."} and {@code ".."} segments resolved. If no such segments
   * are present this path is returned unchanged.
   *
   * <p>A {@code ".."} segment cancels the name element that precedes it. Because an absolute
   * jailed path is rooted at the jail itself, a {@code ".."} with nothing left to cancel is
   * discarded, exactly as {@code "/.."} denotes {@code "/"}; this is what makes it impossible
   * to express a location above the jail root. A relative path, whose base is not yet known,
   * instead retains such a segment, so that {@code "a/../.."} normalizes to {@code ".."}.
   *
   * @return a normalized path, never {@code null} and never throwing for surplus {@code ".."}
   * segments
   */
  @Override
  public Path normalize () {

    boolean normalized = true;

    for (Segment segment : segments) {
      if (isDot(segment) || isDotDot(segment)) {
        normalized = false;
        break;
      }
    }

    if (normalized) {

      return this;
    } else {

      LinkedList<Segment> normalizedSegmentList = new LinkedList<>();

      for (Segment segment : segments) {
        if (!isDot(segment)) {
          if (!isDotDot(segment)) {
            normalizedSegmentList.add(segment);
          } else if ((!normalizedSegmentList.isEmpty()) && (!isDotDot(normalizedSegmentList.getLast()))) {
            normalizedSegmentList.removeLast();
          } else if (!hasRoot) {
            normalizedSegmentList.add(segment);
          }
        }
      }

      return constructPath(text, hasRoot, normalizedSegmentList.toArray(new Segment[0]));
    }
  }

  /**
   * Tests whether a segment of this path is the current-directory segment {@code "."}.
   *
   * @param segment the segment to test, which must index into this path's text
   * @return {@code true} if the segment consists of a single period
   */
  private boolean isDot (Segment segment) {

    return (segment.length() == 1) && (text[segment.getBegin()] == '.');
  }

  /**
   * Tests whether a segment of this path is the parent-directory segment {@code ".."}.
   *
   * @param segment the segment to test, which must index into this path's text
   * @return {@code true} if the segment consists of exactly two periods
   */
  private boolean isDotDot (Segment segment) {

    return (segment.length() == 2) && (text[segment.getBegin()] == '.') && (text[segment.getBegin() + 1] == '.');
  }

  /**
   * Resolves {@code other} against this path. If {@code other} is absolute it is returned
   * as-is; if it has no segments this path is returned; otherwise its segments are appended.
   *
   * @param other the path to resolve; must be a {@link JailedPath}
   * @return the resolved path
   * @throws ProviderMismatchException if {@code other} is not a {@link JailedPath}
   */
  @Override
  public Path resolve (Path other) {

    if (!((other instanceof JailedPath) && jailedFileSystem.equals(other.getFileSystem()))) {
      throw new ProviderMismatchException();
    } else if (other.isAbsolute()) {

      return other;
    } else if (other.getNameCount() == 0) {

      return this;
    } else {

      return constructPath(text, segments, ((JailedPath)other).getText(), hasRoot, ((JailedPath)other).getSegments());
    }
  }

  /**
   * Constructs a relative path from this path to {@code other}. Common leading segments are
   * cancelled and replaced with {@code ".."} steps as necessary.
   *
   * @param other the target path; must be a {@link JailedPath} with matching absoluteness
   * @return a relative path from this path to {@code other}
   * @throws ProviderMismatchException if {@code other} is not a {@link JailedPath}
   * @throws IllegalArgumentException  if the two paths differ in absoluteness
   */
  @Override
  public Path relativize (Path other) {

    if (!((other instanceof JailedPath) && jailedFileSystem.equals(other.getFileSystem()))) {
      throw new ProviderMismatchException();
    } else if (hasRoot != other.isAbsolute()) {
      throw new IllegalArgumentException("The paths specified must be either both absolute or both relative");
    } else {

      JailedPath normalizedPath = (JailedPath)normalize();
      JailedPath otherNormalizedPath = (JailedPath)other.normalize();
      LinkedList<Segment> redactedSegmentList = new LinkedList<>();
      StringBuilder redactedTextBuilder = new StringBuilder();
      int normalizedNameCount = normalizedPath.getNameCount();
      char[] redactedText;

      for (int segmentIndex = 0; segmentIndex < normalizedNameCount; segmentIndex++) {
        if ((segmentIndex >= otherNormalizedPath.getNameCount()) || (!normalizedPath.sameSegment(otherNormalizedPath.getText(), otherNormalizedPath.getSegments()[segmentIndex], segmentIndex))) {
          for (int count = 0; count < normalizedNameCount - segmentIndex; count++) {
            if (redactedTextBuilder.length() > 0) {
              redactedSegmentList.add(new Segment(redactedTextBuilder.length() + 1, redactedTextBuilder.length() + 3));
              redactedTextBuilder.append(SEPARATOR);
            } else {
              redactedSegmentList.add(new Segment(0, 2));
            }
            redactedTextBuilder.append("..");
          }

          redactedText = new char[redactedTextBuilder.length()];
          redactedTextBuilder.getChars(0, redactedTextBuilder.length(), redactedText, 0);

          return constructPath(redactedText, redactedSegmentList.toArray(new Segment[0]), otherNormalizedPath.getText(), false, Arrays.copyOfRange(otherNormalizedPath.getSegments(), segmentIndex, otherNormalizedPath.getNameCount()));
        }
      }

      return constructPath(otherNormalizedPath.getText(), false, Arrays.copyOfRange(otherNormalizedPath.getSegments(), normalizedNameCount, otherNormalizedPath.getNameCount()));
    }
  }

  /**
   * Returns an absolute form of this path. If already absolute, returns {@code this};
   * otherwise a new path with the same segments but marked as absolute is returned.
   *
   * @return an absolute version of this path
   */
  @Override
  public Path toAbsolutePath () {

    if (isAbsolute()) {

      return this;
    } else {

      return constructPath(text, true, getSegments());
    }
  }

  /**
   * Returns the real path of an existing file, resolved on the backing native file system and
   * translated back into jail space.
   *
   * <p>The path is translated to its native equivalent, resolved there - which eliminates
   * {@code .} and {@code ..} elements and, unless {@link LinkOption#NOFOLLOW_LINKS} is given,
   * follows symbolic links - and the result is wrapped back into the jail. A real path that
   * falls outside the jail is refused rather than reported, so a link pointing out of the jail
   * can not be used to learn about the file system beyond it.
   *
   * @param options options indicating how symbolic links are handled
   * @return the absolute, resolved jailed {@link Path} of the file
   * @throws IOException       if the file does not exist or can not be resolved
   * @throws SecurityException if the resolved location lies outside the jail
   */
  @Override
  public Path toRealPath (LinkOption... options)
    throws IOException {

    JailedPathTranslator jailedPathTranslator = jailedFileSystem.getJailedPathTranslator();

    return jailedPathTranslator.wrapPath(jailedFileSystem, jailedPathTranslator.unwrapPath(this, options).toRealPath(options));
  }

  /**
   * Compares this path to {@code other} lexicographically by segment. Absolute paths sort
   * after relative paths when absoluteness differs.
   *
   * @param other the path to compare to; must be a {@link JailedPath}
   * @return a negative integer, zero, or a positive integer as this path is less than,
   * equal to, or greater than {@code other}
   * @throws ProviderMismatchException if {@code other} is not a {@link JailedPath}
   */
  @Override
  public int compareTo (Path other) {

    if (!((other instanceof JailedPath) && jailedFileSystem.equals(other.getFileSystem()))) {
      throw new ProviderMismatchException();
    } else if (hasRoot != other.isAbsolute()) {

      return hasRoot ? 1 : -1;
    } else {

      int maxSegmentCount = Math.max(segments.length, other.getNameCount());

      for (int segmentIndex = 0; segmentIndex < maxSegmentCount; segmentIndex++) {
        if ((segmentIndex >= segments.length) || (segmentIndex >= other.getNameCount())) {

          return (segmentIndex >= segments.length) ? -1 : 1;
        } else {

          int comparison;

          if ((comparison = compareSegment(segments[segmentIndex], ((JailedPath)other).getText(), ((JailedPath)other).getSegments()[segmentIndex])) != 0) {

            return comparison;
          }
        }
      }

      return 0;
    }
  }

  /**
   * Performs a lexicographic character-by-character comparison between a segment in this
   * path and a segment in another path's text buffer.
   *
   * @param segment      the segment in this path to compare
   * @param otherText    the raw text of the other path
   * @param otherSegment the segment within {@code otherText} to compare against
   * @return a negative integer, zero, or a positive integer as the local segment is less
   * than, equal to, or greater than the other segment
   */
  private int compareSegment (Segment segment, char[] otherText, Segment otherSegment) {

    int segmentLength = segment.length();
    int otherSegmentLength = otherSegment.length();
    int maxSegmentLength = Math.max(segmentLength, otherSegmentLength);

    for (int charIndex = 0; charIndex < maxSegmentLength; charIndex++) {
      if ((charIndex >= segmentLength) || (charIndex >= otherSegmentLength)) {

        return (charIndex >= segmentLength) ? -1 : 1;
      } else {

        int comparison;

        if ((comparison = Character.compare(text[segment.getBegin() + charIndex], otherText[otherSegment.getBegin() + charIndex])) != 0) {

          return comparison;
        }
      }
    }

    return 0;
  }

  /**
   * Returns a URI of the form {@code <scheme>://<absolutePath>}. Any checked exception
   * during {@link URI} construction is wrapped in a {@link RuntimeException}.
   *
   * @return a URI that identifies this path within the jailed file system
   */
  @Override
  public URI toUri () {

    try {
      return new URI(jailedFileSystem.provider().getScheme(), "", toAbsolutePath().toString(), null);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Registers this path with a watch service obtained from
   * {@link JailedFileSystem#newWatchService()}.
   *
   * <p>The registration is performed against the native equivalent of this path, and the
   * returned key reports this jailed path as its {@link WatchKey#watchable()} and delivers
   * events whose context lies in jail space.
   *
   * @param watcher   the watch service to register with, which must have been created by the
   *                  jailed file system that owns this path
   * @param events    the events to watch for
   * @param modifiers optional modifiers qualifying how the path is registered
   * @return a {@link WatchKey} representing the registration of this path
   * @throws IOException               if an I/O error occurs
   * @throws ProviderMismatchException if {@code watcher} was not created by this file system
   * @throws SecurityException         if this path can not be confined to the jail
   */
  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers)
    throws IOException {

    if (!(watcher instanceof JailedWatchService)) {
      throw new ProviderMismatchException();
    } else {

      return ((JailedWatchService)watcher).register(this, events, modifiers);
    }
  }

  /**
   * Returns the string representation of this path.
   *
   * @return the path string as stored in the underlying char array
   */
  @Override
  public String toString () {

    return String.valueOf(text);
  }

  /**
   * Returns a hash code consistent with {@link #equals(Object)}, derived from the owning file
   * system, whether the path is absolute, and the characters of each name element.
   *
   * @return a hash code for this path
   */
  @Override
  public int hashCode () {

    int hashCode = (31 * jailedFileSystem.hashCode()) + (hasRoot ? 1 : 0);

    for (Segment segment : segments) {
      for (int charIndex = segment.getBegin(); charIndex < segment.getEnd(); charIndex++) {
        hashCode = (31 * hashCode) + text[charIndex];
      }

      hashCode = 31 * hashCode;
    }

    return hashCode;
  }

  /**
   * Tests this path for equality with another object.
   *
   * <p>Two jailed paths are equal when they belong to the same {@link JailedFileSystem}, agree
   * on whether they are absolute, and have the same name elements. Comparison is
   * character-for-character, so jail space is case-sensitive no matter how the backing native
   * file system compares names; equality therefore says nothing about whether two paths locate
   * the same file, which is what {@link java.nio.file.Files#isSameFile(Path, Path)} answers.
   *
   * <p>Redundant separators are not significant, since they are discarded when the path is
   * parsed - {@code "//a//b"} equals {@code "/a/b"}.
   *
   * @param obj the object to compare with this path
   * @return {@code true} if {@code obj} is an equal jailed path
   */
  @Override
  public boolean equals (Object obj) {

    if (this == obj) {

      return true;
    } else if (!((obj instanceof JailedPath) && jailedFileSystem.equals(((JailedPath)obj).getFileSystem()))) {

      return false;
    } else if ((hasRoot != ((JailedPath)obj).isAbsolute()) || (segments.length != ((JailedPath)obj).getNameCount())) {

      return false;
    } else {

      for (int segmentIndex = 0; segmentIndex < segments.length; segmentIndex++) {
        if (!sameSegment(((JailedPath)obj).getText(), ((JailedPath)obj).getSegments()[segmentIndex], segmentIndex)) {

          return false;
        }
      }

      return true;
    }
  }

  /**
   * Immutable record of a single path segment's character boundaries within the
   * owning {@link JailedPath}'s raw text array.
   *
   * <p>The segment spans {@code text[begin..end-1]} (i.e., {@code begin} is inclusive
   * and {@code end} is exclusive).
   */
  protected static class Segment {

    /**
     * Inclusive start index of the segment within the path text array.
     */
    private final int begin;

    /**
     * Exclusive end index of the segment within the path text array.
     */
    private final int end;

    /**
     * Creates a segment record with the given bounds.
     *
     * @param begin the inclusive start index within the parent path's char array
     * @param end   the exclusive end index within the parent path's char array
     */
    public Segment (int begin, int end) {

      this.begin = begin;
      this.end = end;
    }

    /**
     * Returns the inclusive start index of this segment within the parent path's char array.
     *
     * @return the inclusive start index
     */
    public int getBegin () {

      return begin;
    }

    /**
     * Returns the exclusive end index of this segment within the parent path's char array.
     *
     * @return the exclusive end index
     */
    public int getEnd () {

      return end;
    }

    /**
     * Returns the number of characters in this segment ({@code end - begin}).
     *
     * @return the segment length; always non-negative
     */
    public int length () {

      return end - begin;
    }
  }
}
