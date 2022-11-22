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
package org.smallmind.scribe.pen.adapter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.smallmind.scribe.pen.Parameter;

public class RecordParameters {

  private static final Parameter[] NO_PARAMETERS = new Parameter[0];

  private final HashMap<String, RecordParameterValue> parameterMap = new HashMap<>();

  public void clear () {

    parameterMap.clear();
  }

  public void remove (String key) {

    parameterMap.remove(key);
  }

  public Serializable get (String key) {

    RecordParameterValue parameterValue;

    return ((parameterValue = parameterMap.get(key)) == null) ? null : parameterValue.get();
  }

  public void set (String key, Serializable value) {

    RecordParameterValue parameterValue;

    if ((parameterValue = parameterMap.get(key)) == null) {
      parameterMap.put(key, new SingleParameterValue(value));
    } else {
      parameterValue.set(value);
    }
  }

  public Serializable pop (String key) {

    RecordParameterValue parameterValue;

    return ((parameterValue = parameterMap.get(key)) == null) ? null : parameterValue.pop();
  }

  public void push (String key, Serializable value) {

    RecordParameterValue parameterValue;

    if ((parameterValue = parameterMap.get(key)) == null) {
      parameterMap.put(key, new StackParameterValue(value));
    } else if (ParameterValueType.STACK.equals(parameterValue.type())) {
      parameterValue.push(value);
    } else {

      StackParameterValue replacement;

      parameterMap.put(key, replacement = new StackParameterValue(parameterValue.get()));
      replacement.push(value);
    }
  }

  public List<Serializable> copyList (String key) {

    RecordParameterValue parameterValue;

    if ((parameterValue = parameterMap.get(key)) == null) {

      return null;
    } else if (ParameterValueType.SINGLE.equals(parameterValue.type())) {

      return List.of(parameterValue.get());
    } else {

      return ((StackParameterValue)parameterValue).copyList();
    }
  }

  public Parameter[] asParameters () {

    if (parameterMap.isEmpty()) {

      return NO_PARAMETERS;
    } else {

      LinkedList<Parameter> parameterList = new LinkedList<>();

      for (Map.Entry<String, RecordParameterValue> entry : parameterMap.entrySet()) {

        Serializable value;

        if ((value = entry.getValue().get()) != null) {
          parameterList.add(new Parameter(entry.getKey(), value));
        }
      }

      return parameterList.isEmpty() ? NO_PARAMETERS : parameterList.toArray(new Parameter[0]);
    }
  }
}
