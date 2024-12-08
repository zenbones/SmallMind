/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.property;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import org.smallmind.nutsnbolts.security.kms.Decryptor;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;

public class PropertyExpander {

  private final PropertyClosure propertyClosure;
  private final SystemPropertyMode systemPropertyMode;
  private final boolean ignoreUnresolvableProperties;
  private final boolean searchSystemEnvironment;

  public PropertyExpander ()
    throws PropertyExpanderException {

    this(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, true);
  }

  public PropertyExpander (PropertyClosure propertyClosure, boolean ignoreUnresolvableProperties, SystemPropertyMode systemPropertyMode, boolean searchSystemEnvironment) {

    this.propertyClosure = propertyClosure;
    this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
    this.systemPropertyMode = systemPropertyMode;
    this.searchSystemEnvironment = searchSystemEnvironment;
  }

  public String expand (String expansion)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), Collections.emptyMap()).toString();
  }

  public String expand (String expansion, Map<String, Object> expansionMap)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), expansionMap).toString();
  }

  private StringBuilder expand (String originalExpansion, StringBuilder expansionBuilder, Map<String, Object> expansionMap)
    throws PropertyExpanderException {

    HashSet<String> encounteredKeySet;
    PropertyPrologue prologue;
    PropertyPrologue nextPrologue;
    String expansionKey;
    Object expansionValue;
    int arabesqueCount = 0;
    int parsePos = 0;
    int suffixPos;
    int markerPos = -1;

    encounteredKeySet = new HashSet<>();
    while ((prologue = propertyClosure.nextPrologue(expansionBuilder, parsePos)).getPos() >= 0) {
      arabesqueCount++;
      parsePos = prologue.getPos() + prologue.getPrefix().length();
      do {
        if ((suffixPos = expansionBuilder.indexOf(propertyClosure.getSuffix(), parsePos)) < 0) {
          throw new PropertyExpanderException("Unclosed property prefix within the expansion sub-template(%s)", expansionBuilder.toString());
        }

        if (((nextPrologue = propertyClosure.nextPrologue(expansionBuilder, parsePos)).getPos() >= 0) && (nextPrologue.getPos() < suffixPos)) {
          arabesqueCount++;
          parsePos = nextPrologue.getPos() + nextPrologue.getPrefix().length();
        } else {
          arabesqueCount--;
          parsePos = suffixPos + propertyClosure.getSuffix().length();
        }
      } while (arabesqueCount > 0);

      if (parsePos >= markerPos) {
        encounteredKeySet.clear();
      }
      markerPos = parsePos;

      expansionKey = expand(originalExpansion, new StringBuilder(expansionBuilder.substring(prologue.getPos() + prologue.getPrefix().length(), suffixPos)), expansionMap).toString();

      if ((!systemPropertyMode.equals(SystemPropertyMode.OVERRIDE)) || ((expansionValue = System.getProperty(expansionKey)) == null)) {
        if ((!(systemPropertyMode.equals(SystemPropertyMode.OVERRIDE) && searchSystemEnvironment)) || ((expansionValue = System.getenv(expansionKey)) == null)) {
          if ((expansionValue = expansionMap.get(expansionKey)) == null) {
            if ((!systemPropertyMode.equals(SystemPropertyMode.FALLBACK)) || ((expansionValue = System.getProperty(expansionKey)) == null)) {
              if ((!(systemPropertyMode.equals(SystemPropertyMode.FALLBACK) && searchSystemEnvironment)) || ((expansionValue = System.getenv(expansionKey)) == null)) {
                if (!ignoreUnresolvableProperties) {

                  String subExpansion;

                  if (originalExpansion.equals(subExpansion = expansionBuilder.substring(prologue.getPos(), suffixPos + propertyClosure.getSuffix().length()))) {
                    if (originalExpansion.equals(prologue.getPrefix() + expansionKey + propertyClosure.getSuffix())) {
                      throw new PropertyExpanderException("Could find no mapping for property(%s)", originalExpansion);
                    } else {
                      throw new PropertyExpanderException("Could find no mapping for property(%s%s%s) within the expansion (%s)", prologue.getPrefix(), expansionKey, propertyClosure.getSuffix(), originalExpansion);
                    }
                  } else if (subExpansion.equals(prologue.getPrefix() + expansionKey + propertyClosure.getSuffix())) {
                    throw new PropertyExpanderException("Could find no mapping for property(%s) within the expansion (%s)", subExpansion, originalExpansion);
                  } else {
                    throw new PropertyExpanderException("Could find no mapping for property(%s%s%s) within the expansion sub-template(%s) of (%s)", prologue.getPrefix(), expansionKey, propertyClosure.getSuffix(), subExpansion, originalExpansion);
                  }
                } else {
                  expansionBuilder.replace(prologue.getPos() + prologue.getPrefix().length(), suffixPos, expansionKey);
                  parsePos += expansionKey.length() - (suffixPos - (prologue.getPos() + prologue.getPrefix().length()));
                }
              }
            }
          }
        }
      }

      if (expansionValue != null) {

        Decryptor decryptor;
        String expansionValueAsString;

        if ((decryptor = prologue.getDecryptor()) == null) {
          expansionValueAsString = expansionValue.toString();
        } else {
          try {
            expansionValueAsString = decryptor.decrypt(expansionValue.toString());
          } catch (Exception exception) {
            throw new PropertyExpanderException(exception);
          }
        }

        if (encounteredKeySet.contains(expansionKey)) {
          throw new PropertyExpanderException("Circular reference to property(%s%s%s) within the expansion sub-template(%s)", prologue.getPrefix(), expansionKey, propertyClosure.getSuffix(), expansionBuilder.toString());
        }

        encounteredKeySet.add(expansionKey);
        expansionBuilder.replace(prologue.getPos(), suffixPos + propertyClosure.getSuffix().length(), expansionValueAsString);
        parsePos = prologue.getPos();
        markerPos += expansionValueAsString.length() - (suffixPos + propertyClosure.getSuffix().length() - prologue.getPos());
      }
    }

    return expansionBuilder;
  }
}
