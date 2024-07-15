/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.command.ArithmeticCommand;
import org.smallmind.memcached.cubby.command.ArithmeticMode;
import org.smallmind.memcached.cubby.command.GetCommand;
import org.smallmind.memcached.cubby.command.NoopCommand;
import org.smallmind.memcached.cubby.command.Result;
import org.smallmind.memcached.cubby.command.SetCommand;
import org.smallmind.memcached.cubby.command.SetMode;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.smallmind.scribe.pen.TestLoggerConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "experimental")
public class CubbyTest {

  private static final String LARGE_KEY = "0123456789".repeat(30);
  private static final String LARGE_VALUE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".repeat(650);

  private final CubbyConfiguration configuration = CubbyConfiguration.OPTIMAL;
  private CubbyMemcachedClient client;
  private Long setCas;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();

    client = new CubbyMemcachedClient(configuration, new MemcachedHost("0", "localhost", 11211));
    client.start();

    client.delete("second");
    client.delete("third");
    client.delete("fourth");
    client.delete("fifth");
    client.delete("sixth");
    client.delete("seventh");
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    client.stop();
  }

  @Test
  public void testNoop ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new NoopCommand().setKey("none"), null);
    Assert.assertEquals(response.getCode(), ResponseCode.MN);
  }

  @Test
  public void testBasicSetWithOpaqueToken ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("first").setValue(configuration.getCodec().serialize("value1")).setOpaqueToken("opaque"), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(response.getToken(), "opaque");
  }

  @Test(dependsOnMethods = "testBasicSetWithOpaqueToken")
  public void testBasicGet ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = client.send(command = new GetCommand().setKey("first").setValue(true).setCas(true), null);
    Result result = command.process(response);

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(configuration.getCodec().deserialize(result.getValue()), "value1");
  }

  @Test
  public void testCasSet ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("second").setValue(configuration.getCodec().serialize("value2")).setCas(0L), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertTrue(response.getCas() > 0);
    setCas = response.getCas();
  }

  @Test(dependsOnMethods = "testCasSet")
  public void testCasGet ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = client.send(command = new GetCommand().setKey("second").setValue(true).setCas(true), null);
    Result result = command.process(response);

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(configuration.getCodec().deserialize(result.getValue()), "value2");
    Assert.assertEquals(result.getCas(), setCas);
  }

  @Test(dependsOnMethods = "testBasicSetWithOpaqueToken")
  public void testAdd ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("first").setValue(configuration.getCodec().serialize("value3")).setMode(SetMode.ADD), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("third").setValue(configuration.getCodec().serialize("value3")).setMode(SetMode.ADD), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
  }

  @Test(dependsOnMethods = "testAdd")
  public void testReplace ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    Response response = client.send(new SetCommand().setKey("fourth").setValue(configuration.getCodec().serialize("value4")).setMode(SetMode.REPLACE), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("third").setValue(configuration.getCodec().serialize("value4")).setMode(SetMode.REPLACE), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(client.get("third"), "value4");
  }

  @Test(dependsOnMethods = "testReplace")
  public void testAppend ()
    throws InterruptedException, IOException, CubbyOperationException {

    client.send(new SetCommand().setKey("fourth").setValue("value4".getBytes(StandardCharsets.UTF_8)), null);

    Response response = client.send(new SetCommand().setKey("fifth").setValue("5".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.APPEND), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("fourth").setValue("5".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.APPEND), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);

    response = client.send(new GetCommand().setKey("fourth").setValue(true), null);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "value45");
  }

  @Test(dependsOnMethods = "testAppend")
  public void testPrepend ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("fifth").setValue("5".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.PREPEND), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("fourth").setValue("3".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.PREPEND), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);

    response = client.send(new GetCommand().setKey("fourth").setValue(true), null);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "3value45");
  }

  @Test
  public void testLargeSetAndGet ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Result result;
    Response response = client.send(new SetCommand().setKey(LARGE_KEY).setValue(configuration.getCodec().serialize(LARGE_VALUE)), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);

    response = client.send(command = new GetCommand().setKey(LARGE_KEY), null);
    result = command.process(response);

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(configuration.getCodec().deserialize(result.getValue()), LARGE_VALUE);
  }

  @Test
  public void testInitialIncrement ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new ArithmeticCommand().setKey("sixth").setInitial(2).setDelta(3), null);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "2");
  }

  @Test(dependsOnMethods = "testInitialIncrement")
  public void testIncrement ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new ArithmeticCommand().setKey("sixth").setInitial(11).setDelta(3), null);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "5");
  }

  @Test(dependsOnMethods = "testIncrement")
  public void testImplicitDecrement ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new ArithmeticCommand().setKey("sixth").setDelta(-2), null);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "3");
  }

  @Test(dependsOnMethods = "testImplicitDecrement")
  public void testExplicitDecrement ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new ArithmeticCommand().setKey("sixth").setMode(ArithmeticMode.DECREMENT).setDelta(2), null);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "1");
  }

  public void testIncrementWithCas ()
    throws InterruptedException, IOException, CubbyOperationException {

    long incrementCas;
    Response response = client.send(new ArithmeticCommand().setKey("seventh").setInitial(0).setDelta(3).setCas(0L), null);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "0");
    incrementCas = response.getCas();

    response = client.send(new ArithmeticCommand().setKey("seventh").setInitial(0).setDelta(8).setCas(incrementCas + 1), null);
    Assert.assertEquals(response.getCode(), ResponseCode.EX);

    response = client.send(new ArithmeticCommand().setKey("seventh").setInitial(0).setDelta(8).setCas(incrementCas), null);
    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(new String(response.getValue(), StandardCharsets.UTF_8), "8");
  }
}
