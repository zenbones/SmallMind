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
package org.smallmind.web.json.doppelganger;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the marker types and {@link DefinitionException} formatting.
 */
@Test(groups = "unit")
public class MarkerAndExceptionTest {

  public void testNullXmlAdapterUnmarshalThrows () {

    NullXmlAdapter adapter = new NullXmlAdapter();

    Assert.assertThrows(UnsupportedOperationException.class, () -> adapter.unmarshal(new Object()));
  }

  public void testNullXmlAdapterMarshalThrows () {

    NullXmlAdapter adapter = new NullXmlAdapter();

    Assert.assertThrows(UnsupportedOperationException.class, () -> adapter.marshal(new Object()));
  }

  public void testNotPolymorphicInstantiable () {

    Assert.assertNotNull(new NotPolymorphic());
  }

  public void testDefinitionExceptionFormatsMessage () {

    DefinitionException definitionException = new DefinitionException("The purpose(%s) must be a legal identifier fragment", "bad value");

    Assert.assertEquals(definitionException.getMessage(), "The purpose(bad value) must be a legal identifier fragment");
  }

  public void testDefinitionExceptionWrapsCause () {

    IllegalStateException cause = new IllegalStateException("boom");
    DefinitionException definitionException = new DefinitionException(cause, "wrapped(%d)", 7);

    Assert.assertEquals(definitionException.getMessage(), "wrapped(7)");
    Assert.assertSame(definitionException.getCause(), cause);
  }
}
