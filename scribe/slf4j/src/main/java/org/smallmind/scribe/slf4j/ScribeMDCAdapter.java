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

public class ScribeMDCAdapter implements MDCAdapter {

  @Override
  public void put (String key, String val) {

    Parameters.getInstance().put(key, val);
  }

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

  @Override
  public void clearDequeByKey (String key) {

    Parameters.getInstance().put(key, new LinkedList<Serializable>());
  }

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

  @Override
  public String get (String key) {

    Serializable value;

    return ((value = Parameters.getInstance().get(key)) == null) ? null : value.toString();
  }

  @Override
  public void remove (String key) {

    Parameters.getInstance().remove(key);
  }

  @Override
  public void clear () {

    Parameters.getInstance().clear();
  }

  @Override
  public Map<String, String> getCopyOfContextMap () {

    HashMap<String, String> map = new HashMap<>();

    for (Parameter parameter : Parameters.getInstance().getParameters()) {
      map.put(parameter.getKey(), parameter.getValue().toString());
    }

    return map;
  }

  @Override
  public void setContextMap (Map<String, String> contextMap) {

    Parameters.getInstance().clear();
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      Parameters.getInstance().put(entry.getKey(), entry.getValue());
    }
  }
}
