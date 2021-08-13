/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class JailedPath implements Path {

  private final JailedFileSystem jailedFileSystem;
  private final Segment[] segments;
  private final char[] text;
  private final boolean hasRoot;

  private JailedPath (JailedFileSystem jailedFileSystem, char[] text, boolean hasRoot, Segment... segments) {

    this.jailedFileSystem = jailedFileSystem;
    this.text = text;
    this.hasRoot = hasRoot;
    this.segments = segments;
  }

  public JailedPath (JailedFileSystem jailedFileSystem, char... text) {

    this.jailedFileSystem = jailedFileSystem;
    this.text = text;

    hasRoot = ((text.length > 0) && (text[0] == '/'));
    segments = divideAndConquer();
  }

  public JailedPath (JailedFileSystem jailedFileSystem, String text) {

    this(jailedFileSystem, text.toCharArray());
  }

  private Segment[] divideAndConquer () {

    Segment[] segments;
    boolean slash = true;
    int count = 0;
    int begin = 0;

    for (char singleChar : text) {
      if (singleChar == '/') {
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
        if (text[index] == '/') {
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

  private char[] getText () {

    return text;
  }

  private Segment[] getSegments () {

    return segments;
  }

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

  private Path constructPath (char[] text, boolean hasRoot, Segment... segments) {

    return constructPath(null, null, text, hasRoot, segments);
  }

  private Path constructPath (char[] prologueText, Segment[] prologueSegments, char[] text, boolean hasRoot, Segment... segments) {

    Segment[] translatedSegments = new Segment[((prologueSegments == null) ? 0 : prologueSegments.length) + segments.length];
    StringBuilder translatedTextBuilder = (prologueText == null) ? new StringBuilder() : new StringBuilder(String.copyValueOf(prologueText));
    char[] translatedText;

    if (prologueSegments != null) {
      System.arraycopy(prologueSegments, 0, translatedSegments, 0, prologueSegments.length);
    }

    for (int segmentIndex = 0; segmentIndex < segments.length; segmentIndex++) {

      int segmentLength = segments[segmentIndex].length();

      if (hasRoot || translatedTextBuilder.length() > 0) {
        translatedTextBuilder.append('/');
        translatedSegments[segmentIndex] = new Segment(translatedTextBuilder.length(), translatedTextBuilder.length() + segmentLength);
      } else {
        translatedSegments[segmentIndex] = new Segment(0, segmentLength);
      }

      for (int charIndex = 0; charIndex < segmentLength; charIndex++) {
        translatedTextBuilder.append(text[segments[segmentIndex].getBegin() + charIndex]);
      }
    }

    translatedText = new char[translatedTextBuilder.length()];
    translatedTextBuilder.getChars(0, translatedTextBuilder.length(), translatedText, 0);

    return new JailedPath(jailedFileSystem, translatedText, hasRoot, translatedSegments);
  }

  @Override
  public FileSystem getFileSystem () {

    return jailedFileSystem;
  }

  @Override
  public boolean isAbsolute () {

    return hasRoot;
  }

  @Override
  public Path getRoot () {

    return hasRoot ? new JailedPath(jailedFileSystem, new char[] {'/'}, true) : null;
  }

  @Override
  public Path getFileName () {

    return (segments.length == 0) ? null : constructPath(text, false, segments[segments.length - 1]);
  }

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

  @Override
  public int getNameCount () {

    return segments.length;
  }

  @Override
  public Path getName (int index) {

    if ((index < 0) || (index >= segments.length)) {
      throw new IllegalArgumentException("The requested element does not exist");
    } else {

      return constructPath(text, false, segments[index]);
    }
  }

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

  @Override
  public boolean startsWith (Path other) {

    if (!(other instanceof JailedPath)) {
      throw new ProviderMismatchException();
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

  @Override
  public boolean endsWith (Path other) {

    if (!(other instanceof JailedPath)) {
      throw new ProviderMismatchException();
    } else if (((!other.isAbsolute()) || hasRoot) && (segments.length >= other.getNameCount())) {

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

  @Override
  public Path normalize () {

    boolean normalized = true;

    for (Segment segment : segments) {

      int segmentLength = segment.length();

      if (((segmentLength == 1) && (text[segment.getBegin()] != '.')) || ((segmentLength == 2) && (text[segment.getBegin()] == '.') && (text[segment.getBegin() + 1] == '.'))) {
        normalized = false;
        break;
      }
    }

    if (normalized) {

      return this;
    } else {

      LinkedList<Segment> normalizedSegmentList = new LinkedList<>();

      for (Segment segment : segments) {

        int segmentLength = segment.length();

        if ((segmentLength != 1) || (text[segment.getBegin()] != '.')) {
          if ((segmentLength == 2) && (text[segment.getBegin()] == '.') && (text[segment.getBegin() + 1] == '.')) {
            normalizedSegmentList.pop();
          } else {
            normalizedSegmentList.push(segment);
          }
        }
      }

      return constructPath(text, hasRoot, normalizedSegmentList.toArray(new Segment[0]));
    }
  }

  @Override
  public Path resolve (Path other) {

    if (!(other instanceof JailedPath)) {
      throw new ProviderMismatchException();
    } else if (other.isAbsolute()) {

      return other;
    } else if (other.getNameCount() == 0) {

      return this;
    } else {

      return constructPath(text, segments, ((JailedPath)other).getText(), hasRoot, ((JailedPath)other).getSegments());
    }
  }

  @Override
  public Path relativize (Path other) {

    if (!(other instanceof JailedPath)) {
      throw new ProviderMismatchException();
    } else if (hasRoot != other.isAbsolute()) {
      throw new IllegalArgumentException("The paths specified must be either both absolute or both relative");
    } else {

      JailedPath normalizedPath = (JailedPath)normalize();
      JailedPath otherNormalizedPath = (JailedPath)other.normalize();
      LinkedList<Segment> redactedSegmentList = new LinkedList<>();
      StringBuilder redactedTextBuilder = new StringBuilder();
      char[] redactedText;

      for (int segmentIndex = 0; segmentIndex < segments.length; segmentIndex++) {
        if ((segmentIndex >= otherNormalizedPath.getNameCount()) || (!normalizedPath.sameSegment(otherNormalizedPath.getText(), otherNormalizedPath.getSegments()[segmentIndex], segmentIndex))) {
          for (int count = 0; count < segments.length - segmentIndex; count++) {
            if (redactedTextBuilder.length() > 0) {
              redactedSegmentList.add(new Segment(redactedTextBuilder.length() + 1, redactedTextBuilder.length() + 3));
              redactedTextBuilder.append('/');
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

      return constructPath(otherNormalizedPath.getText(), false, Arrays.copyOfRange(otherNormalizedPath.getSegments(), segments.length, otherNormalizedPath.getNameCount()));
    }
  }

  @Override
  public Path toAbsolutePath () {

    if (isAbsolute()) {

      return this;
    } else {

      return constructPath(text, true, getSegments());
    }
  }

  @Override
  public Path toRealPath (LinkOption... options) {

    return normalize().toAbsolutePath();
  }

  @Override
  public int compareTo (Path other) {

    if (!(other instanceof JailedPath)) {
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

  @Override
  public URI toUri () {

    try {
      return new URI(jailedFileSystem.provider().getScheme(), "", toAbsolutePath().toString(), null);
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public WatchKey register (WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {

    throw new UnsupportedOperationException();
  }

  @Override
  public String toString () {

    return String.valueOf(text);
  }

  private static class Segment {

    private final int begin;
    private final int end;

    public Segment (int begin, int end) {

      this.begin = begin;
      this.end = end;
    }

    public int getBegin () {

      return begin;
    }

    public int getEnd () {

      return end;
    }

    public int length () {

      return end - begin;
    }
  }
}
