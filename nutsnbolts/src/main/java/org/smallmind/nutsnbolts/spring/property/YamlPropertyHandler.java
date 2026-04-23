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
package org.smallmind.nutsnbolts.spring.property;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A {@link PropertyHandler} that flattens nested YAML map and list structures into dot-notated {@link YamlPropertyEntry} instances.
 */
public class YamlPropertyHandler implements PropertyHandler<YamlPropertyEntry> {

  private final Map<String, Object> yamlMap;

  /**
   * Creates a handler backed by the root YAML map produced by SnakeYAML.
   *
   * @param yamlMap the root map of the parsed YAML document
   */
  public YamlPropertyHandler (Map<String, Object> yamlMap) {

    this.yamlMap = yamlMap;
  }

  /**
   * Returns an iterator that recursively walks the YAML structure and yields one flattened entry per leaf value.
   *
   * @return an iterator producing {@link YamlPropertyEntry} instances with dot-notated keys
   */
  @Override
  public Iterator<YamlPropertyEntry> iterator () {

    return new YamlPropertyEntryIterator(yamlMap);
  }

  /**
   * Recursively walks nested YAML maps and lists, building dot-notated keys for each scalar leaf value.
   */
  private static class YamlPropertyEntryIterator implements Iterator<YamlPropertyEntry> {

    private final LinkedList<NamedIteration> namedIterationList = new LinkedList<>();

    /**
     * Seeds the iterator with the root YAML map; an empty or null map produces an empty iteration.
     *
     * @param yamlMap the root YAML map to iterate
     */
    private YamlPropertyEntryIterator (Map<String, Object> yamlMap) {

      if ((yamlMap != null) && (!yamlMap.isEmpty())) {
        namedIterationList.push(new NamedIteration(yamlMap.entrySet().iterator()));
      }
    }

    /**
     * Returns {@code true} if the iteration stack contains at least one more leaf entry.
     *
     * @return {@code true} if another flattened entry is available
     */
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

    /**
     * Advances to and returns the next flattened leaf entry, descending into nested maps and lists as needed.
     *
     * @return the next {@link YamlPropertyEntry} with its full dot-notated key
     */
    @Override
    public YamlPropertyEntry next () {

      Map.Entry<String, Object> entry = namedIterationList.peekFirst().getIterator().next();

      namedIterationList.peekFirst().setName(entry.getKey());

      if (entry.getValue() instanceof Map) {
        namedIterationList.push(new NamedIteration(((Map<String, Object>)entry.getValue()).entrySet().iterator()));

        return next();
      } else if (entry.getValue() instanceof List<?>) {

        LinkedHashMap<String, Object> interpolatedMap = new LinkedHashMap<>();
        int index = 0;

        for (Object item : (List<?>)entry.getValue()) {
          interpolatedMap.put(String.valueOf(index++), item);
        }

        namedIterationList.push(new NamedIteration(interpolatedMap.entrySet().iterator()));

        return next();
      } else {

        StringBuilder keyBuilder = new StringBuilder();
        boolean first = true;

        for (NamedIteration namedIteration : namedIterationList) {
          if (!first) {
            keyBuilder.insert(0, '.');
          }
          first = false;
          keyBuilder.insert(0, namedIteration.getName());
        }

        return new YamlPropertyEntry(keyBuilder.toString(), entry.getValue());
      }
    }

    /**
     * Removal is not supported by this iterator.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  /**
   * Pairs a map-level iterator with the key segment name of the entry most recently advanced at that level.
   */
  private static class NamedIteration {

    private final Iterator<Map.Entry<String, Object>> iterator;
    private String name;

    /**
     * Creates a named iteration wrapping the given map entry iterator.
     *
     * @param iterator the iterator over entries at a single YAML map level
     */
    private NamedIteration (Iterator<Map.Entry<String, Object>> iterator) {

      this.iterator = iterator;
    }

    /**
     * Returns the key segment recorded for the most recently visited entry at this map level.
     *
     * @return the current key segment name
     */
    private String getName () {

      return name;
    }

    /**
     * Records the key segment for the most recently visited entry at this map level.
     *
     * @param name the key segment to record
     */
    private void setName (String name) {

      this.name = name;
    }

    /**
     * Returns the underlying iterator over entries at this YAML map level.
     *
     * @return the map entry iterator
     */
    private Iterator<Map.Entry<String, Object>> getIterator () {

      return iterator;
    }
  }
}
