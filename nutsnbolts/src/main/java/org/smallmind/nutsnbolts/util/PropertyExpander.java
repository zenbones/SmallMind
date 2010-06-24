package org.smallmind.nutsnbolts.util;

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

  public String expand (String expansion, Map<String, String> expansionMap)
    throws PropertyExpanderException {

    return expand(expansion, new StringBuilder(expansion), expansionMap).toString();
  }

  private StringBuilder expand (String originalExpansion, StringBuilder expansionBuilder, Map<String, String> expansionMap)
    throws PropertyExpanderException {

    HashSet<String> encounteredKeySet;
    String expansionKey;
    String expansionValue;
    int arabesqueCount = 0;
    int parsePos = 0;
    int prefixPos;
    int nextPrefixPos;
    int suffixPos;
    int markerPos = -1;

    encounteredKeySet = new HashSet<String>();
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
                  throw new PropertyExpanderException("Could find no mapping for property(%s%s%s) within the expansion sub-template(%s) of (%s)", prefix, expansionKey, suffix, expansionBuilder.substring(prefixPos, suffixPos + suffix.length()), originalExpansion);
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
        if (encounteredKeySet.contains(expansionKey)) {
          throw new PropertyExpanderException("Circular reference to property(%s%s%s) within the expansion sub-template(%s)", prefix, expansionKey, suffix, expansionBuilder.toString());
        }

        encounteredKeySet.add(expansionKey);
        expansionBuilder.replace(prefixPos, suffixPos + suffix.length(), expansionValue);
        parsePos = prefixPos;
        markerPos += expansionValue.length() - (suffixPos + suffix.length() - prefixPos);
      }
    }

    return expansionBuilder;
  }
}
