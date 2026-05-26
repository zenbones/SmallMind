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
public class SetCommandTest {

  private static final KeyTranslator IDENTITY = key -> key;

  private static String headerOf (byte[] commandBytes) {

    String text = new String(commandBytes, StandardCharsets.UTF_8);
    int crlf = text.indexOf("\r\n");

    return text.substring(0, crlf);
  }

  private static String payloadOf (byte[] commandBytes) {

    String text = new String(commandBytes, StandardCharsets.UTF_8);
    int firstCrlf = text.indexOf("\r\n");

    return text.substring(firstCrlf + 2, text.length() - 2);
  }

  public void testPlainSetEmitsMetaSetHeaderWithLengthAndBinaryFlag ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("hello".getBytes(StandardCharsets.UTF_8));

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.startsWith("ms k 5 b"), header);
    Assert.assertFalse(header.contains(" M "), header);
    Assert.assertFalse(header.contains(" C "), header);
  }

  public void testTrailingCrlfFramesValuePayload ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("hello".getBytes(StandardCharsets.UTF_8));

    byte[] bytes = command.construct(IDENTITY);

    Assert.assertEquals(payloadOf(bytes), "hello");
    Assert.assertEquals(bytes[bytes.length - 2], (byte)'\r');
    Assert.assertEquals(bytes[bytes.length - 1], (byte)'\n');
  }

  public void testCasZeroPromotesToAddAndRequestsResultingCas ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setCas(0L);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" ME"), header);
    Assert.assertTrue(header.contains(" c"), header);
    Assert.assertFalse(header.contains(" C0"), header);
  }

  public void testExplicitCasEmitsCFlagWithToken ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setCas(7L);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" C7 c"), header);
  }

  public void testModeReplaceEmitsMrFlag ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.REPLACE);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" MR"), header);
  }

  public void testExpirationOnSetEmitsTFlag ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.SET).setExpiration(120);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" T120"), header);
    Assert.assertFalse(header.contains(" N"), header);
  }

  public void testVivifyAppendWithoutExpirationEmitsNZeroFlag ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.APPEND).setVivify(true);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" MA"), header);
    Assert.assertTrue(header.contains(" N0"), header);
    Assert.assertFalse(header.contains(" T"), header);
  }

  public void testVivifyAppendWithExpirationEmitsNExpirationFlag ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.APPEND).setVivify(true).setExpiration(60);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" N60"), header);
    Assert.assertFalse(header.contains(" T60"), header);
  }

  public void testAppendWithoutVivifyOmitsNAndTFlags ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setMode(SetMode.APPEND).setExpiration(60);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertFalse(header.contains(" N"), header);
    Assert.assertFalse(header.contains(" T"), header);
  }

  public void testOpaqueTokenIsAppended ()
    throws Exception {

    SetCommand command = new SetCommand().setKey("k").setValue("v".getBytes(StandardCharsets.UTF_8)).setOpaqueToken("xyz");

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.endsWith(" Oxyz"), header);
  }

  public void testHdResponseProducesSuccessfulResultCarryingCas ()
    throws Exception {

    Response response = new Response(ResponseCode.HD);
    response.setCas(99L);

    Result result = new SetCommand().process(response);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(result.getCas(), 99L);
    Assert.assertNull(result.getValue());
  }

  public void testExNfNsResponsesProduceUnsuccessfulResults ()
    throws Exception {

    SetCommand command = new SetCommand();

    Assert.assertFalse(command.process(new Response(ResponseCode.EX)).isSuccessful());
    Assert.assertFalse(command.process(new Response(ResponseCode.NF)).isSuccessful());
    Assert.assertFalse(command.process(new Response(ResponseCode.NS)).isSuccessful());
  }

  public void testUnexpectedResponseCodeIsRejected () {

    Assert.assertThrows(UnexpectedResponseException.class, () -> new SetCommand().process(new Response(ResponseCode.MN)));
  }
}
