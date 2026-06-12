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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import org.productivity.java.syslog4j.Syslog;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SyslogAppenderTest {

  private DatagramSocket serverSocket;
  private int serverPort;

  @BeforeMethod
  public void openServerSocket ()
    throws Exception {

    serverSocket = new DatagramSocket(0, InetAddress.getByName("localhost"));
    serverSocket.setSoTimeout(2000);
    serverPort = serverSocket.getLocalPort();
  }

  @AfterMethod
  public void closeServerSocket () {

    if (serverSocket != null) {
      serverSocket.close();
    }

    // SyslogAppender registers a process-wide syslog4j protocol named "logging"; it must be torn down
    // between tests or the next afterPropertiesSet() fails with "already defined". syslog4j 0.9.30 only
    // offers a registry-wide shutdown, which is exactly what is wanted here.
    if (Syslog.exists("logging")) {
      Syslog.shutdown();
    }
  }

  public void testSettersAndGettersRoundTrip () {

    SyslogAppender appender = new SyslogAppender();

    appender.setSyslogHost("syslog.example.com");
    appender.setSyslogPort(9514);
    appender.setFacility("LOCAL3");
    appender.setBase64EncodeStackTraces(true);

    Assert.assertEquals(appender.getSyslogHost(), "syslog.example.com");
    Assert.assertEquals(appender.getSyslogPort(), 9514);
    Assert.assertEquals(appender.getFacility(), "LOCAL3");
    Assert.assertTrue(appender.isBase64EncodeStackTraces());
  }

  public void testHandleOutputTransmitsDatagramOverUdp ()
    throws Exception {

    SyslogAppender appender = new SyslogAppender();

    appender.setSyslogHost("localhost");
    appender.setSyslogPort(serverPort);
    appender.afterPropertiesSet();

    appender.handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("syslog-capture-marker").setMillis(System.currentTimeMillis()));

    byte[] buffer = new byte[8192];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    serverSocket.receive(packet);

    String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

    Assert.assertTrue(packet.getLength() > 0);
    Assert.assertTrue(received.contains("syslog-capture-marker"), "Datagram payload was: " + received);
  }

  private SyslogAppender boundAppender () {

    SyslogAppender appender = new SyslogAppender();

    appender.setSyslogHost("localhost");
    appender.setSyslogPort(serverPort);
    appender.afterPropertiesSet();

    return appender;
  }

  private String receive ()
    throws Exception {

    byte[] buffer = new byte[8192];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    serverSocket.receive(packet);

    return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
  }

  public void testSeverityMappingTransmitsAtEveryActiveLevel ()
    throws Exception {

    SyslogAppender appender = boundAppender();

    for (Level level : new Level[] {Level.FATAL, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE}) {

      String marker = "severity-" + level.name();

      appender.handleOutput(new RecordFixture().setLevel(level).setMessage(marker).setMillis(System.currentTimeMillis()));
      Assert.assertTrue(receive().contains(marker), "missing datagram for level " + level);
    }
  }

  public void testOffLevelTransmitsNothing ()
    throws Exception {

    SyslogAppender appender = boundAppender();

    appender.handleOutput(new RecordFixture().setLevel(Level.OFF).setMessage("never-sent").setMillis(System.currentTimeMillis()));

    serverSocket.setSoTimeout(400);
    try {
      receive();
      Assert.fail("an OFF-level record must not produce a datagram");
    } catch (SocketTimeoutException socketTimeoutException) {
      // expected — nothing was transmitted
    }
  }

  public void testPlainStackTraceIsTransmitted ()
    throws Exception {

    SyslogAppender appender = boundAppender();

    appender.handleOutput(new RecordFixture().setLevel(Level.ERROR).setMessage("with-trace").setThrown(new RuntimeException("kaboom")).setMillis(System.currentTimeMillis()));

    // syslog4j caps the datagram length, so the trailing MSG field can be truncated after a long
    // stack-trace structured-data element; assert on the structured data, which precedes it.
    String received = receive();
    Assert.assertTrue(received.contains("stack-trace"), "Datagram payload was: " + received);
    Assert.assertTrue(received.contains("kaboom"), "Datagram payload was: " + received);
  }

  public void testBase64StackTraceIsTransmitted ()
    throws Exception {

    SyslogAppender appender = boundAppender();

    appender.setBase64EncodeStackTraces(true);
    appender.handleOutput(new RecordFixture().setLevel(Level.ERROR).setMessage("encoded-trace").setThrown(new RuntimeException("kaboom")).setMillis(System.currentTimeMillis()));

    // The base64 value is opaque and long enough that the trailing MSG field is truncated, so assert
    // only on the structured-data key that carries the encoded trace.
    String received = receive();
    Assert.assertTrue(received.contains("stack-trace"), "Datagram payload was: " + received);
  }

  public void testLoggerContextAndParametersAreTransmitted ()
    throws Exception {

    SyslogAppender appender = boundAppender();
    LoggerContextFixture context = new LoggerContextFixture(true, "com.example.Svc", "doWork", "Svc.java", 42, false);
    Parameter[] parameters = {new Parameter("requestId", "abc-123")};

    appender.handleOutput(new RecordFixture().setLevel(Level.INFO).setMessage("rich-record").setLoggerContext(context).setParameters(parameters).setMillis(System.currentTimeMillis()));

    String received = receive();
    Assert.assertTrue(received.contains("rich-record"));
    Assert.assertTrue(received.contains("com.example.Svc"));
    Assert.assertTrue(received.contains("requestId"));
  }
}
