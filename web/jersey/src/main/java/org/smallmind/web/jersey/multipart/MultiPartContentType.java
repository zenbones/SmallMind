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
package org.smallmind.web.jersey.multipart;

/**
 * Known multipart content types keyed by file extension.
 */
public enum MultiPartContentType {

  IMAGE_PNG("png", "image/png");

  private final String extension;
  private final String contentType;

  /**
   * Associates an extension with a content type string.
   *
   * @param extension   file extension without dot
   * @param contentType MIME content type
   */
  MultiPartContentType (String extension, String contentType) {

    this.extension = extension;
    this.contentType = contentType;
  }

  /**
   * Looks up a content type based on a file extension.
   *
   * @param extension extension to match
   * @return matching {@link MultiPartContentType} or {@code null} if none found
   */
  public static MultiPartContentType forExtension (String extension) {

    for (MultiPartContentType multiPartContentType : MultiPartContentType.values()) {
      if (multiPartContentType.getExtension().equals(extension)) {

        return multiPartContentType;
      }
    }

    return null;
  }

  public String getExtension () {

    return extension;
  }

  /**
   * Returns the MIME content type string.
   *
   * @return content type
   */
  public String getContentType () {

    return contentType;
  }
}
