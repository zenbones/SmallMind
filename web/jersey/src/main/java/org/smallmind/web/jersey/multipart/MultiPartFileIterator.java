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
 * Iterator over the {@code file} fields of a {@link FormDataMultiPart} request, yielding a {@link MultiPartFile}
 * for each part.
 */
public class MultiPartFileIterator implements Iterator<MultiPartFile> {

  private final Iterator<FormDataBodyPart> bodyPartIter;

  /**
   * Constructs an iterator over all parts named {@code file} in the given multipart form.
   *
   * @param formDataMultiPart the multipart form payload containing the file fields
   * @throws IllegalArgumentException if no field named {@code file} is present in the form
   */
  public MultiPartFileIterator (FormDataMultiPart formDataMultiPart) {

    List<FormDataBodyPart> bodyPartList;

    if ((bodyPartList = formDataMultiPart.getFields("file")) == null) {
      throw new IllegalArgumentException("You must specify a field name of 'file'");
    }

    bodyPartIter = bodyPartList.iterator();
  }

  /**
   * Returns {@code true} if there are more file parts to iterate over.
   *
   * @return {@code true} if another file part is available
   */
  @Override
  public boolean hasNext () {

    return bodyPartIter.hasNext();
  }

  /**
   * Returns the next {@link MultiPartFile}, inferring the content type from the filename extension when the part
   * carries no media type.
   *
   * @return the next multipart file
   */
  @Override
  public MultiPartFile next () {

    FormDataBodyPart bodyPart = bodyPartIter.next();

    return new MultiPartFile(bodyPart.getContentDisposition().getFileName(), getContentType(bodyPart), bodyPart.getValueAs(InputStream.class));
  }

  /**
   * Resolves the content type for a body part, falling back to extension-based inference and then
   * {@code application/octet-stream} when no media type is available.
   *
   * @param bodyPart the multipart body part whose content type is needed
   * @return the resolved MIME type string
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
