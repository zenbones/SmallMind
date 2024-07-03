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
package org.smallmind.web.jwt;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemReader;
import org.smallmind.nutsnbolts.security.AsymmetricAlgorithm;
import org.smallmind.nutsnbolts.security.SecurityProvider;
import org.smallmind.nutsnbolts.security.key.AsymmetricKeyType;
import org.smallmind.nutsnbolts.security.key.X509KeyParser;

public class Wombat {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static void main (String... args)
    throws Exception {

    AsymmetricKeyType.PUBLIC.generateKey(AsymmetricAlgorithm.RSA, SecurityProvider.BOUNCY_CASTLE, "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCYye+zcu9DSrM98ONHXIa+f4BQFNC1sT00IVRdurzqE695Mzw/LJHIFf/62gItw/ofETesFR6S5krSyg7K0uOt3Cw+0aOf5o/p0dJO0oqTWdWd7fjUU49Qd+1I6bifFCiXDQgeIym+dexc+GBzMoDcJqvy4+2d179OYo9kEAqhMp0JMKavWHhlxH7ipJNsswcZhNdi2svYE4chCyzVR8I+VL4BFOrgmXT2TqZnXl/yI0Yg6kqlgD1UPGqqpbiqVOi5oY+zPImkdUtHq19tv6aFxdPUKI3wf0FYx8oGpQDR9D0PUJv0Tq33PLzI7BNBnVeR4Qxt9PBpGr6O8c3puD/v");

    String raw = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCYye+zcu9DSrM9\n" +
                   "8ONHXIa+f4BQFNC1sT00IVRdurzqE695Mzw/LJHIFf/62gItw/ofETesFR6S5krS\n" +
                   "yg7K0uOt3Cw+0aOf5o/p0dJO0oqTWdWd7fjUU49Qd+1I6bifFCiXDQgeIym+dexc\n" +
                   "+GBzMoDcJqvy4+2d179OYo9kEAqhMp0JMKavWHhlxH7ipJNsswcZhNdi2svYE4ch\n" +
                   "CyzVR8I+VL4BFOrgmXT2TqZnXl/yI0Yg6kqlgD1UPGqqpbiqVOi5oY+zPImkdUtH\n" +
                   "q19tv6aFxdPUKI3wf0FYx8oGpQDR9D0PUJv0Tq33PLzI7BNBnVeR4Qxt9PBpGr6O\n" +
                   "8c3puD/vAgMBAAECggEBAIAISAsx9vmGsWjoYSw9htQ/d8CjkLmQil8SxuW8Q+5L\n" +
                   "DLIdkxDFQmxOszD8WvMK1KAQB3z2PaaPwIeetoKT8iKQ66rAdcLarCIdp4RRvbn4\n" +
                   "f59V+TvDwcaGmJqO8ByuAc2CKBlJEgP+QVEu+XPPEFhrN4/UHBw23KLMyKfrIV0u\n" +
                   "Bvsw/txLRgXoobCSrV2nQiWiclYOg0caEM4t0ekwnsWAt1PEj6txqquwUxzhkpJR\n" +
                   "Qn2P3sVmbdGRf9fVYISSFPh/UgsFUaAydCnOrefspUxoidsnjs0DBzm2tao4jkV7\n" +
                   "GSlBraSnBbrEKXSehSP0CjV0mBhHJl8APGwEji17p8ECgYEAy7N8AkimRW4D0+BG\n" +
                   "x8ErXKD4GVuoclwNVPr2uu25VfjtNn9enC1rlu3BEmbrQci2DBREDnvnX6Qd6NQ3\n" +
                   "UZwuS9GWtM82OoJOu1rFlETaphjYpOC4Bmw1v4s4qRB87WwRQ5EWZfZmWRbtplWv\n" +
                   "B0g/Jy89F4TUQg7HXrnkLrzKB7ECgYEAwAQtGrQyjnoMNGpJvft7IwDxPoiroEj5\n" +
                   "fTT+ZUkqYy49Oh+k5Qrdb/W5rBFu11OFUEfp9wkxs46wqpw0GE3hR6xFd1aWqc0+\n" +
                   "6+egT4i5Fa1P1Cff8tmEynQdWDSLgzL6CWMaVzoMC9AbKFi+HYlRl8eWB70xIhQS\n" +
                   "+Uv7Z1vMSZ8CgYEAmdofcK4kf04fr/i2HQuGT7j4ilaTPITQQP4oOnlwwUF5EO9U\n" +
                   "qwsXNvCuIdMAHziUXz0zRelJkRAo0wDuI0KeKP/NHxVedQSUqKdfkaQLrOYZQzbD\n" +
                   "Z351fg2OJwtgAAbeZzT/QNXA6csKhdYzk4F6yYLBVrEpjBcTeDVY5gALa5ECgYBR\n" +
                   "eLQmk3SsDU7mYn69dFmv5XN2xAiGLtBk9rpGAYBRqsnhwpF1eWGxYoKQZqkwBckX\n" +
                   "4ht8bNNAy3dcPIDCGzFN9uNWmk+85lAfSh00Ad/+OZYYf36/DNoSCKh2x3y2g0eh\n" +
                   "+gGwJvn7fSY4vUaVQ7FyBAY9bGHPgtL2Ie4e0c1hIwKBgCbUQeUpLNt/IF3Y6tKR\n" +
                   "YAFdkoC38Tea3JDXHSf5iI+cxvMeVLR38XSXyUx3p0nQn2B52k39tzHl9r55GMG+\n" +
                   "PucpRnycGUfnPb7qMfh4a7PJQipbgvrIvL4HSkWgR79L9du88orYschvnI8/0pvM\n" +
                   "B5cku14qo766iJWgmNSJI7lu";

    X509KeyParser parser = new X509KeyParser(raw);
    parser.extractFactors();
    byte[] bytes = new PemReader(new StringReader(raw.startsWith("-----") ? raw : "-----BEGIN PRIVATE KEY-----\n" + raw + "\n-----END PRIVATE KEY-----\n")).readPemObject().getContent();
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
    KeyFactory.getInstance("RSA", "BC").generatePrivate(spec);
    AsymmetricKeyType.PRIVATE.generateKey(AsymmetricAlgorithm.RSA, SecurityProvider.BOUNCY_CASTLE, raw);

    // KeyParser keyParser = new X509KeyParser("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzKBgQnYA3f6v68uFMcVDiwrZCMztj1duPSLyK4Q3C+7qPnDYiOzxL9IwacRnllDFN+iR1QMDLZrzcu4vwU7fwBsrAmoJtZ0GMECEPcbmx2VhT9acKpa/GMsiUzfQWGauY4Q3/Z9K8YwuOmzQcMyxRYj2DkWD682QrZYbIuz+pQwZc0IOkJA4ExvqBnW72oiW0o9k2Pio7nWx506Lcp4FXmE3F6ixrSgsEK7dwuLeiOIoboTyyPWcLDWmi7k3GDETyOUPY4dG9LxNSHgnL2xUWotxY2vu1MFIK+Cdw8IWkci/lHvgHPa1GwJtioUNg7LnYHx4ng6R41llfbYPmADS8QIDAQAB");
    // System.out.println(OpenSSHPubicKeyUtility.convert(keyParser.extractFactors()));
  }
}
