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
package org.smallmind.phalanx.wire.signal;

import java.lang.reflect.Method;
import org.smallmind.phalanx.wire.Argument;
import org.smallmind.phalanx.wire.CallAs;
import org.smallmind.phalanx.wire.Result;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises how {@link Function} descriptors are derived from reflected methods (name overrides,
 * inferred and annotated type tags) and the {@code equals}/{@code hashCode} contract that lets a
 * {@code Function} serve as a method-map key during server-side dispatch.
 */
@Test(groups = "unit")
public class FunctionTest {

  public interface Sample {

    @CallAs("renamed")
    int alpha (@Argument("a") int a);

    String beta (@Argument("s") String s);

    @Result("CustomResult")
    Object gamma ();

    void delta (@Argument(value = "c", type = "Hint") String c);
  }

  private Method method (String name, Class<?>... parameterTypes)
    throws NoSuchMethodException {

    return Sample.class.getMethod(name, parameterTypes);
  }

  @Test
  public void testNameDefaultsToMethodName ()
    throws NoSuchMethodException {

    Assert.assertEquals(new Function(method("beta", String.class)).getName(), "beta");
  }

  @Test
  public void testCallAsOverridesName ()
    throws NoSuchMethodException {

    Assert.assertEquals(new Function(method("alpha", int.class)).getName(), "renamed");
  }

  @Test
  public void testInferredSignatureAndReturnEncoding ()
    throws NoSuchMethodException {

    Function function = new Function(method("beta", String.class));

    Assert.assertEquals(function.getResultType(), "G");
    Assert.assertEquals(function.getSignature(), new String[] {"G"});
    Assert.assertEquals(function.getNativeType(), "Ljava/lang/String;");
    Assert.assertFalse(function.isPartial());
  }

  @Test
  public void testPrimitiveSignatureEncoding ()
    throws NoSuchMethodException {

    Function function = new Function(method("alpha", int.class));

    Assert.assertEquals(function.getResultType(), "I");
    Assert.assertEquals(function.getSignature(), new String[] {"I"});
    Assert.assertEquals(function.getNativeType(), "I");
  }

  @Test
  public void testResultAnnotationOverridesReturnType ()
    throws NoSuchMethodException {

    Assert.assertEquals(new Function(method("gamma")).getResultType(), "!CustomResult");
  }

  @Test
  public void testArgumentTypeHintOverridesInferredSignature ()
    throws NoSuchMethodException {

    Assert.assertEquals(new Function(method("delta", String.class)).getSignature(), new String[] {"!Hint"});
  }

  @Test
  public void testNameOnlyDescriptorIsPartial () {

    Assert.assertTrue(new Function("beta").isPartial());
  }

  @Test
  public void testEqualityAndHashCodeForMapKeyUse ()
    throws NoSuchMethodException {

    Function first = new Function(method("alpha", int.class));
    Function second = new Function(method("alpha", int.class));
    Function other = new Function(method("beta", String.class));

    Assert.assertEquals(first, second);
    Assert.assertEquals(first.hashCode(), second.hashCode());
    Assert.assertNotEquals(first, other);
  }
}
