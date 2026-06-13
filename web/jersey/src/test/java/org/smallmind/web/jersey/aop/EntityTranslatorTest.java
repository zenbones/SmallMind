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
package org.smallmind.web.jersey.aop;

import java.lang.annotation.Annotation;
import org.glassfish.jersey.server.ContainerRequest;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.reflection.MissingAnnotationException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Covers the thread-local entity lifecycle of {@link EntityTranslator}: the missing-type and raw-interface guard
 * branches, lazy deserialization of the registered entity followed by delegated parameter lookup, and the cleanup
 * performed by {@link EntityTranslator#clearEntity}. State is reset before and after each method because the
 * coordinator holds its data in static thread-locals.
 */
@Test(groups = "unit")
public class EntityTranslatorTest {

  @BeforeMethod
  public void clearBefore () {

    EntityTranslator.clearEntity();
  }

  @AfterMethod
  public void clearAfter () {

    EntityTranslator.clearEntity();
  }

  private ParameterAnnotations plainAnnotations () {

    return new ParameterAnnotations(new Annotation[0]);
  }

  @Test(expectedExceptions = MissingAnnotationException.class)
  public void testMissingEntityTypeThrows () {

    EntityTranslator.getParameter(Mockito.mock(ContainerRequest.class), "key", String.class, plainAnnotations());
  }

  @Test(expectedExceptions = ParameterProcessingException.class)
  public void testRawJsonEntityTypeThrows () {

    EntityTranslator.storeEntityType(JsonEntity.class);
    EntityTranslator.getParameter(Mockito.mock(ContainerRequest.class), "key", String.class, plainAnnotations());
  }

  public void testDelegatesToDeserializedEntity () {

    Envelope envelope = new Envelope(new Argument("count", 42));
    ContainerRequest containerRequest = Mockito.mock(ContainerRequest.class);

    Mockito.when(containerRequest.readEntity(Envelope.class)).thenReturn(envelope);

    EntityTranslator.storeEntityType(Envelope.class);

    Assert.assertEquals(EntityTranslator.getParameter(containerRequest, "count", Integer.class, plainAnnotations()), Integer.valueOf(42));

    // The deserialized entity is cached, so a second lookup does not re-read the request body.
    Assert.assertNull(EntityTranslator.getParameter(containerRequest, "missing", String.class, plainAnnotations()));
    Mockito.verify(containerRequest, Mockito.times(1)).readEntity(Envelope.class);
  }

  public void testClearEntityResetsState () {

    EntityTranslator.storeEntityType(Envelope.class);
    EntityTranslator.clearEntity();

    try {
      EntityTranslator.getParameter(Mockito.mock(ContainerRequest.class), "key", String.class, plainAnnotations());
      Assert.fail("Expected a MissingAnnotationException after clearEntity");
    } catch (MissingAnnotationException missingAnnotationException) {
      Assert.assertNotNull(missingAnnotationException.getMessage());
    }
  }
}
