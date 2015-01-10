/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.spring.property;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class YamlPropertyHandler implements PropertyHandler<YamlPropertyEntry> {

  private LinkedList<NamedIteration> iteratorList = new LinkedList<>();

  public YamlPropertyHandler (Map<String, Object> yamlMap) {

    iteratorList.push(new NamedIteration(yamlMap.entrySet().iterator()));
  }

  @Override
  public Iterator<YamlPropertyEntry> iterator () {

    return this;
  }

  @Override
  public boolean hasNext () {

    while (!iteratorList.isEmpty()) {
      if (!iteratorList.peekFirst().getIterator().hasNext()) {
        iteratorList.pop();
      }
      else {

        return true;
      }
    }

    return false;
  }

  @Override
  public YamlPropertyEntry next () {

    Map.Entry<String, Object> entry = iteratorList.peekFirst().getIterator().next();

    iteratorList.peekFirst().setName(entry.getKey());

    if (entry.getValue() instanceof Map) {
      iteratorList.push(new NamedIteration(((Map<String, Object>)entry.getValue()).entrySet().iterator()));

      return next();
    }
    else if (entry.getValue() instanceof List) {

      LinkedHashMap<String, Object> interpolatedMap = new LinkedHashMap<>();
      int index = 0;

      for (Object item : (List)entry.getValue()) {
        interpolatedMap.put(String.valueOf(index++), item);
      }

      iteratorList.push(new NamedIteration(interpolatedMap.entrySet().iterator()));

      return next();
    }
    else {

      StringBuilder keyBuilder = new StringBuilder();
      boolean first = true;

      for (NamedIteration namedIteration : iteratorList) {
        if (!first) {
          keyBuilder.insert(0, '.');
        }
        first = false;
        keyBuilder.insert(0, namedIteration.getName());
      }

      return new YamlPropertyEntry(keyBuilder.toString(), entry.getValue().toString());
    }
  }

  @Override
  public void remove () {

    throw new UnsupportedOperationException();
  }

  private class NamedIteration {

    private Iterator<Map.Entry<String, Object>> iterator;
    private String name;

    public NamedIteration (Iterator<Map.Entry<String, Object>> iterator) {

      this.iterator = iterator;
    }

    private String getName () {

      return name;
    }

    private void setName (String name) {

      this.name = name;
    }

    private Iterator<Map.Entry<String, Object>> getIterator () {

      return iterator;
    }
  }
}
