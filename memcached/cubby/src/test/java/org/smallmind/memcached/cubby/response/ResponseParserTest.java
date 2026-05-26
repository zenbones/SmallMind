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
package org.smallmind.memcached.cubby.response;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.IncomprehensibleRequestException;
import org.smallmind.memcached.cubby.IncomprehensibleResponseException;
import org.smallmind.memcached.cubby.connection.ExposedByteArrayOutputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ResponseParserTest {

  private static Response parse (String line)
    throws Exception {

    byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
    ExposedByteArrayOutputStream accumulator = new ExposedByteArrayOutputStream(16);
    JoinedBuffer joinedBuffer = new JoinedBuffer(accumulator, ByteBuffer.wrap(bytes));

    return ResponseParser.parse(joinedBuffer, 0, bytes.length);
  }

  public void testParsesNoopResponseAsMnCode ()
    throws Exception {

    Response response = parse("MN");

    Assert.assertEquals(response.getCode(), ResponseCode.MN);
  }

  public void testParsesHitResponseWithCasAndOpaqueToken ()
    throws Exception {

    Response response = parse("HD c42 Ocorrelation-1");

    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(response.getCas(), 42L);
    Assert.assertEquals(response.getToken(), "correlation-1");
  }

  public void testParsesValueResponseLengthCasAndSize ()
    throws Exception {

    Response response = parse("VA 7 c9 s12");

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(response.getValueLength(), 7);
    Assert.assertEquals(response.getCas(), 9L);
    Assert.assertEquals(response.getSize(), 12);
  }

  public void testParsesValueResponseWithStampedeFlags ()
    throws Exception {

    Response response = parse("VA 0 W Z X");

    Assert.assertEquals(response.getCode(), ResponseCode.VA);
    Assert.assertEquals(response.getValueLength(), 0);
    Assert.assertTrue(response.isWon());
    Assert.assertTrue(response.isAlsoWon());
    Assert.assertTrue(response.isStale());
  }

  public void testParsesMissResponseAsEnCode ()
    throws Exception {

    Response response = parse("EN");

    Assert.assertEquals(response.getCode(), ResponseCode.EN);
  }

  public void testParsesCasMismatchAsExCode ()
    throws Exception {

    Response response = parse("EX");

    Assert.assertEquals(response.getCode(), ResponseCode.EX);
  }

  public void testParsesNotFoundAsNfCode ()
    throws Exception {

    Response response = parse("NF");

    Assert.assertEquals(response.getCode(), ResponseCode.NF);
  }

  public void testParsesNotStoredAsNsCode ()
    throws Exception {

    Response response = parse("NS");

    Assert.assertEquals(response.getCode(), ResponseCode.NS);
  }

  public void testTreatsBareErrorLineAsIncomprehensibleRequest () {

    Assert.assertThrows(IncomprehensibleRequestException.class, () -> parse("ERROR"));
  }

  public void testRejectsUnknownTwoCharacterResponseCode () {

    Assert.assertThrows(IncomprehensibleResponseException.class, () -> parse("ZZ"));
  }

  public void testRejectsValueLineMissingLengthSeparator () {

    Assert.assertThrows(IncomprehensibleResponseException.class, () -> parse("VA"));
  }

  public void testRejectsValueLineWithNonNumericLength () {

    Assert.assertThrows(IncomprehensibleResponseException.class, () -> parse("VA notanumber"));
  }

  public void testRejectsUnknownFlagCharacter () {

    Assert.assertThrows(IncomprehensibleResponseException.class, () -> parse("HD Q"));
  }

  public void testRejectsLineShorterThanTwoBytes () {

    Assert.assertThrows(IncomprehensibleResponseException.class, () -> parse("H"));
  }

  public void testParsesAcrossAccumulatingAndReadBufferBoundary ()
    throws Exception {

    byte[] head = "HD c4".getBytes(StandardCharsets.UTF_8);
    byte[] tail = "2 Otok".getBytes(StandardCharsets.UTF_8);

    ExposedByteArrayOutputStream accumulator = new ExposedByteArrayOutputStream(16);
    accumulator.write(head);

    JoinedBuffer joinedBuffer = new JoinedBuffer(accumulator, ByteBuffer.wrap(tail));

    Response response = ResponseParser.parse(joinedBuffer, 0, head.length + tail.length);

    Assert.assertEquals(response.getCode(), ResponseCode.HD);
    Assert.assertEquals(response.getCas(), 42L);
    Assert.assertEquals(response.getToken(), "tok");
  }
}
