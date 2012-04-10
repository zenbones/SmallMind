/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.statistics;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.persistence.Durable;

public class Statistics implements Serializable {

  // Map<Durable Name, Map<Method Name, Map<Stat Source, StatLine>>>
  private HashMap<String, HashMap<String, HashMap<String, StatLine>>> lineMap = new HashMap<String, HashMap<String, HashMap<String, StatLine>>>();

  public synchronized HashMap<String, HashMap<String, HashMap<String, StatLine>>> getLineMap () {

    return lineMap;
  }

  public synchronized void addStatLine (Class<? extends Durable> durableClass, Method method, String source, long time) {

    HashMap<String, HashMap<String, StatLine>> methodMap;
    HashMap<String, StatLine> sourceMap;
    StatLine statLine;

    if ((methodMap = lineMap.get(durableClass.getSimpleName())) == null) {
      lineMap.put(durableClass.getSimpleName(), methodMap = new HashMap<String, HashMap<String, StatLine>>());
    }

    if ((sourceMap = methodMap.get(method.getName())) == null) {
      methodMap.put(method.getName(), sourceMap = new HashMap<String, StatLine>());
    }

    if ((statLine = sourceMap.get(source)) == null) {
      sourceMap.put(source, statLine = new StatLine(source));
    }

    statLine.hit(time);
  }
}
