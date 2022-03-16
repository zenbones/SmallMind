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
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.UnexpectedResponseException;
import org.smallmind.memcached.cubby.codec.CubbyCodec;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.smallmind.memcached.cubby.translator.KeyTranslator;

public class SetCommand extends Command {

  private SetMode mode;
  private Object value;
  private String key;
  private String opaqueToken;
  private Integer expiration;
  private Long cas;

  @Override
  public String getKey () {

    return key;
  }

  public SetCommand setKey (String key) {

    this.key = key;

    return this;
  }

  public SetCommand setValue (Object value) {

    this.value = value;

    return this;
  }

  public SetCommand setMode (SetMode mode) {

    this.mode = mode;

    return this;
  }

  public SetCommand setCas (Long cas) {

    this.cas = cas;

    return this;
  }

  public SetCommand setExpiration (Integer expiration) {

    this.expiration = expiration;

    return this;
  }

  public SetCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  @Override
  public byte[] construct (KeyTranslator keyTranslator, CubbyCodec codec)
    throws IOException, CubbyOperationException {

    byte[] bytes;
    byte[] commandBytes;
    byte[] valueBytes = codec.serialize(value);

    StringBuilder line = new StringBuilder("ms ").append(keyTranslator.encode(key)).append(' ').append(valueBytes.length).append(" b");

    if (mode != null) {
      line.append(" M").append(mode.getToken());
    }
    if (cas != null) {
      line.append(" C").append(cas).append(" c");
    }
    if (expiration != null) {
      line.append(" T").append(expiration);
    }
    if (opaqueToken != null) {
      line.append(" O").append(opaqueToken);
    }

    commandBytes = line.append("\r\n").toString().getBytes();
    bytes = new byte[commandBytes.length + valueBytes.length + 2];

    System.arraycopy(commandBytes, 0, bytes, 0, commandBytes.length);
    System.arraycopy(valueBytes, 0, bytes, commandBytes.length, valueBytes.length);
    System.arraycopy("\r\n".getBytes(), 0, bytes, commandBytes.length + valueBytes.length, 2);

    return bytes;
  }

  @Override
  public <T> Result<T> process (CubbyCodec codec, Response response)
    throws IOException {

    if (response.getCode().in(ResponseCode.EX, ResponseCode.NF, ResponseCode.NS)) {

      return new Result<>(null, false, response.getCas());
    } else if (ResponseCode.HD.equals(response.getCode())) {

      return new Result<>(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
