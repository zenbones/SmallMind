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
package org.smallmind.nutsnbolts.spring.property;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class YamlPropertyHandler implements PropertyHandler<YamlPropertyEntry> {

  private final JsonNode yamlNode;

  public YamlPropertyHandler (JsonNode yamlNode) {

    this.yamlNode = yamlNode;
  }

  @Override
  public Iterator<YamlPropertyEntry> iterator () {

    return new YamlPropertyEntryIterator(yamlNode);
  }

  private static class YamlPropertyEntryIterator implements Iterator<YamlPropertyEntry> {

    private final LinkedList<NamedIteration> namedIterationList = new LinkedList<>();

    private YamlPropertyEntryIterator (JsonNode yamlNode) {

      if (yamlNode != null) {
        namedIterationList.push(new NamedIteration(yamlNode.fields()));
      }
    }

    @Override
    public boolean hasNext () {

      while (!namedIterationList.isEmpty()) {
        if (!namedIterationList.peekFirst().getIterator().hasNext()) {
          namedIterationList.pop();
        } else {

          return true;
        }
      }

      return false;
    }

    @Override
    public YamlPropertyEntry next () {

      NamedIteration namedIteration;

      if ((namedIteration = namedIterationList.peekFirst()) == null) {
        throw new NullPointerException();
      } else {

        Map.Entry<String, JsonNode> entry = namedIteration.getIterator().next();

        namedIteration.setName(entry.getKey());

        if (JsonNodeType.OBJECT.equals(entry.getValue().getNodeType())) {
          namedIterationList.push(new NamedIteration(entry.getValue().fields()));

          return next();
        } else if (JsonNodeType.ARRAY.equals(entry.getValue().getNodeType())) {

          LinkedHashMap<String, JsonNode> interpolatedMap = new LinkedHashMap<>();
          int index = 0;

          for (JsonNode item : entry.getValue()) {
            interpolatedMap.put(String.valueOf(index++), item);
          }

          namedIterationList.push(new NamedIteration(interpolatedMap.entrySet().iterator()));

          return next();
        } else {

          StringBuilder keyBuilder = new StringBuilder();
          boolean first = true;

          for (NamedIteration child : namedIterationList) {
            if (!first) {
              keyBuilder.insert(0, '.');
            }
            first = false;
            keyBuilder.insert(0, child.getName());
          }

          return new YamlPropertyEntry(keyBuilder.toString(),
            switch (entry.getValue().getNodeType()) {
              case NUMBER -> {
                if (entry.getValue().isInt()) {
                  yield entry.getValue().asInt();
                } else if (entry.getValue().isLong()) {
                  yield entry.getValue().asLong();
                } else {
                  yield entry.getValue().asDouble();
                }
              }
              case BOOLEAN -> entry.getValue().asBoolean();
              case STRING -> entry.getValue().asText();
              default -> throw new UnknownSwitchCaseException(entry.getValue().getNodeType().name());
            });
        }
      }
    }

    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  private static class NamedIteration {

    private final Iterator<Map.Entry<String, JsonNode>> iterator;
    private String name;

    private NamedIteration (Iterator<Map.Entry<String, JsonNode>> iterator) {

      this.iterator = iterator;
    }

    private String getName () {

      return name;
    }

    private void setName (String name) {

      this.name = name;
    }

    private Iterator<Map.Entry<String, JsonNode>> getIterator () {

      return iterator;
    }
  }
}
