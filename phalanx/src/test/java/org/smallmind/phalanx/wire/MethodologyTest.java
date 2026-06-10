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
package org.smallmind.phalanx.wire;

import java.lang.reflect.Method;
import org.smallmind.phalanx.wire.transport.ArgumentInfo;
import org.smallmind.phalanx.wire.transport.SyntheticArgument;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies how {@link Methodology} resolves a method's named arguments to their index and declared
 * type: from {@link Argument} annotations, from explicit {@link SyntheticArgument} overrides, and
 * the fail-fast guard when annotations do not cover every parameter.
 */
@Test(groups = "unit")
public class MethodologyTest {

  public interface Sample {

    int good (@Argument("a") int a, @Argument("b") String b);

    void single (@Argument("real") String s);

    void bad (String missing);
  }

  private Method method (String name, Class<?>... parameterTypes)
    throws NoSuchMethodException {

    return Sample.class.getMethod(name, parameterTypes);
  }

  @Test
  public void testAnnotatedArgumentsMapToIndexAndType ()
    throws NoSuchMethodException, ServiceDefinitionException {

    Methodology methodology = new Methodology(Sample.class, method("good", int.class, String.class));
    ArgumentInfo first = methodology.getArgumentInfo("a");
    ArgumentInfo second = methodology.getArgumentInfo("b");

    Assert.assertEquals(first.getIndex(), 0);
    Assert.assertEquals(first.getParameterType(), int.class);
    Assert.assertEquals(second.getIndex(), 1);
    Assert.assertEquals(second.getParameterType(), String.class);
  }

  @Test
  public void testUnknownArgumentNameReturnsNull ()
    throws NoSuchMethodException, ServiceDefinitionException {

    Assert.assertNull(new Methodology(Sample.class, method("good", int.class, String.class)).getArgumentInfo("missing"));
  }

  @Test
  public void testSyntheticArgumentsTakePrecedenceOverAnnotations ()
    throws NoSuchMethodException, ServiceDefinitionException {

    Methodology methodology = new Methodology(Sample.class, method("single", String.class), new SyntheticArgument("synthetic", Integer.class));

    Assert.assertNotNull(methodology.getArgumentInfo("synthetic"));
    Assert.assertEquals(methodology.getArgumentInfo("synthetic").getParameterType(), Integer.class);
    Assert.assertNull(methodology.getArgumentInfo("real"));
  }

  @Test(expectedExceptions = ServiceDefinitionException.class)
  public void testMissingArgumentAnnotationIsRejected ()
    throws NoSuchMethodException, ServiceDefinitionException {

    new Methodology(Sample.class, method("bad", String.class));
  }
}
