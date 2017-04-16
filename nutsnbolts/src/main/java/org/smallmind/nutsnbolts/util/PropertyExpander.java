/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class PropertyExpander {

  private SystemPropertyMode systemPropertyMode;
  private String prefix;
  private String suffix;
  private boolean ignoreUnresolvableProperties;
  private boolean searchSystemEnvironment;

  public PropertyExpander ()
    throws PropertyExpanderException {

    this("${", "}", false, SystemPropertyMode.FALLBACK, true);
  }

  public PropertyExpander (boolean ignoreUnresolvableProperties)
    throws PropertyExpanderException {

    this("${", "}", ignoreUnresolvableProperties, SystemPropertyMode.FALLBACK, true);
  }

  public PropertyExpander (SystemPropertyMode systemPropertyMode, boolean searchSystemEnvironment)
    throws PropertyExpanderException {

    this("${", "}", false, systemPropertyMode, searchSystemEnvironment);
  }

  public PropertyExpander (boolean ignoreUnresolvableProperties, SystemPropertyMode systemPropertyMode, boolean searchSystemEnvironment)
    throws PropertyExpanderException {

    this("${", "}", ignoreUnresolvableProperties, systemPropertyMode, searchSystemEnvironment);
  }

  public PropertyExpander (String prefix, String suffix, boolean ignoreUnresolvableProperties, SystemPropertyMode systemPropertyMode, boolean searchSystemEnvironment)
    throws PropertyExpanderException {

    this.prefix = prefix;
    this.suffix = suffix;
    this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
    this.systemPropertyMode = systemPropertyMode;
    this.searchSystemEnvironment = searchSystemEnvironment;

    for (int pos = 0; pos < prefix.length(); pos++) {
      if (suffix.indexOf(prefix.charAt(pos)) >= 0) {
        throw new PropertyExpanderException("The prefix(%s) and suffix(%s) should have no characters in common", prefix, suffix);
      }
    }
  }

  public String expand (String expansion)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), Collections.<String, Object>emptyMap()).toString();
  }

  public String expand (String expansion, Map<String, Object> expansionMap)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), expansionMap).toString();
  }

  private StringBuilder expand (String originalExpansion, StringBuilder expansionBuilder, Map<String, Object> expansionMap)
    throws PropertyExpanderException {

    HashSet<String> encounteredKeySet;
    String expansionKey;
    Object expansionValue;
    int arabesqueCount = 0;
    int parsePos = 0;
    int prefixPos;
    int nextPrefixPos;
    int suffixPos;
    int markerPos = -1;

    encounteredKeySet = new HashSet<>();
    while ((prefixPos = expansionBuilder.indexOf(prefix, parsePos)) >= 0) {
      arabesqueCount++;
      parsePos = prefixPos + prefix.length();
      do {
        if ((suffixPos = expansionBuilder.indexOf(suffix, parsePos)) < 0) {
          throw new PropertyExpanderException("Unclosed property prefix within the expansion sub-template(%s)", expansionBuilder.toString());
        }

        if (((nextPrefixPos = expansionBuilder.indexOf(prefix, parsePos)) >= 0) && (nextPrefixPos < suffixPos)) {
          arabesqueCount++;
          parsePos = nextPrefixPos + prefix.length();
        }
        else {
          arabesqueCount--;
          parsePos = suffixPos + suffix.length();
        }
      } while (arabesqueCount > 0);

      if (parsePos >= markerPos) {
        encounteredKeySet.clear();
      }
      markerPos = parsePos;

      expansionKey = expand(originalExpansion, new StringBuilder(expansionBuilder.substring(prefixPos + prefix.length(), suffixPos)), expansionMap).toString();

      if ((!systemPropertyMode.equals(SystemPropertyMode.OVERRIDE)) || ((expansionValue = System.getProperty(expansionKey)) == null)) {
        if ((!(systemPropertyMode.equals(SystemPropertyMode.OVERRIDE) && searchSystemEnvironment)) || ((expansionValue = System.getenv(expansionKey)) == null)) {
          if ((expansionValue = expansionMap.get(expansionKey)) == null) {
            if ((!systemPropertyMode.equals(SystemPropertyMode.FALLBACK)) || ((expansionValue = System.getProperty(expansionKey)) == null)) {
              if ((!(systemPropertyMode.equals(SystemPropertyMode.FALLBACK) && searchSystemEnvironment)) || ((expansionValue = System.getenv(expansionKey)) == null)) {
                if (!ignoreUnresolvableProperties) {

                  String subExpansion;

                  if (originalExpansion.equals(subExpansion = expansionBuilder.substring(prefixPos, suffixPos + suffix.length()))) {
                    if (originalExpansion.equals(prefix + expansionKey + suffix)) {
                      throw new PropertyExpanderException("Could find no mapping for property(%s)", originalExpansion);
                    }
                    else {
                      throw new PropertyExpanderException("Could find no mapping for property(%s%s%s) within the expansion (%s)", prefix, expansionKey, suffix, originalExpansion);
                    }
                  }
                  else if (subExpansion.equals(prefix + expansionKey + suffix)) {
                    throw new PropertyExpanderException("Could find no mapping for property(%s) within the expansion (%s)", subExpansion, originalExpansion);
                  }
                  else {
                    throw new PropertyExpanderException("Could find no mapping for property(%s%s%s) within the expansion sub-template(%s) of (%s)", prefix, expansionKey, suffix, subExpansion, originalExpansion);
                  }
                }
                else {
                  expansionBuilder.replace(prefixPos + prefix.length(), suffixPos, expansionKey);
                  parsePos += expansionKey.length() - (suffixPos - (prefixPos + prefix.length()));
                }
              }
            }
          }
        }
      }

      if (expansionValue != null) {

        String expansionValueAsString = expansionValue.toString();

        if (encounteredKeySet.contains(expansionKey)) {
          throw new PropertyExpanderException("Circular reference to property(%s%s%s) within the expansion sub-template(%s)", prefix, expansionKey, suffix, expansionBuilder.toString());
        }

        encounteredKeySet.add(expansionKey);
        expansionBuilder.replace(prefixPos, suffixPos + suffix.length(), expansionValueAsString);
        parsePos = prefixPos;
        markerPos += expansionValueAsString.length() - (suffixPos + suffix.length() - prefixPos);
      }
    }

    return expansionBuilder;
  }
}
