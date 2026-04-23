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
 * Represents a fully parsed memcached server response in the Cubby protocol.
 *
 * <p>A {@code Response} carries the {@link ResponseCode} that classifies the result along with
 * optional metadata fields returned by the server: an opaque client token, a value payload with
 * its length, a CAS token, a server-reported object size, and three boolean flags ({@code won},
 * {@code alsoWon}, {@code stale}) used by the meta-protocol for cache-stampede protection.</p>
 *
 * <p>Instances are created and populated by {@link ResponseParser}.</p>
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
   * Creates a response bearing the given status code.
   *
   * @param code the status code returned by the server
   */
  public Response (ResponseCode code) {

    this.code = code;
  }

  /**
   * Returns the status code of this response.
   *
   * @return the {@link ResponseCode} classifying the server outcome
   */
  public ResponseCode getCode () {

    return code;
  }

  /**
   * Returns the opaque client token echoed back by the server, if present.
   *
   * @return the token string, or {@code null} when the server did not echo one
   */
  public String getToken () {

    return token;
  }

  /**
   * Sets the opaque client token echoed back by the server.
   *
   * @param token the token string included in the response
   */
  public void setToken (String token) {

    this.token = token;
  }

  /**
   * Returns the byte-length of the value payload announced in the response header.
   *
   * @return the value length in bytes, or {@code -1} if no value is expected
   */
  public int getValueLength () {

    return valueLength;
  }

  /**
   * Sets the byte-length of the value payload announced in the response header.
   *
   * @param valueLength the length in bytes of the forthcoming value
   */
  public void setValueLength (int valueLength) {

    this.valueLength = valueLength;
  }

  /**
   * Returns the raw value bytes returned by the server.
   *
   * @return the value bytes, or {@code null} when the response carries no value
   */
  public byte[] getValue () {

    return value;
  }

  /**
   * Sets the raw value bytes read from the server.
   *
   * @param value the value payload bytes
   */
  public void setValue (byte[] value) {

    this.value = value;
  }

  /**
   * Returns the CAS (compare-and-swap) token associated with the cached item.
   *
   * @return the CAS token, or {@code 0} if the server did not supply one
   */
  public long getCas () {

    return cas;
  }

  /**
   * Sets the CAS token returned by the server for the cached item.
   *
   * @param cas the compare-and-swap token
   */
  public void setCas (long cas) {

    this.cas = cas;
  }

  /**
   * Returns the server-reported size of the stored object, when provided.
   *
   * @return the stored object size in bytes, or {@code -1} if not supplied
   */
  public int getSize () {

    return size;
  }

  /**
   * Sets the server-reported size of the stored object.
   *
   * @param size the object size in bytes as reported by the server
   */
  public void setSize (int size) {

    this.size = size;
  }

  /**
   * Indicates whether this client has won ownership of a miss-leader lease.
   *
   * @return {@code true} if the server granted this client a cache-miss lease
   */
  public boolean isWon () {

    return won;
  }

  /**
   * Sets the flag indicating that this client won a cache-miss lease.
   *
   * @param won {@code true} if a lease was granted by the server
   */
  public void setWon (boolean won) {

    this.won = won;
  }

  /**
   * Indicates whether another client also holds a lease for the same key.
   *
   * @return {@code true} if a concurrent lease holder exists
   */
  public boolean isAlsoWon () {

    return alsoWon;
  }

  /**
   * Sets the flag indicating multiple concurrent lease holders for the same key.
   *
   * @param alsoWon {@code true} if another client also holds a lease
   */
  public void setAlsoWon (boolean alsoWon) {

    this.alsoWon = alsoWon;
  }

  /**
   * Indicates whether the returned value is marked as stale by the server.
   *
   * @return {@code true} if the value is stale and a background refresh is expected
   */
  public boolean isStale () {

    return stale;
  }

  /**
   * Sets the stale indicator on this response.
   *
   * @param stale {@code true} if the server flagged the value as stale
   */
  public void setStale (boolean stale) {

    this.stale = stale;
  }
}
