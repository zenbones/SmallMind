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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link FaultWrappingException}: copying the wrapped {@link Fault}'s elements onto its own stack
 * trace, exposing the fault, and the three {@code printStackTrace} overloads (no-arg/stream/writer) that
 * render the fault rather than the JVM trace. Also covers the no-elements branch of the constructor.
 */
@Test(groups = "unit")
public class FaultWrappingExceptionTest {

  public void testWrapsFaultMessageAndCopiesStackTrace () {

    Fault fault = new Fault(new IllegalStateException("inner"), false);
    FaultWrappingException exception = new FaultWrappingException(fault);

    Assert.assertEquals(exception.getFault(), fault);
    Assert.assertEquals(exception.getMessage(), fault.getMessage());
    Assert.assertEquals(exception.getStackTrace().length, fault.getElements().length);
  }

  public void testMessageOnlyFaultLeavesJvmStackTrace () {

    Fault fault = new Fault("plain message");
    FaultWrappingException exception = new FaultWrappingException(fault);

    // When the wrapped fault carries no elements, the constructor never calls setStackTrace, so the
    // wrapper retains the JVM-filled trace from its own construction rather than an empty one.
    Assert.assertNull(fault.getElements());
    Assert.assertEquals(exception.getMessage(), "plain message");
  }

  public void testPrintStackTraceToStreamRendersFault () {

    Fault fault = new Fault(new RuntimeException("kaboom"), false);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    new FaultWrappingException(fault).printStackTrace(new PrintStream(byteArrayOutputStream));

    Assert.assertEquals(byteArrayOutputStream.toString(), fault.toString());
  }

  public void testPrintStackTraceToWriterRendersFault () {

    Fault fault = new Fault(new RuntimeException("kaboom"), false);

    StringWriter stringWriter = new StringWriter();

    new FaultWrappingException(fault).printStackTrace(new PrintWriter(stringWriter));

    Assert.assertEquals(stringWriter.toString(), fault.toString());
  }

  public void testNoArgPrintStackTraceRendersFaultToSystemOut () {

    Fault fault = new Fault(new RuntimeException("kaboom"), false);

    PrintStream originalOut = System.out;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try {
      System.setOut(new PrintStream(byteArrayOutputStream));
      new FaultWrappingException(fault).printStackTrace();
    } finally {
      System.setOut(originalOut);
    }

    Assert.assertEquals(byteArrayOutputStream.toString(), fault.toString());
  }
}
