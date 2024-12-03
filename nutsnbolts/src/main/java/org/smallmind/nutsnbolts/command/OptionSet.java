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
package org.smallmind.nutsnbolts.command;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class OptionSet {

  private final HashMap<String, LinkedList<String>> optionMap = new HashMap<>();
  private final LinkedList<String> remainingList = new LinkedList<>();

  public synchronized void addOption (String option) {

    if (!optionMap.containsKey(option)) {
      optionMap.put(option, new LinkedList<>());
    }
  }

  public synchronized void addArgument (String option, String argument) {

    addOption(option);
    (optionMap.get(option)).add(argument);
  }

  public synchronized void addRemaining (String argument) {

    remainingList.add(argument);
  }

  public synchronized String[] getRemaining () {

    return remainingList.toArray(new String[0]);
  }

  public synchronized String[] getOptions () {

    return optionMap.keySet().toArray(new String[0]);
  }

  public synchronized boolean containsOption (String name) {

    return containsOption(name, null);
  }

  public synchronized boolean containsOption (Character flag) {

    return containsOption(null, flag);
  }

  public synchronized boolean containsOption (String name, Character flag) {

    return optionMap.containsKey(name) || optionMap.containsKey((flag == null) ? null : flag.toString());
  }

  public synchronized String getArgument (String name) {

    return getArgument(name, null);
  }

  public synchronized String getArgument (char flag) {

    return getArgument(null, flag);
  }

  public synchronized String getArgument (String name, Character flag) {

    LinkedList<String> argumentList;

    if ((argumentList = optionMap.get(name)) == null) {
      argumentList = optionMap.get((flag == null) ? null : flag.toString());
    }

    if (argumentList != null) {
      if (!argumentList.isEmpty()) {
        return argumentList.getFirst();
      }
    }

    return null;
  }

  public synchronized String[] getArguments (String name) {

    return getArguments(name, null);
  }

  public synchronized String[] getArguments (char flag) {

    return getArguments(null, flag);
  }

  public synchronized String[] getArguments (String name, Character flag) {

    LinkedList<String> argumentList;
    String[] arguments = null;

    if ((argumentList = optionMap.get(name)) == null) {
      argumentList = optionMap.get((flag == null) ? null : flag.toString());
    }

    if (argumentList != null) {
      arguments = new String[argumentList.size()];
      argumentList.toArray(arguments);
    }

    return arguments;
  }

  public String toString () {

    StringBuilder lineBuilder = new StringBuilder("[");
    boolean first = true;

    for (Map.Entry<String, LinkedList<String>> optionEntry : optionMap.entrySet()) {
      if (!first) {
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
            if (!innerFirst) {
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
