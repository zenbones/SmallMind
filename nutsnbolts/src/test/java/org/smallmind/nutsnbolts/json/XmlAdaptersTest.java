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
package org.smallmind.nutsnbolts.json;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.Date;
import javax.crypto.KeyGenerator;
import org.smallmind.nutsnbolts.http.HttpMethod;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class XmlAdaptersTest {

  public void testZonedDateTimeRoundTrip () {

    ZonedDateTimeXmlAdapter adapter = new ZonedDateTimeXmlAdapter();
    ZonedDateTime original = ZonedDateTime.of(2026, 5, 28, 10, 15, 30, 0, ZoneId.of("UTC"));

    String marshalled = adapter.marshal(original);

    Assert.assertNotNull(marshalled);
    Assert.assertEquals(adapter.unmarshal(marshalled).toInstant(), original.toInstant());
  }

  public void testZonedDateTimeNullPassesThrough () {

    ZonedDateTimeXmlAdapter adapter = new ZonedDateTimeXmlAdapter();

    Assert.assertNull(adapter.marshal(null));
    Assert.assertNull(adapter.unmarshal(null));
  }

  public void testLocalDateTimeRoundTrip () {

    LocalDateTimeXmlAdapter adapter = new LocalDateTimeXmlAdapter();
    LocalDateTime original = LocalDateTime.of(2026, 1, 2, 3, 4, 5);

    String marshalled = adapter.marshal(original);

    Assert.assertNotNull(marshalled);
    Assert.assertEquals(adapter.unmarshal(marshalled), original);
  }

  public void testDateAdapterRoundTrip () {

    DateXmlAdapter adapter = new DateXmlAdapter();
    Date original = Date.from(ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneId.of("UTC")).toInstant());

    Date roundTrip = adapter.unmarshal(adapter.marshal(original));

    Assert.assertEquals(roundTrip.getTime() / 1000L, original.getTime() / 1000L);
  }

  public void testTimestampAdapterRoundTrip () {

    TimestampXmlAdapter adapter = new TimestampXmlAdapter();
    long epoch = ZonedDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();

    String marshalled = adapter.marshal(epoch);

    Assert.assertNotNull(marshalled);
    Assert.assertEquals((long)adapter.unmarshal(marshalled), epoch);
    Assert.assertNull(adapter.marshal(null));
    Assert.assertNull(adapter.unmarshal(null));
  }

  public void testCurrencyAdapterRoundTrip () {

    CurrencyXmlAdapter adapter = new CurrencyXmlAdapter();
    Currency original = Currency.getInstance("USD");

    Assert.assertEquals(adapter.marshal(original), "USD");
    Assert.assertEquals(adapter.unmarshal("usd"), original);
    Assert.assertNull(adapter.marshal(null));
    Assert.assertNull(adapter.unmarshal(null));
  }

  public void testHttpMethodEnumAdapterRoundTrip () {

    HttpMethodEnumXmlAdapter adapter = new HttpMethodEnumXmlAdapter();

    Assert.assertEquals(adapter.unmarshal("GET"), HttpMethod.GET);
    Assert.assertEquals(adapter.unmarshal("get"), HttpMethod.GET);
    Assert.assertEquals(adapter.marshal(HttpMethod.POST), "POST");
    Assert.assertNull(adapter.marshal(null));
    Assert.assertNull(adapter.unmarshal(null));
  }

  public void testBinaryDataRoundTripsBytesViaBase64 ()
    throws Exception {

    byte[] payload = {0x01, 0x02, 0x03, 0x04};

    BinaryData binary = new BinaryData(Encoding.BASE_64, payload);

    Assert.assertEquals(binary.decode(), payload);
    Assert.assertFalse(binary.isEncrypted());
  }

  public void testBinaryDataRoundTripsBytesViaHex ()
    throws Exception {

    byte[] payload = {0x10, 0x20, 0x30};

    BinaryData binary = new BinaryData(Encoding.HEX, payload);

    Assert.assertEquals(binary.decode(), payload);
    Assert.assertEquals(binary.getEncoding(), Encoding.HEX);
  }

  public void testBinaryDataEncryptsAndDecrypts ()
    throws Exception {

    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(128);
    Key key = keyGenerator.generateKey();

    BinaryData binary = new BinaryData(Encoding.BASE_64, Encryption.AES, key, "top-secret");

    Assert.assertTrue(binary.isEncrypted());
    Assert.assertEquals(new String(binary.decrypt(key)), "top-secret");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testBinaryDataDecryptWithoutEncryptionThrows ()
    throws Exception {

    BinaryData binary = new BinaryData(Encoding.HEX, new byte[] {1, 2});

    binary.decrypt(null);
  }
}
