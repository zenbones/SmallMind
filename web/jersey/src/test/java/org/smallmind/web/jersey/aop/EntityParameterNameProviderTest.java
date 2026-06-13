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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link EntityParameterNameProvider}, verifying that {@code @EntityParam} values become parameter names while
 * unannotated parameters fall back to a positional {@code argument[n]} name, for both methods and constructors.
 */
@Test(groups = "unit")
public class EntityParameterNameProviderTest {

  public static class Target {

    public Target (@EntityParam("ctorArg") String first, int second) {

    }

    public void mixed (@EntityParam("named") String first, String unnamed) {

    }
  }

  public void testMethodNames ()
    throws Exception {

    Method method = Target.class.getMethod("mixed", String.class, String.class);
    List<String> names = new EntityParameterNameProvider().getParameterNames(method);

    Assert.assertEquals(names.get(0), "named");
    Assert.assertEquals(names.get(1), "argument[1]");
  }

  public void testConstructorNames ()
    throws Exception {

    Constructor<Target> constructor = Target.class.getConstructor(String.class, int.class);
    List<String> names = new EntityParameterNameProvider().getParameterNames(constructor);

    Assert.assertEquals(names.get(0), "ctorArg");
    Assert.assertEquals(names.get(1), "argument[1]");
  }
}
