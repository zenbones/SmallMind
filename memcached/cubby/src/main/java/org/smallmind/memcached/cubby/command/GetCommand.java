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

public class GetCommand extends Command {

  private String key;
  private String opaqueToken;
  private boolean cas;
  private boolean value = true;
  private Integer expiration;

  @Override
  public String getKey () {

    return key;
  }

  public GetCommand setKey (String key) {

    this.key = key;

    return this;
  }

  public GetCommand setCas (boolean cas) {

    this.cas = cas;

    return this;
  }

  public GetCommand setExpiration (Integer expiration) {

    this.expiration = expiration;

    return this;
  }

  public GetCommand setOpaqueToken (String opaqueToken) {

    this.opaqueToken = opaqueToken;

    return this;
  }

  public GetCommand setValue (boolean value) {

    this.value = value;

    return this;
  }

  @Override
  public byte[] construct (KeyTranslator keyTranslator)
    throws IOException, CubbyOperationException {

    StringBuilder line = new StringBuilder("mg ").append(keyTranslator.encode(key)).append(" b");

    if (value) {
      line.append(" v");
    }
    if (cas) {
      line.append(" c");
    }
    if (expiration != null) {
      line.append(" T").append(expiration);
    }
    if (opaqueToken != null) {
      line.append(" O").append(opaqueToken);
    }

    return line.append("\r\n").toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Result process (Response response)
    throws UnexpectedResponseException {

    if (ResponseCode.EN.equals(response.getCode()) || response.isWon() || response.isAlsoWon()) {

      return new Result(null, false, response.getCas());
    } else if (value && ResponseCode.VA.equals(response.getCode())) {

      return new Result(response.getValue(), true, response.getCas());
    } else if ((!value) && ResponseCode.HD.equals(response.getCode())) {

      return new Result(null, true, response.getCas());
    } else {
      throw new UnexpectedResponseException("Unexpected response code(%s)", response.getCode().name());
    }
  }
}
