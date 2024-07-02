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

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.smallmind.nutsnbolts.security.key.KeyParser;
import org.smallmind.nutsnbolts.security.key.OpenSSHPubicKeyUtility;
import org.smallmind.nutsnbolts.security.key.X509KeyParser;

public class Wombat {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static void main (String... args)
    throws Exception {

    KeyParser keyParser = new X509KeyParser("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzKBgQnYA3f6v68uFMcVDiwrZCMztj1duPSLyK4Q3C+7qPnDYiOzxL9IwacRnllDFN+iR1QMDLZrzcu4vwU7fwBsrAmoJtZ0GMECEPcbmx2VhT9acKpa/GMsiUzfQWGauY4Q3/Z9K8YwuOmzQcMyxRYj2DkWD682QrZYbIuz+pQwZc0IOkJA4ExvqBnW72oiW0o9k2Pio7nWx506Lcp4FXmE3F6ixrSgsEK7dwuLeiOIoboTyyPWcLDWmi7k3GDETyOUPY4dG9LxNSHgnL2xUWotxY2vu1MFIK+Cdw8IWkci/lHvgHPa1GwJtioUNg7LnYHx4ng6R41llfbYPmADS8QIDAQAB");
    System.out.println(OpenSSHPubicKeyUtility.convert(keyParser.extractFactors()));
  }
}
