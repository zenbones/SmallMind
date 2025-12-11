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

public class Response {

  private final ResponseCode code;
  private String token;
  private boolean won;
  private boolean alsoWon;
  private boolean stale;
  private long cas;
  private int size = -1;
  private int valueLength = -1;
  private byte[] value;

  public Response (ResponseCode code) {

    this.code = code;
  }

  public ResponseCode getCode () {

    return code;
  }

  public String getToken () {

    return token;
  }

  public void setToken (String token) {

    this.token = token;
  }

  public int getValueLength () {

    return valueLength;
  }

  public void setValueLength (int valueLength) {

    this.valueLength = valueLength;
  }

  public byte[] getValue () {

    return value;
  }

  public void setValue (byte[] value) {

    this.value = value;
  }

  public long getCas () {

    return cas;
  }

  public void setCas (long cas) {

    this.cas = cas;
  }

  public int getSize () {

    return size;
  }

  public void setSize (int size) {

    this.size = size;
  }

  public boolean isWon () {

    return won;
  }

  public void setWon (boolean won) {

    this.won = won;
  }

  public boolean isAlsoWon () {

    return alsoWon;
  }

  public void setAlsoWon (boolean alsoWon) {

    this.alsoWon = alsoWon;
  }

  public boolean isStale () {

    return stale;
  }

  public void setStale (boolean stale) {

    this.stale = stale;
  }
}
