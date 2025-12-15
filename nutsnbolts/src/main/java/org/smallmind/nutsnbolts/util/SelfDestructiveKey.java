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
package org.smallmind.nutsnbolts.util;

import org.smallmind.nutsnbolts.time.Stint;

/**
 * Wrapper around a map key carrying an expiration time for self-destruction.
 *
 * @param <K> comparable key type
 */
public class SelfDestructiveKey<K extends Comparable<K>> implements Comparable<SelfDestructiveKey<K>> {

  private final K mapKey;
  private final Stint timeoutStint;
  private final long ignitionTime;

  /**
   * Constructs a key without an underlying map key, using the provided timeout.
   *
   * @param timeoutStint timeout before destruction
   */
  public SelfDestructiveKey (Stint timeoutStint) {

    this(null, timeoutStint);
  }

  /**
   * Constructs a key associated with a map key and timeout.
   *
   * @param mapKey       underlying key
   * @param timeoutStint timeout before destruction
   */
  public SelfDestructiveKey (K mapKey, Stint timeoutStint) {

    this.mapKey = mapKey;
    this.timeoutStint = timeoutStint;

    ignitionTime = System.currentTimeMillis() + timeoutStint.toMilliseconds();
  }

  /**
   * @return underlying map key
   */
  public K getMapKey () {

    return mapKey;
  }

  /**
   * @return timeout stint used when this key was created
   */
  public Stint getTimeoutStint () {

    return timeoutStint;
  }

  /**
   * @return absolute expiration time in milliseconds since epoch
   */
  public long getIgnitionTime () {

    return ignitionTime;
  }

  /**
   * Orders keys by ignition time, then by underlying key when present.
   */
  @Override
  public int compareTo (SelfDestructiveKey<K> key) {

    int comparison;

    if ((comparison = Long.compare(ignitionTime, key.getIgnitionTime())) == 0) {

      return (mapKey == null) ? ((key.getMapKey() == null) ? 0 : -1) : ((key.getMapKey() == null) ? 1 : mapKey.compareTo(key.getMapKey()));
    }

    return comparison;
  }

  /**
   * Hashes based on the underlying map key.
   */
  @Override
  public int hashCode () {

    return mapKey.hashCode();
  }

  /**
   * Equality is based on the underlying map key value.
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof SelfDestructiveKey) && (mapKey.equals(((SelfDestructiveKey<?>)obj).getMapKey()));
  }
}
