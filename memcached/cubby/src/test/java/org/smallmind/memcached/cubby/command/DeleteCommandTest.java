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
public class DeleteCommandTest {

  private static final KeyTranslator IDENTITY = key -> key;

  private static String headerOf (byte[] commandBytes) {

    String text = new String(commandBytes, StandardCharsets.UTF_8);
    int crlf = text.indexOf("\r\n");

    return text.substring(0, crlf);
  }

  public void testPlainDeleteEmitsHeaderWithoutCasFlag ()
    throws Exception {

    DeleteCommand command = new DeleteCommand().setKey("k");

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertEquals(header, "md k b");
  }

  public void testDeleteWithCasEmitsConditionalCasFlags ()
    throws Exception {

    DeleteCommand command = new DeleteCommand().setKey("k").setCas(42L);

    String header = headerOf(command.construct(IDENTITY));

    Assert.assertTrue(header.contains(" C42 c"), header);
  }

  public void testHitResponseProducesSuccessfulResult ()
    throws Exception {

    Result result = new DeleteCommand().process(new Response(ResponseCode.HD));

    Assert.assertTrue(result.isSuccessful());
  }

  public void testNotFoundResponseIsIdempotentSuccess ()
    throws Exception {

    Result result = new DeleteCommand().process(new Response(ResponseCode.NF));

    Assert.assertTrue(result.isSuccessful());
  }

  public void testCasMismatchProducesUnsuccessfulResult ()
    throws Exception {

    Result result = new DeleteCommand().process(new Response(ResponseCode.EX));

    Assert.assertFalse(result.isSuccessful());
  }

  public void testUnexpectedResponseCodeIsRejected () {

    Assert.assertThrows(UnexpectedResponseException.class, () -> new DeleteCommand().process(new Response(ResponseCode.MN)));
  }
}
