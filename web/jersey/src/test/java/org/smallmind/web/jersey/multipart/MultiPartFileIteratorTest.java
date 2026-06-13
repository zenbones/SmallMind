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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link MultiPartFileIterator} with mocked {@code file} body parts to verify iteration, the explicit
 * media-type path, the filename-extension inference fallback, the {@code application/octet-stream} default, and the
 * missing-{@code file}-field guard.
 */
@Test(groups = "unit")
public class MultiPartFileIteratorTest {

  /**
   * Builds a real {@link FormDataBodyPart} carrying the supplied content disposition and overriding {@code getMediaType}
   * and the worker-backed {@code getValueAs} so the iterator can be exercised without a JAX-RS runtime. A plain
   * Mockito mock cannot stand in here because the body part's self-referencing accessors run real code during stubbing.
   */
  private FormDataBodyPart bodyPart (String fileName, MediaType mediaType, byte[] content)
    throws Exception {

    FormDataContentDisposition contentDisposition = FormDataContentDisposition.name("file").fileName(fileName).build();

    FormDataBodyPart formDataBodyPart = new FormDataBodyPart(contentDisposition, "ignored") {

      @Override
      public MediaType getMediaType () {

        return mediaType;
      }

      @Override
      public <T> T getValueAs (Class<T> clazz) {

        return clazz.cast(new ByteArrayInputStream(content));
      }
    };

    return formDataBodyPart;
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMissingFileFieldThrows () {

    FormDataMultiPart formDataMultiPart = Mockito.mock(FormDataMultiPart.class);

    Mockito.when(formDataMultiPart.getFields("file")).thenReturn(null);

    new MultiPartFileIterator(formDataMultiPart);
  }

  public void testExplicitMediaTypeIsUsed ()
    throws Exception {

    FormDataMultiPart formDataMultiPart = Mockito.mock(FormDataMultiPart.class);

    Mockito.when(formDataMultiPart.getFields("file")).thenReturn(Arrays.asList(bodyPart("report.bin", MediaType.APPLICATION_OCTET_STREAM_TYPE, new byte[] {1, 2, 3})));

    MultiPartFileIterator iterator = new MultiPartFileIterator(formDataMultiPart);

    Assert.assertTrue(iterator.hasNext());

    MultiPartFile multiPartFile = iterator.next();

    Assert.assertEquals(multiPartFile.getFileName(), "report.bin");
    Assert.assertEquals(multiPartFile.getContentType(), MediaType.APPLICATION_OCTET_STREAM);
    Assert.assertFalse(iterator.hasNext());
  }

  public void testContentTypeInferredFromExtension ()
    throws Exception {

    FormDataMultiPart formDataMultiPart = Mockito.mock(FormDataMultiPart.class);

    Mockito.when(formDataMultiPart.getFields("file")).thenReturn(Arrays.asList(bodyPart("logo.png", null, new byte[] {9})));

    MultiPartFile multiPartFile = new MultiPartFileIterator(formDataMultiPart).next();

    Assert.assertEquals(multiPartFile.getContentType(), "image/png");
  }

  public void testUnknownExtensionFallsBackToOctetStream ()
    throws Exception {

    FormDataMultiPart formDataMultiPart = Mockito.mock(FormDataMultiPart.class);

    Mockito.when(formDataMultiPart.getFields("file")).thenReturn(Arrays.asList(bodyPart("mystery.zzz", null, new byte[] {0})));

    MultiPartFile multiPartFile = new MultiPartFileIterator(formDataMultiPart).next();

    Assert.assertEquals(multiPartFile.getContentType(), "application/octet-stream");
  }

  public void testNoExtensionFallsBackToOctetStream ()
    throws Exception {

    FormDataMultiPart formDataMultiPart = Mockito.mock(FormDataMultiPart.class);

    Mockito.when(formDataMultiPart.getFields("file")).thenReturn(Arrays.asList(bodyPart("noextension", null, new byte[] {0})));

    MultiPartFile multiPartFile = new MultiPartFileIterator(formDataMultiPart).next();

    Assert.assertEquals(multiPartFile.getContentType(), "application/octet-stream");
  }
}
