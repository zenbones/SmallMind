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
package org.smallmind.scribe.slf4j;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.slf4j.spi.MDCAdapter;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.adapter.Parameters;

/**
 * SLF4J {@link MDCAdapter} that stores all diagnostic context in the scribe
 * {@link Parameters} thread-local parameter store, making MDC values available
 * to scribe formatters and appenders as first-class log record parameters.
 * Supports both flat key/value entries and keyed deques for nested context.
 */
public class ScribeMDCAdapter implements MDCAdapter {

  /**
   * Stores or replaces a flat string value under {@code key} in the current thread's
   * parameter context.
   *
   * @param key the MDC key; must not be {@code null}
   * @param val the string value to associate with the key
   */
  @Override
  public void put (String key, String val) {

    Parameters.getInstance().put(key, val);
  }

  /**
   * Appends {@code val} to the deque stored under {@code key}, creating a new deque if
   * the key is absent or its current value is not a {@link Deque}.
   *
   * @param key the MDC key identifying the deque
   * @param val the string value to append
   */
  @Override
  public void pushByKey (String key, String val) {

    Serializable serializable;

    if (((serializable = Parameters.getInstance().get(key)) == null) || (!Deque.class.isAssignableFrom(serializable.getClass()))) {

      LinkedList<Serializable> deque = new LinkedList<>();

      deque.add(val);
      Parameters.getInstance().put(key, deque);
    } else {
      ((Deque)serializable).add(val);
    }
  }

  /**
   * Removes and returns the head of the deque stored under {@code key}.
   * Returns {@code null} if the key is absent, its value is not a deque, or the deque is empty;
   * removes the entry entirely if the value is a non-deque scalar.
   *
   * @param key the MDC key identifying the deque
   * @return the popped string value, or {@code null} if nothing was available
   */
  @Override
  public String popByKey (String key) {

    Serializable value;

    if ((value = Parameters.getInstance().get(key)) == null) {

      return null;
    } else if (Deque.class.isAssignableFrom(value.getClass())) {
      if (((Deque)value).isEmpty()) {

        return null;
      } else {

        Object obj;

        return ((obj = ((Deque)value).pop()) == null) ? null : obj.toString();
      }
    } else {

      Parameters.getInstance().remove(key);

      return null;
    }
  }

  /**
   * Replaces the value stored under {@code key} with a new empty deque, discarding any
   * previously accumulated entries.
   *
   * @param key the MDC key whose deque should be cleared
   */
  @Override
  public void clearDequeByKey (String key) {

    Parameters.getInstance().put(key, new LinkedList<Serializable>());
  }

  /**
   * Returns a snapshot copy of the deque stored under {@code key}, converting each element
   * to a string via {@code toString()}.
   *
   * @param key the MDC key identifying the deque
   * @return a new {@link Deque} containing the current elements as strings, or {@code null}
   * if no deque exists for the key
   */
  @Override
  public Deque<String> getCopyOfDequeByKey (String key) {

    Serializable value;

    if (((value = Parameters.getInstance().get(key)) == null) || (!Deque.class.isAssignableFrom(value.getClass()))) {

      return null;
    } else {

      LinkedList<String> dequeueCopy = new LinkedList<>();

      for (Object obj : (Deque)value) {
        dequeueCopy.add(obj.toString());
      }

      return dequeueCopy;
    }
  }

  /**
   * Retrieves the string representation of the value stored under {@code key}.
   *
   * @param key the MDC key to look up
   * @return the value's {@code toString()}, or {@code null} if the key is absent
   */
  @Override
  public String get (String key) {

    Serializable value;

    return ((value = Parameters.getInstance().get(key)) == null) ? null : value.toString();
  }

  /**
   * Removes the entry for {@code key} from the current thread's parameter context.
   *
   * @param key the MDC key to remove
   */
  @Override
  public void remove (String key) {

    Parameters.getInstance().remove(key);
  }

  /**
   * Removes all entries from the current thread's parameter context.
   */
  @Override
  public void clear () {

    Parameters.getInstance().clear();
  }

  /**
   * Builds a {@link Map} snapshot of the current thread's parameter context, converting
   * each value to a string via {@code toString()}.
   *
   * @return a new modifiable map containing all current key/value pairs as strings
   */
  @Override
  public Map<String, String> getCopyOfContextMap () {

    HashMap<String, String> map = new HashMap<>();

    for (Parameter parameter : Parameters.getInstance().getParameters()) {
      map.put(parameter.getKey(), parameter.getValue().toString());
    }

    return map;
  }

  /**
   * Replaces the current thread's entire parameter context with the entries from
   * {@code contextMap}, first clearing all existing entries.
   *
   * @param contextMap the new key/value pairs to install; must not be {@code null}
   */
  @Override
  public void setContextMap (Map<String, String> contextMap) {

    Parameters.getInstance().clear();
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      Parameters.getInstance().put(entry.getKey(), entry.getValue());
    }
  }
}
