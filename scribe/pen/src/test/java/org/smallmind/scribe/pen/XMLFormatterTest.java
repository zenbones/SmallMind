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
package org.smallmind.scribe.pen;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link XMLFormatter}'s hand-rolled XML emission (it does not use JAXB): element selection,
 * CDATA wrapping, the message-to-throwable fallback, and the suppress-empty-element behavior. A
 * {@link NullTimestamp} and zero indent keep the output deterministic.
 */
@Test(groups = "unit")
public class XMLFormatterTest {

  private XMLFormatter formatter (boolean cdata, RecordElement... elements) {

    return new XMLFormatter(new NullTimestamp(), "\n", 0, cdata, elements);
  }

  public void testEmitsSelectedElements ()
    throws Exception {

    String xml = formatter(false, RecordElement.LOGGER_NAME, RecordElement.LEVEL, RecordElement.MESSAGE).format(new RecordFixture().setLoggerName("com.example.Svc").setLevel(Level.INFO).setMessage("hello"));

    Assert.assertTrue(xml.startsWith("<log-record>"));
    Assert.assertTrue(xml.contains("<logger>com.example.Svc</logger>"));
    Assert.assertTrue(xml.contains("<level>INFO</level>"));
    Assert.assertTrue(xml.contains("<message>hello</message>"));
  }

  public void testCdataWrapsMessage ()
    throws Exception {

    String xml = formatter(true, RecordElement.MESSAGE).format(new RecordFixture().setMessage("needs <escaping>"));

    Assert.assertTrue(xml.contains("<![CDATA[needs <escaping>]]>"));
  }

  public void testMessageFallsBackToThrowableMessage ()
    throws Exception {

    String xml = formatter(false, RecordElement.MESSAGE).format(new RecordFixture().setMessage(null).setThrown(new RuntimeException("boom")));

    Assert.assertTrue(xml.contains("<message>boom</message>"));
  }

  public void testStackTraceElementSuppressedWhenNoThrowable ()
    throws Exception {

    String xml = formatter(false, RecordElement.STACK_TRACE).format(new RecordFixture().setThrown(null));

    Assert.assertFalse(xml.contains("stack-trace"));
  }

  public void testStackTraceElementPresentWhenThrowable ()
    throws Exception {

    String xml = formatter(false, RecordElement.STACK_TRACE).format(new RecordFixture().setThrown(new RuntimeException("kaboom")));

    Assert.assertTrue(xml.contains("<stack-trace>"));
    Assert.assertTrue(xml.contains("kaboom"));
  }

  public void testMillisecondsElementRendersRawMillis ()
    throws Exception {

    String xml = formatter(false, RecordElement.MILLISECONDS).format(new RecordFixture().setMillis(1234L));

    Assert.assertTrue(xml.contains("<milliseconds>1234</milliseconds>"));
  }

  public void testDateElementRendersWhenTimestampSupplied ()
    throws Exception {

    String xml = new XMLFormatter(DateFormatTimestamp.getDefaultInstance(), "\n", 0, false, RecordElement.DATE).format(new RecordFixture().setMillis(0L));

    Assert.assertTrue(xml.contains("<date>"));
    Assert.assertTrue(xml.contains("</date>"));
  }

  public void testMessageElementSuppressedWhenMessageAndThrowableNull ()
    throws Exception {

    String xml = formatter(false, RecordElement.MESSAGE).format(new RecordFixture().setMessage(null).setThrown(null));

    Assert.assertFalse(xml.contains("<message>"));
  }

  public void testThreadElementRendersNameAndSuppressesAbsentId ()
    throws Exception {

    String xml = formatter(false, RecordElement.THREAD).format(new RecordFixture().setThreadName("worker-7"));

    Assert.assertTrue(xml.contains("<thread>"));
    Assert.assertTrue(xml.contains("<name>worker-7</name>"));
    Assert.assertFalse(xml.contains("<id>"));
  }

  public void testLoggerContextRendersWhenFilled ()
    throws Exception {

    LoggerContextFixture context = new LoggerContextFixture(true, "com.example.Svc", "doWork", "Svc.java", 42, false);
    String xml = formatter(false, RecordElement.LOGGER_CONTEXT).format(new RecordFixture().setLoggerContext(context));

    Assert.assertTrue(xml.contains("<context>"));
    Assert.assertTrue(xml.contains("<class>com.example.Svc</class>"));
    Assert.assertTrue(xml.contains("<method>doWork</method>"));
    Assert.assertTrue(xml.contains("<native>false</native>"));
    Assert.assertTrue(xml.contains("<line>42</line>"));
    Assert.assertTrue(xml.contains("<file>Svc.java</file>"));
  }

  public void testLoggerContextLineSuppressedForNativeMethod ()
    throws Exception {

    LoggerContextFixture context = new LoggerContextFixture(true, "com.example.Svc", "doWork", "Svc.java", 42, true);
    String xml = formatter(false, RecordElement.LOGGER_CONTEXT).format(new RecordFixture().setLoggerContext(context));

    Assert.assertTrue(xml.contains("<native>true</native>"));
    Assert.assertFalse(xml.contains("<line>"));
  }

  public void testLoggerContextSuppressedWhenUnfilled ()
    throws Exception {

    LoggerContextFixture context = new LoggerContextFixture(false, "com.example.Svc", "doWork", "Svc.java", 42, false);
    String xml = formatter(false, RecordElement.LOGGER_CONTEXT).format(new RecordFixture().setLoggerContext(context));

    Assert.assertFalse(xml.contains("<context>"));
  }

  public void testParametersElementRendersEachPair ()
    throws Exception {

    Parameter[] parameters = {new Parameter("requestId", "abc"), new Parameter("tenant", "acme")};
    String xml = formatter(false, RecordElement.PARAMETERS).format(new RecordFixture().setParameters(parameters));

    Assert.assertTrue(xml.contains("<parameters>"));
    Assert.assertTrue(xml.contains("<requestId>abc</requestId>"));
    Assert.assertTrue(xml.contains("<tenant>acme</tenant>"));
  }

  public void testParametersElementSuppressedWhenEmpty ()
    throws Exception {

    String xml = formatter(false, RecordElement.PARAMETERS).format(new RecordFixture().setParameters(new Parameter[0]));

    Assert.assertFalse(xml.contains("<parameters>"));
  }

  public void testIndentationScalesWithLevel ()
    throws Exception {

    String xml = new XMLFormatter(new NullTimestamp(), "\n", 2, false, RecordElement.LOGGER_NAME).format(new RecordFixture().setLoggerName("svc"));

    Assert.assertTrue(xml.contains("\n  <logger>svc</logger>"));
  }

  public void testChainedCauseRendersCausedByAndMoreAbbreviation ()
    throws Exception {

    String xml = formatter(false, RecordElement.STACK_TRACE).format(new RecordFixture().setThrown(chainedThrowable()));

    Assert.assertTrue(xml.contains("Caused by:"));
    Assert.assertTrue(xml.contains(" more"));
  }

  public void testCdataWrapsStackTrace ()
    throws Exception {

    String xml = formatter(true, RecordElement.STACK_TRACE).format(new RecordFixture().setThrown(new RuntimeException("wrapped")));

    Assert.assertTrue(xml.contains("<stack-trace><![CDATA["));
    Assert.assertTrue(xml.contains("]]></stack-trace>"));
  }

  public void testDefaultConstructorProducesWellFormedEnvelope ()
    throws Exception {

    String xml = new XMLFormatter().format(new RecordFixture().setLoggerName("svc").setMessage("hi"));

    Assert.assertTrue(xml.startsWith("<log-record>"));
    Assert.assertTrue(xml.contains("</log-record>"));
  }

  private Throwable chainedThrowable () {

    try {
      try {
        throw new IllegalStateException("inner");
      } catch (IllegalStateException innerException) {
        throw new RuntimeException("outer", innerException);
      }
    } catch (RuntimeException outerException) {

      return outerException;
    }
  }
}
