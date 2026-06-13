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
package org.smallmind.web.json.scaffold.fault;

import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Covers the transportable {@link Fault} model built from a {@link Throwable}: captured type, message,
 * stack frames, nested cause chain, {@link Informed} supplemental data, and the human-readable
 * {@code toString} rendering.
 *
 * <p>The no-native-encoding constructor is used where the throwable carries non-serializable state
 * (the {@code Informed} {@link ObjectNode}), since native encoding would otherwise attempt to
 * Java-serialize it.
 */
@Test(groups = "unit")
public class FaultTest {

  public void testMessageOnly () {

    Assert.assertEquals(new Fault("oops").getMessage(), "oops");
  }

  public void testFromThrowableCapturesTypeMessageAndStack () {

    Fault fault = new Fault(new IllegalStateException("bad state"), false);

    Assert.assertEquals(fault.getThrowableType(), "java.lang.IllegalStateException");
    Assert.assertEquals(fault.getMessage(), "bad state");
    Assert.assertNotNull(fault.getElements());
    Assert.assertTrue(fault.getElements().length > 0);
  }

  public void testCauseChainIsCaptured () {

    Fault fault = new Fault(new RuntimeException("outer", new IllegalArgumentException("inner")), false);

    Assert.assertNotNull(fault.getCause());
    Assert.assertEquals(fault.getCause().getThrowableType(), "java.lang.IllegalArgumentException");
    Assert.assertEquals(fault.getCause().getMessage(), "inner");
  }

  public void testNativeEncodingOnSerializableThrowable () {

    // Default constructor includes native encoding; a standard exception serializes cleanly.
    Fault fault = new Fault(new RuntimeException("boom"));

    Assert.assertEquals(fault.getMessage(), "boom");
  }

  public void testInformedInformationCaptured () {

    ObjectNode info = JsonNodeFactory.instance.objectNode();
    info.put("code", 42);

    Fault fault = new Fault(new InformedException("annotated", info), false);

    Assert.assertEquals(fault.getInformation(), info);
  }

  public void testToStringContainsMessage () {

    Assert.assertTrue(new Fault(new RuntimeException("kaboom"), false).toString().contains("kaboom"));
  }

  private static class InformedException extends RuntimeException implements Informed {

    private final ObjectNode information;

    private InformedException (String message, ObjectNode information) {

      super(message);

      this.information = information;
    }

    @Override
    public ObjectNode getInformation () {

      return information;
    }
  }
}
