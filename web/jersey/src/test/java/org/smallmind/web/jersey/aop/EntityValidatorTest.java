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

import java.lang.reflect.Method;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link EntityValidator} against a constrained method, verifying that conforming arguments and return values
 * pass while violations raise an {@link EntityValidationException} whose message names the offending parameter.
 */
@Test(groups = "unit")
public class EntityValidatorTest {

  public static class Service {

    @NotNull
    public String handle (@NotNull @EntityParam("name") String name, @Min(1) int count) {

      return name + count;
    }
  }

  private Method handleMethod ()
    throws Exception {

    return Service.class.getMethod("handle", String.class, int.class);
  }

  public void testValidParametersPass ()
    throws Exception {

    EntityValidator.validateParameters(new Service(), handleMethod(), new Object[] {"ok", 5});
  }

  public void testInvalidParametersThrow ()
    throws Exception {

    try {
      EntityValidator.validateParameters(new Service(), handleMethod(), new Object[] {null, 0});
      Assert.fail("Expected an EntityValidationException");
    } catch (EntityValidationException entityValidationException) {
      Assert.assertNotNull(entityValidationException.getMessage());
    }
  }

  public void testValidReturnValuePasses ()
    throws Exception {

    EntityValidator.validateReturnValue(new Service(), handleMethod(), "result");
  }

  @Test(expectedExceptions = EntityValidationException.class)
  public void testInvalidReturnValueThrows ()
    throws Exception {

    EntityValidator.validateReturnValue(new Service(), handleMethod(), null);
  }
}
