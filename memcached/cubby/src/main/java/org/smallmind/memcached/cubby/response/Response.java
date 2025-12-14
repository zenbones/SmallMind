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

/**
 * Represents a parsed memcached response including status code, flags, CAS token and value payload.
 */
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

  /**
   * Creates a response with the given status code.
   *
   * @param code response status
   */
  public Response (ResponseCode code) {

    this.code = code;
  }

  /**
   * @return response status code
   */
  public ResponseCode getCode () {

    return code;
  }

  /**
   * @return opaque token echoed by the server
   */
  public String getToken () {

    return token;
  }

  /**
   * @param token opaque token echoed by the server
   */
  public void setToken (String token) {

    this.token = token;
  }

  /**
   * @return length of the value payload, or -1 if absent
   */
  public int getValueLength () {

    return valueLength;
  }

  /**
   * @param valueLength length of the value payload
   */
  public void setValueLength (int valueLength) {

    this.valueLength = valueLength;
  }

  /**
   * @return value bytes, or {@code null} when no value was returned
   */
  public byte[] getValue () {

    return value;
  }

  /**
   * @param value value payload
   */
  public void setValue (byte[] value) {

    this.value = value;
  }

  /**
   * @return CAS token returned by the server
   */
  public long getCas () {

    return cas;
  }

  /**
   * @param cas CAS token returned by the server
   */
  public void setCas (long cas) {

    this.cas = cas;
  }

  /**
   * @return size of the object on the server, when provided
   */
  public int getSize () {

    return size;
  }

  /**
   * @param size size of the object on the server
   */
  public void setSize (int size) {

    this.size = size;
  }

  /**
   * @return {@code true} if the server indicates ownership of the record
   */
  public boolean isWon () {

    return won;
  }

  /**
   * @param won ownership flag
   */
  public void setWon (boolean won) {

    this.won = won;
  }

  /**
   * @return {@code true} if another server also claims the record
   */
  public boolean isAlsoWon () {

    return alsoWon;
  }

  /**
   * @param alsoWon flag indicating multiple winners
   */
  public void setAlsoWon (boolean alsoWon) {

    this.alsoWon = alsoWon;
  }

  /**
   * @return {@code true} if the value is marked as stale
   */
  public boolean isStale () {

    return stale;
  }

  /**
   * @param stale stale indicator
   */
  public void setStale (boolean stale) {

    this.stale = stale;
  }
}
