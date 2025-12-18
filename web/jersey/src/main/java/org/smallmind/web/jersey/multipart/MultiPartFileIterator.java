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

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

/**
 * Iterator that yields {@link MultiPartFile} instances from a {@link FormDataMultiPart} request.
 */
public class MultiPartFileIterator implements Iterator<MultiPartFile> {

  private final Iterator<FormDataBodyPart> bodyPartIter;

  /**
   * Creates an iterator for the multipart form, expecting parts named {@code file}.
   *
   * @param formDataMultiPart multipart form payload
   * @throws IllegalArgumentException if no file fields are present
   */
  public MultiPartFileIterator (FormDataMultiPart formDataMultiPart) {

    List<FormDataBodyPart> bodyPartList;

    if ((bodyPartList = formDataMultiPart.getFields("file")) == null) {
      throw new IllegalArgumentException("You must specify a field name of 'file'");
    }

    bodyPartIter = bodyPartList.iterator();
  }

  /**
   * Indicates whether more file parts remain.
   *
   * @return {@code true} if additional parts are available
   */
  @Override
  public boolean hasNext () {

    return bodyPartIter.hasNext();
  }

  /**
   * Returns the next multipart file, inferring content type when missing.
   *
   * @return next multipart file
   */
  @Override
  public MultiPartFile next () {

    FormDataBodyPart bodyPart = bodyPartIter.next();

    return new MultiPartFile(bodyPart.getContentDisposition().getFileName(), getContentType(bodyPart), bodyPart.getValueAs(InputStream.class));
  }

  /**
   * Resolves content type from the body part or infers it from the filename when absent.
   *
   * @param bodyPart multipart body part
   * @return MIME type string
   */
  private String getContentType (BodyPart bodyPart) {

    if (bodyPart.getMediaType() != null) {

      return bodyPart.getMediaType().toString();
    } else {

      String fileName;
      int lastPeriodPos;

      if ((lastPeriodPos = (fileName = bodyPart.getContentDisposition().getFileName()).lastIndexOf('.')) >= 0) {

        MultiPartContentType multiPartContentType;

        if ((multiPartContentType = MultiPartContentType.forExtension(fileName.substring(lastPeriodPos + 1))) != null) {

          return multiPartContentType.getContentType();
        }
      }

      return "application/octet-stream";
    }
  }
}
