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
package org.smallmind.nutsnbolts.property;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import org.smallmind.nutsnbolts.security.kms.Decryptor;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;

/**
 * Resolves property placeholders embedded in strings by consulting a caller-supplied map, system
 * properties, and environment variables according to a configurable {@link SystemPropertyMode}.
 */
public class PropertyExpander {

  private final PropertyClosure propertyClosure;
  private final SystemPropertyMode systemPropertyMode;
  private final boolean ignoreUnresolvableProperties;
  private final boolean searchSystemEnvironment;

  /**
   * Constructs an expander with the default {@link PropertyClosure}, {@link SystemPropertyMode#FALLBACK}
   * resolution order, system-environment lookup enabled, and strict failure on unresolvable placeholders.
   *
   * @throws PropertyExpanderException if the default property closure cannot be constructed
   */
  public PropertyExpander ()
    throws PropertyExpanderException {

    this(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, true);
  }

  /**
   * Constructs a fully configured expander.
   *
   * @param propertyClosure              the placeholder delimiter and optional encrypted-value configuration
   * @param ignoreUnresolvableProperties when {@code true}, unresolvable placeholders are left in place
   *                                     rather than causing an exception
   * @param systemPropertyMode           controls whether system properties override or fall back to the caller map
   * @param searchSystemEnvironment      when {@code true}, environment variables are also consulted
   */
  public PropertyExpander (PropertyClosure propertyClosure, boolean ignoreUnresolvableProperties, SystemPropertyMode systemPropertyMode, boolean searchSystemEnvironment) {

    this.propertyClosure = propertyClosure;
    this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
    this.systemPropertyMode = systemPropertyMode;
    this.searchSystemEnvironment = searchSystemEnvironment;
  }

  /**
   * Expands all property placeholders in the given string using only system properties and environment
   * variables according to the configured resolution mode.
   *
   * @param expansion the expression string whose placeholders should be resolved
   * @return the fully expanded string
   * @throws PropertyExpanderException if a placeholder cannot be resolved and strict mode is enabled
   */
  public String expand (String expansion)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), Collections.emptyMap()).toString();
  }

  /**
   * Expands all property placeholders in the given string, consulting the supplied map before or after
   * system sources depending on the configured {@link SystemPropertyMode}.
   *
   * @param expansion    the expression string whose placeholders should be resolved
   * @param expansionMap a map of additional property keys to values used during resolution
   * @return the fully expanded string
   * @throws PropertyExpanderException if a placeholder cannot be resolved and strict mode is enabled
   */
  public String expand (String expansion, Map<String, Object> expansionMap)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), expansionMap).toString();
  }

  /**
   * Internal recursive implementation that walks {@code expansionBuilder}, resolves each placeholder it
   * finds against the configured sources, decrypts values when required, and detects circular references.
   *
   * @param originalExpansion the original expression text, retained for meaningful error messages
   * @param expansionBuilder  the mutable string being transformed in place
   * @param expansionMap      the caller-supplied map consulted during resolution
   * @return {@code expansionBuilder} after all resolvable placeholders have been substituted
   * @throws PropertyExpanderException if a circular reference is detected, a placeholder is unclosed,
   *                                   or a required value cannot be found and strict mode is enabled
   */
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
    while ((prologue = propertyClosure.findPrologue(expansionBuilder, parsePos)).getPos() >= 0) {
      arabesqueCount++;
      parsePos = prologue.getPos() + prologue.getPrefix().length();
      do {
        if ((suffixPos = expansionBuilder.indexOf(propertyClosure.getSuffix(), parsePos)) < 0) {
          throw new PropertyExpanderException("Unclosed property prefix within the expansion sub-template(%s)", expansionBuilder.toString());
        }

        if (((nextPrologue = propertyClosure.findPrologue(expansionBuilder, parsePos)).getPos() >= 0) && (nextPrologue.getPos() < suffixPos)) {
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
