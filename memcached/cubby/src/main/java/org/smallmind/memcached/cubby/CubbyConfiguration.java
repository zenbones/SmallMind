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

import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.codec.LargeValueCompressingCodec;
import org.smallmind.memcached.cubby.codec.ObjectStreamCubbyCodec;
import org.smallmind.memcached.cubby.locator.DefaultKeyLocator;
import org.smallmind.memcached.cubby.locator.KeyLocator;
import org.smallmind.memcached.cubby.locator.MaglevKeyLocator;
import org.smallmind.memcached.cubby.translator.DefaultKeyTranslator;
import org.smallmind.memcached.cubby.translator.KeyTranslator;
import org.smallmind.memcached.cubby.translator.LargeKeyHashingTranslator;

public class CubbyConfiguration {

  public static final CubbyConfiguration DEFAULT = new CubbyConfiguration();
  public static final CubbyConfiguration OPTIMAL = new CubbyConfiguration()
                                                     .setCodec(new LargeValueCompressingCodec(new ObjectStreamCubbyCodec()))
                                                     .setKeyLocator(new MaglevKeyLocator())
                                                     .setKeyTranslator(new LargeKeyHashingTranslator(new DefaultKeyTranslator()));

  private CubbyCodec codec = new ObjectStreamCubbyCodec();
  private KeyLocator keyLocator = new DefaultKeyLocator();
  private KeyTranslator keyTranslator = new DefaultKeyTranslator();
  private Authentication authentication;
  private long defaultRequestTimeoutMilliseconds = 0;
  private long connectionTimeoutMilliseconds = 3000;
  private long readTimeoutMilliseconds = 30000;
  private long keepAliveSeconds = 30;
  private long resuscitationSeconds = 10;
  private int connectionsPerHost = 1;

  public CubbyCodec getCodec () {

    return codec;
  }

  public CubbyConfiguration setCodec (CubbyCodec codec) {

    this.codec = codec;

    return this;
  }

  public KeyLocator getKeyLocator () {

    return keyLocator;
  }

  public CubbyConfiguration setKeyLocator (KeyLocator keyLocator) {

    this.keyLocator = keyLocator;

    return this;
  }

  public KeyTranslator getKeyTranslator () {

    return keyTranslator;
  }

  public CubbyConfiguration setKeyTranslator (KeyTranslator keyTranslator) {

    this.keyTranslator = keyTranslator;

    return this;
  }

  public Authentication getAuthentication () {

    return authentication;
  }

  public CubbyConfiguration setAuthentication (Authentication authentication) {

    this.authentication = authentication;

    return this;
  }

  public long getDefaultRequestTimeoutMilliseconds () {

    return defaultRequestTimeoutMilliseconds;
  }

  public CubbyConfiguration setDefaultRequestTimeoutMilliseconds (long defaultRequestTimeoutMilliseconds) {

    this.defaultRequestTimeoutMilliseconds = defaultRequestTimeoutMilliseconds;

    return this;
  }

  public long getConnectionTimeoutMilliseconds () {

    return connectionTimeoutMilliseconds;
  }

  public CubbyConfiguration setConnectionTimeoutMilliseconds (long connectionTimeoutMilliseconds) {

    this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;

    return this;
  }

  public long getReadTimeoutMilliseconds () {

    return readTimeoutMilliseconds;
  }

  public CubbyConfiguration setReadTimeoutMilliseconds (long readTimeoutMilliseconds) {

    this.readTimeoutMilliseconds = readTimeoutMilliseconds;

    return this;
  }

  public long getKeepAliveSeconds () {

    return keepAliveSeconds;
  }

  public CubbyConfiguration setKeepAliveSeconds (long keepAliveSeconds) {

    this.keepAliveSeconds = keepAliveSeconds;

    return this;
  }

  public long getResuscitationSeconds () {

    return resuscitationSeconds;
  }

  public CubbyConfiguration setResuscitationSeconds (long resuscitationSeconds) {

    this.resuscitationSeconds = resuscitationSeconds;

    return this;
  }

  public int getConnectionsPerHost () {

    return connectionsPerHost;
  }

  public CubbyConfiguration setConnectionsPerHost (int connectionsPerHost) {

    this.connectionsPerHost = connectionsPerHost;

    return this;
  }
}
