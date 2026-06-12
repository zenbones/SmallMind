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
package org.smallmind.scribe.pen.fluentbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContextFixture;
import org.smallmind.scribe.pen.NullTimestamp;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.RecordElement;
import org.smallmind.scribe.pen.RecordFixture;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link MessagePackFormatter} record-to-{@code ObjectNode} mapping (the msgpack
 * serialization itself happens later in the appender). Exercises the Jackson-2 path supplied
 * transitively by the optional {@code jackson-dataformat-msgpack} dependency.
 */
@Test(groups = "unit")
public class MessagePackFormatterTest {

  public void testMapsScalarFields () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.LEVEL, RecordElement.MESSAGE, RecordElement.LOGGER_NAME}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setLevel(Level.WARN).setMessage("hello").setLoggerName("com.example.Svc"));

    Assert.assertEquals(node.get("level").asText(), "WARN");
    Assert.assertEquals(node.get("message").asText(), "hello");
    Assert.assertEquals(node.get("logger").asText(), "com.example.Svc");
  }

  public void testParametersBecomeNestedObject () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.PARAMETERS}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setParameters(new Parameter[] {new Parameter("tenant", "acme")}));

    Assert.assertTrue(node.has("parameters"));
    Assert.assertEquals(node.get("parameters").get("tenant").asText(), "acme");
  }

  public void testNullParameterValueBecomesNullNode () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.PARAMETERS}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setParameters(new Parameter[] {new Parameter("tenant", null)}));

    Assert.assertTrue(node.get("parameters").get("tenant").isNull());
  }

  public void testDateAndMillisecondsFields () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.DATE, RecordElement.MILLISECONDS}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setMillis(1234L));

    Assert.assertTrue(node.has("date"));
    Assert.assertEquals(node.get("milliseconds").asLong(), 1234L);
  }

  public void testThreadInfoRendersNameAndSuppressesAbsentId () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.THREAD}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setThreadName("worker-7"));

    Assert.assertEquals(node.get("thread").get("name").asText(), "worker-7");
    Assert.assertFalse(node.get("thread").has("id"));
  }

  public void testLoggerContextRendersWhenFilled () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.LOGGER_CONTEXT}, "\n");
    LoggerContextFixture context = new LoggerContextFixture(true, "com.example.Svc", "doWork", "Svc.java", 42, false);

    ObjectNode node = formatter.format(new RecordFixture().setLoggerContext(context));

    Assert.assertEquals(node.get("context").get("class").asText(), "com.example.Svc");
    Assert.assertEquals(node.get("context").get("method").asText(), "doWork");
    Assert.assertEquals(node.get("context").get("line").asInt(), 42);
    Assert.assertFalse(node.get("context").get("native").asBoolean());
  }

  public void testLoggerContextLineSuppressedForNativeMethod () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.LOGGER_CONTEXT}, "\n");
    LoggerContextFixture context = new LoggerContextFixture(true, "com.example.Svc", "doWork", "Svc.java", 42, true);

    ObjectNode node = formatter.format(new RecordFixture().setLoggerContext(context));

    Assert.assertTrue(node.get("context").get("native").asBoolean());
    Assert.assertFalse(node.get("context").has("line"));
  }

  public void testLoggerContextSuppressedWhenUnfilled () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.LOGGER_CONTEXT}, "\n");
    LoggerContextFixture context = new LoggerContextFixture(false, "com.example.Svc", "doWork", "Svc.java", 42, false);

    ObjectNode node = formatter.format(new RecordFixture().setLoggerContext(context));

    Assert.assertFalse(node.has("context"));
  }

  public void testMessageFallsBackToThrowableMessage () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.MESSAGE}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setMessage(null).setThrown(new RuntimeException("boom")));

    Assert.assertEquals(node.get("message").asText(), "boom");
  }

  public void testStackTraceRendersChainedCause () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), new RecordElement[] {RecordElement.STACK_TRACE}, "\n");

    ObjectNode node = formatter.format(new RecordFixture().setThrown(chainedThrowable()));

    Assert.assertTrue(node.has("stackTrace"));

    String trace = node.get("stackTrace").asText();
    Assert.assertTrue(trace.contains("Caused by:"));
    Assert.assertTrue(trace.contains(" more"));
  }

  public void testAllElementsConstructorProducesPopulatedNode () {

    MessagePackFormatter formatter = new MessagePackFormatter(new NullTimestamp(), RecordElement.values(), "\n");

    ObjectNode node = formatter.format(new RecordFixture().setLevel(Level.INFO).setMessage("full").setLoggerName("svc"));

    Assert.assertEquals(node.get("level").asText(), "INFO");
    Assert.assertEquals(node.get("message").asText(), "full");
    Assert.assertEquals(node.get("logger").asText(), "svc");
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
