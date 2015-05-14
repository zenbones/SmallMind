/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.nutsnbolts.command;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class OptionSet {

  private HashMap<String, LinkedList<String>> optionMap;

  public OptionSet () {

    optionMap = new HashMap<>();
  }

  public synchronized void addOption (String option) {

    if (!optionMap.containsKey(option)) {
      optionMap.put(option, new LinkedList<String>());
    }
  }

  public synchronized void addArgument (String option, String argument) {

    addOption(option);
    (optionMap.get(option)).add(argument);
  }

  public synchronized boolean containsOption (String option) {

    return optionMap.containsKey(option);
  }

  public synchronized boolean containsAllOptions (String[] options) {

    for (String option : options) {
      if (!containsOption(option)) {
        return false;
      }
    }

    return true;
  }

  public synchronized String[] getOptions () {

    String[] options;

    options = new String[optionMap.size()];
    optionMap.keySet().toArray(options);

    return options;
  }

  public synchronized String getArgument (String option) {

    LinkedList<String> argumentList;

    if ((argumentList = optionMap.get(option)) != null) {
      if (!argumentList.isEmpty()) {
        return argumentList.getFirst();
      }
    }

    return null;
  }

  public synchronized String[] getArguments (String option) {

    LinkedList<String> argumentList;
    String[] arguments = null;

    if ((argumentList = optionMap.get(option)) != null) {
      arguments = new String[argumentList.size()];
      argumentList.toArray(arguments);
    }

    return arguments;
  }

  public String toString () {

    StringBuilder lineBuilder = new StringBuilder("[");
    boolean first = true;

    for (Map.Entry<String, LinkedList<String>> optionEntry : optionMap.entrySet()) {
      if (first) {
        lineBuilder.append(", ");
      }

      lineBuilder.append(optionEntry.getKey());
      if ((optionEntry.getValue() != null) && (!optionEntry.getValue().isEmpty())) {
        lineBuilder.append('=');
        if (optionEntry.getValue().size() == 1) {
          lineBuilder.append('"').append(optionEntry.getValue().getFirst()).append('"');
        } else {

          boolean innerFirst = true;

          lineBuilder.append('[');
          for (String argument : optionEntry.getValue()) {
            if (innerFirst) {
              lineBuilder.append(", ");
            }

            lineBuilder.append('"').append(argument).append('"');

            innerFirst = false;
          }
          lineBuilder.append(']');
        }
      }

      first = false;
    }

    return lineBuilder.append(']').toString();
  }
}
