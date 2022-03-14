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
package org.smallmind.memcached.cubby.command;

import java.io.IOException;
import org.smallmind.memcached.cubby.Authentication;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.translator.KeyTranslator;

public class AuthenticationCommand extends Command {

  private Authentication authentication;

  public AuthenticationCommand setAuthentication (Authentication authentication) {

    this.authentication = authentication;

    return this;
  }

  @Override
  public String getKey ()
    throws CubbyOperationException {

    throw new CubbyOperationException("Authentication can't be used in the normal request/response cycle");
  }

  @Override
  public byte[] construct (KeyTranslator keyTranslator, CubbyCodec codec)
    throws IOException, CubbyOperationException {

    byte[] bytes;
    byte[] commandBytes;
    byte[] valueBytes = codec.serialize(authentication.getUsername() + " " + authentication.getPassword());

    StringBuilder line = new StringBuilder("ms ").append(keyTranslator.encode("unused")).append(' ').append(valueBytes.length).append(" b");

    commandBytes = line.append("\r\n").toString().getBytes();
    bytes = new byte[commandBytes.length + valueBytes.length + 2];

    System.arraycopy(commandBytes, 0, bytes, 0, commandBytes.length);
    System.arraycopy(valueBytes, 0, bytes, commandBytes.length, valueBytes.length);
    System.arraycopy("\r\n".getBytes(), 0, bytes, commandBytes.length + valueBytes.length, 2);

    return bytes;
  }
}
