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
package org.smallmind.memcached.cubby.command;

import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.UnexpectedResponseException;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ArithmeticCommandTest {

  private static final KeyTranslator IDENTITY = key -> key;

  private static String headerOf (byte[] commandBytes) {

    String text = new String(commandBytes, StandardCharsets.UTF_8);
    int crlf = text.indexOf("\r\n");

    return text.substring(0, crlf);
  }

  public void testDefaultIncrementEmitsMiAndDeltaFlags ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setDelta(3);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.startsWith("ma k"), header);
    Assert.assertTrue(header.contains(" MI"), header);
    Assert.assertTrue(header.contains(" D3"), header);
    Assert.assertTrue(header.contains(" v"), header);
  }

  public void testExplicitDecrementEmitsMdFlag ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setMode(ArithmeticMode.DECREMENT).setDelta(2);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" MD"), header);
    Assert.assertTrue(header.contains(" D2"), header);
  }

  public void testNegativeDeltaFlipsIncrementIntoDecrement ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setDelta(-4);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" MD"), header);
    Assert.assertTrue(header.contains(" D4"), header);
    Assert.assertFalse(header.contains(" D-"), header);
  }

  public void testNegativeDeltaFlipsDecrementIntoIncrement ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setMode(ArithmeticMode.DECREMENT).setDelta(-5);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" MI"), header);
    Assert.assertTrue(header.contains(" D5"), header);
  }

  public void testInitialSeedWithoutExpirationEmitsJAndNZeroFlags ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setInitial(10).setDelta(1);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" J10"), header);
    Assert.assertTrue(header.contains(" N0"), header);
  }

  public void testInitialSeedWithExpirationEmitsJAndNExpirationFlags ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setInitial(10).setDelta(1).setExpiration(120);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" J10"), header);
    Assert.assertTrue(header.contains(" N120"), header);
    Assert.assertTrue(header.contains(" T120"), header);
  }

  public void testCasFlagsAreEmittedWhenTokenSupplied ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand().setKey("k").setDelta(1).setCas(77L);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" C77 c"), header);
  }

  public void testHitResponseCarriesCounterValue ()
    throws Exception {

    Response response = new Response(ResponseCode.HD);
    response.setValue("42".getBytes(StandardCharsets.UTF_8));
    response.setCas(3L);

    Result result = new ArithmeticCommand().process(response);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(new String(result.getValue(), StandardCharsets.UTF_8), "42");
    Assert.assertEquals(result.getCas(), 3L);
  }

  public void testExNfNsResponsesProduceUnsuccessfulResults ()
    throws Exception {

    ArithmeticCommand command = new ArithmeticCommand();

    Assert.assertFalse(command.process(new Response(ResponseCode.EX)).isSuccessful());
    Assert.assertFalse(command.process(new Response(ResponseCode.NF)).isSuccessful());
    Assert.assertFalse(command.process(new Response(ResponseCode.NS)).isSuccessful());
  }

  public void testUnexpectedResponseCodeIsRejected () {

    Assert.assertThrows(UnexpectedResponseException.class, () -> new ArithmeticCommand().process(new Response(ResponseCode.MN)));
  }
}
