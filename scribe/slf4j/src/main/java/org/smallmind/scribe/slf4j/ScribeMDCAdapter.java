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
 * SLF4J {@link MDCAdapter} backed by the scribe {@link Parameters} holder.
 * This adapter maps MDC operations to the parameter store used by scribe to
 * capture per-thread contextual information.
 */
public class ScribeMDCAdapter implements MDCAdapter {

  /**
   * Adds or replaces an MDC value for the current thread.
   *
   * @param key the MDC key
   * @param val the string value to associate
   */
  @Override
  public void put (String key, String val) {

    Parameters.getInstance().put(key, val);
  }

  /**
   * Pushes a value onto the MDC deque for the given key, creating a deque if none exists.
   *
   * @param key the MDC key
   * @param val the value to push
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
   * Pops the most recent value from the MDC deque for the given key.
   *
   * @param key the MDC key
   * @return the popped value, or {@code null} if none are present
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
   * Clears the deque tracked for the supplied key, replacing it with an empty deque.
   *
   * @param key the MDC key
   */
  @Override
  public void clearDequeByKey (String key) {

    Parameters.getInstance().put(key, new LinkedList<Serializable>());
  }

  /**
   * Returns a copy of the deque for the supplied key.
   *
   * @param key the MDC key
   * @return a copy of the deque, or {@code null} if no deque exists
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
   * Retrieves a single MDC value for the supplied key.
   *
   * @param key the MDC key
   * @return the string value, or {@code null} if none exists
   */
  @Override
  public String get (String key) {

    Serializable value;

    return ((value = Parameters.getInstance().get(key)) == null) ? null : value.toString();
  }

  /**
   * Removes the MDC value associated with the supplied key.
   *
   * @param key the MDC key to clear
   */
  @Override
  public void remove (String key) {

    Parameters.getInstance().remove(key);
  }

  /**
   * Clears all MDC entries for the current thread.
   */
  @Override
  public void clear () {

    Parameters.getInstance().clear();
  }

  /**
   * Produces a copy of the current MDC map.
   *
   * @return a defensive copy of the context map
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
   * Replaces the current MDC map with the provided entries.
   *
   * @param contextMap the context entries to install for the current thread
   */
  @Override
  public void setContextMap (Map<String, String> contextMap) {

    Parameters.getInstance().clear();
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      Parameters.getInstance().put(entry.getKey(), entry.getValue());
    }
  }
}
