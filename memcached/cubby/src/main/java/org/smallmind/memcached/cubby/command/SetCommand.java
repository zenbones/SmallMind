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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.memcached.cubby.UnexpectedResponseException;
import org.smallmind.memcached.cubby.response.Response;
import org.smallmind.memcached.cubby.response.ResponseCode;
import org.smallmind.memcached.cubby.translator.KeyTranslator;

/**
 * Implements the memcached set, add, replace, append and prepend operations along with CAS support.
 */
public class SetCommand extends Command {

  private static final Long ZERO = 0L;

  private SetMode mode;
  private byte[] value;
  private String key;
  private String opaqueToken;
  private boolean vivify;
  private Integer expiration;
  private Long cas;

  /**
   * {@inheritDoc}
   */
  @Override
  public String getKey () {

    return key;
  }

  /**
   * Assigns the cache key.
   *
   * @param key cache key
   * @return this command for chaining
   */
  public SetCommand setKey (String key) {

    this.key = key;

    return this;
  }

  /**
   * Supplies the serialized value payload.
   *
   * @param value encoded value bytes
   * @return this command for chaining
   */
  public SetCommand setValue (byte[] value) {

    this.value = value;

    return this;
  }

  /**
   * Sets the operation mode (set/add/replace/append/prepend).
   *
   * @param mode set mode token
   * @return this command for chaining
   */
  public SetCommand setMode (SetMode mode) {

    this.mode = mode;

    return this;
  }

  /**
   * Adds a CAS token to guard the mutation.
   *
   * @param cas compare-and-swap token
   * @return this command for chaining
   */
  public SetCommand setCas (Long cas) {

    this.cas = cas;

    return this;
  }

  /**
   * Sets the expiration in seconds for the value.
   *
   * @param expiration expiration time in seconds
   * @return this command for chaining
   */
  public SetCommand setExpiration (Integer expiration) {

    this.expiration = expiration;

    return this;
  }

  /**
   * Enables creation of a previously absent key when appending or prepending.
   *
   * @param vivify {@code true} to create the key if missing
   * @return this command for chaining
   */
  public SetCommand setVivify (boolean vivify) {

    this.vivify = vivify;

    return this;
  }

  /**
   * Attaches an opaque token echoed by the server in responses.
   *
   * @param opaqueToken token to include
   * @return this command for chaining
   */
  public SetCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] construct (KeyTranslator keyTranslator)
    throws IOException, CubbyOperationException {

    byte[] bytes;
    byte[] commandBytes;

    StringBuilder line = new StringBuilder("ms ").append(keyTranslator.encode(key)).append(' ').append(value.length).append(" b");

    if (ZERO.equals(cas) && ((mode == null) || SetMode.SET.equals(mode))) {
      line.append(" M").append(SetMode.ADD.getToken());
      line.append(" c");
    } else {
      if (mode != null) {
        line.append(" M").append(mode.getToken());
      }
      if (cas != null) {
        line.append(" C").append(cas).append(" c");
      }
    }
    if (expiration != null) {
      if (SetMode.APPEND.equals(mode) || SetMode.PREPEND.equals(mode)) {
        if (vivify) {
          line.append(" N").append(expiration);
        }
      } else {
        line.append(" T").append(expiration);
      }
    } else if (SetMode.APPEND.equals(mode) || SetMode.PREPEND.equals(mode)) {
      if (vivify) {
        line.append(" N0");
      }
    }

    if (opaqueToken != null) {
      line.append(" O").append(opaqueToken);
    }

    commandBytes = line.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
    bytes = new byte[commandBytes.length + value.length + 2];

    System.arraycopy(commandBytes, 0, bytes, 0, commandBytes.length);
    System.arraycopy(value, 0, bytes, commandBytes.length, value.length);
    System.arraycopy("\r\n".getBytes(StandardCharsets.UTF_8), 0, bytes, commandBytes.length + value.length, 2);

    return bytes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (response.getCode().in(ResponseCode.EX, ResponseCode.NF, ResponseCode.NS)) {

      return new Result(null, false, response.getCas());
    } else if (ResponseCode.HD.equals(response.getCode())) {

      return new Result(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
