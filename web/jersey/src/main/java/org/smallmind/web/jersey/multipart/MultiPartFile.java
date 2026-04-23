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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Value object representing a single file from a multipart form upload, providing access to its name, content type,
 * and data stream.
 */
public class MultiPartFile {

  private final InputStream inputStream;
  private final String fileName;
  private final String contentType;

  /**
   * Constructs a multipart file wrapper with the given metadata and data stream.
   *
   * @param fileName    the original filename from the content-disposition header
   * @param contentType the MIME type of the file
   * @param inputStream the stream containing the file data
   */
  public MultiPartFile (String fileName, String contentType, InputStream inputStream) {

    this.fileName = fileName;
    this.contentType = contentType;
    this.inputStream = inputStream;
  }

  /**
   * Returns the original filename supplied in the content-disposition header.
   *
   * @return the file name
   */
  public String getFileName () {

    return fileName;
  }

  /**
   * Returns the MIME content type of the uploaded file.
   *
   * @return the content type string
   */
  public String getContentType () {

    return contentType;
  }

  /**
   * Returns the raw input stream for reading file data.
   *
   * @return the file data input stream
   */
  public InputStream getInputStream () {

    return inputStream;
  }

  /**
   * Reads the entire file content from the input stream into a byte array.
   *
   * @return all bytes in the file
   * @throws MultiPartFileException if an I/O error occurs while reading the stream
   */
  public byte[] readAllBytes () {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try {

      byte[] buffer = new byte[1024];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) >= 0) {
        byteArrayOutputStream.write(buffer, 0, bytesRead);
      }

      byteArrayOutputStream.close();
    } catch (IOException ioException) {
      throw new MultiPartFileException(ioException);
    }

    return byteArrayOutputStream.toByteArray();
  }
}
