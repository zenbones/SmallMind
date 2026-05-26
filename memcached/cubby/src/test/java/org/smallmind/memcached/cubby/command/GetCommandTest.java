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
public class GetCommandTest {

  private static final KeyTranslator IDENTITY = key -> key;

  private static String headerOf (byte[] commandBytes) {

    String text = new String(commandBytes, StandardCharsets.UTF_8);
    int crlf = text.indexOf("\r\n");

    return text.substring(0, crlf);
  }

  public void testDefaultGetRequestsValueBody ()
    throws Exception {

    GetCommand command = new GetCommand().setKey("k");

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.startsWith("mg k b"), header);
    Assert.assertTrue(header.contains(" v"), header);
    Assert.assertFalse(header.contains(" c"), header);
    Assert.assertFalse(header.contains(" T"), header);
  }

  public void testTouchOnlyGetOmitsValueFlag ()
    throws Exception {

    GetCommand command = new GetCommand().setKey("k").setValue(false).setExpiration(30);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertFalse(header.contains(" v"), header);
    Assert.assertTrue(header.contains(" T30"), header);
  }

  public void testCasRequestEmitsCFlag ()
    throws Exception {

    GetCommand command = new GetCommand().setKey("k").setCas(true);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" c"), header);
  }

  public void testOpaqueTokenIsAppended ()
    throws Exception {

    GetCommand command = new GetCommand().setKey("k").setOpaqueToken("trace-7");

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.endsWith(" Otrace-7"), header);
  }

  public void testValueResponseWithBodyProducesSuccessfulResultCarryingBytes ()
    throws Exception {

    Response response = new Response(ResponseCode.VA);
    response.setValue("payload".getBytes(StandardCharsets.UTF_8));
    response.setCas(11L);

    Result result = new GetCommand().process(response);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertEquals(new String(result.getValue(), StandardCharsets.UTF_8), "payload");
    Assert.assertEquals(result.getCas(), 11L);
  }

  public void testHitResponseOnTouchOnlyProducesSuccessfulResultWithoutBytes ()
    throws Exception {

    Response response = new Response(ResponseCode.HD);

    Result result = new GetCommand().setValue(false).process(response);

    Assert.assertTrue(result.isSuccessful());
    Assert.assertNull(result.getValue());
  }

  public void testMissResponseProducesUnsuccessfulResult ()
    throws Exception {

    Result result = new GetCommand().process(new Response(ResponseCode.EN));

    Assert.assertFalse(result.isSuccessful());
  }

  public void testWonLeaseFlagOnValueResponseTreatedAsCacheMiss ()
    throws Exception {

    Response response = new Response(ResponseCode.VA);
    response.setWon(true);
    response.setValue(new byte[0]);

    Result result = new GetCommand().process(response);

    Assert.assertFalse(result.isSuccessful());
  }

  public void testAlsoWonLeaseFlagOnValueResponseTreatedAsCacheMiss ()
    throws Exception {

    Response response = new Response(ResponseCode.VA);
    response.setAlsoWon(true);
    response.setValue(new byte[0]);

    Result result = new GetCommand().process(response);

    Assert.assertFalse(result.isSuccessful());
  }

  public void testUnexpectedResponseCodeIsRejected () {

    Assert.assertThrows(UnexpectedResponseException.class, () -> new GetCommand().process(new Response(ResponseCode.MN)));
  }
}
