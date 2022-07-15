/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.net.InetSocketAddress;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.command.GetCommand;
import org.smallmind.memcached.cubby.command.RawCommand;
import org.smallmind.memcached.cubby.command.Result;
import org.smallmind.memcached.cubby.command.SetCommand;
import org.smallmind.memcached.cubby.command.SetMode;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.util.Strings;

@Test(groups = "experimental")
public class CubbyTest {

  private static final String LARGE_KEY = Strings.repeat("0123456789", 30);
  private static final String LARGE_VALUE = Strings.repeat("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 650);

  private final CubbyConfiguration configuration = CubbyConfiguration.OPTIMAL;
  private final CubbyCodec codec = configuration.getCodec();
  private CubbyMemcachedClient client;
  private Long setCas;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    client = new CubbyMemcachedClient(configuration, new MemcachedHost("0", new InetSocketAddress("localhost", 11211)));
    client.start();

    client.delete("second");
    client.delete("third");
    client.delete("fourth");
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    client.stop();
  }

  @Test
  public void testBasicSetWithOpaqueToken ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("first").setValue("value1").setOpaqueToken("opaque"), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(response.getToken(), "opaque");
  }

  @Test(dependsOnMethods = "testBasicSetWithOpaqueToken")
  public void testBasicGet ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = client.send(command = new GetCommand().setKey("first").setValue(true).setCas(true), null);
    Result<?> result = command.process(configuration.getCodec(), response);

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(result.getValue(), "value1");
  }

  @Test
  public void testCasSet ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("second").setValue("value2").setCas(0L), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertTrue(response.getCas() > 0);
    setCas = response.getCas();
  }

  @Test(dependsOnMethods = "testCasSet")
  public void testCasGet ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Response response = client.send(command = new GetCommand().setKey("second").setValue(true).setCas(true), null);
    Result<?> result = command.process(configuration.getCodec(), response);

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(result.getValue(), "value2");
    Assert.assertEquals(result.getCas(), setCas);
  }

  @Test(dependsOnMethods = "testBasicSetWithOpaqueToken")
  public void testAdd ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    Response response = client.send(new SetCommand().setKey("first").setValue("value3").setMode(SetMode.ADD), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("third").setValue("value3").setMode(SetMode.ADD), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
  }

  @Test(dependsOnMethods = "testAdd")
  public void testReplace ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    Response response = client.send(new SetCommand().setKey("fourth").setValue("value4").setMode(SetMode.REPLACE), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("third").setValue("value4").setMode(SetMode.REPLACE), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(client.get("third"), "value4");
  }

  @Test(dependsOnMethods = "testReplace")
  public void testAppend ()
    throws InterruptedException, IOException, CubbyOperationException {

    client.send(new SetCommand().setKey("fourth").setValue("value4".getBytes()).setRaw(true), null);

    Response response = client.send(new SetCommand().setKey("fifth").setValue("5").setMode(SetMode.APPEND), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("fourth").setValue("5".getBytes()).setMode(SetMode.APPEND).setRaw(true), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);

    response = client.send(new RawCommand().setKey("fourth").setValue(true), null);
    Assert.assertEquals(new String(response.getValue()), "value45");
  }

  @Test(dependsOnMethods = "testAppend")
  public void testPrepend ()
    throws InterruptedException, IOException, CubbyOperationException {

    Response response = client.send(new SetCommand().setKey("fifth").setValue("5").setMode(SetMode.PREPEND), null);
    Assert.assertEquals(response.getCode(), ResponseCode.NS);

    response = client.send(new SetCommand().setKey("fourth").setValue("3".getBytes()).setMode(SetMode.PREPEND).setRaw(true), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);

    response = client.send(new RawCommand().setKey("fourth").setValue(true), null);
    Assert.assertEquals(new String(response.getValue()), "3value45");
  }

  @Test
  public void testLargeSetAndGet ()
    throws InterruptedException, IOException, CubbyOperationException, ClassNotFoundException {

    GetCommand command;
    Result<?> result;
    Response response = client.send(new SetCommand().setKey(LARGE_KEY).setValue(LARGE_VALUE), null);
    Assert.assertEquals(response.getCode(), ResponseCode.HD);

    response = client.send(command = new GetCommand().setKey(LARGE_KEY), null);
    result = command.process(configuration.getCodec(), response);

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(result.getValue(), LARGE_VALUE);
  }
}
