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
package org.smallmind.nutsnbolts.command;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Captures parsed options and their associated arguments.
 */
public class OptionSet {

  private final HashMap<String, LinkedList<String>> optionMap = new HashMap<>();
  private final LinkedList<String> remainingList = new LinkedList<>();

  /**
   * Adds an option without arguments if it has not been recorded yet.
   *
   * @param option option name or flag (as a string)
   */
  public synchronized void addOption (String option) {

    if (!optionMap.containsKey(option)) {
      optionMap.put(option, new LinkedList<>());
    }
  }

  /**
   * Adds an argument to an option, creating the option entry if needed.
   *
   * @param option   option name or flag
   * @param argument argument value
   */
  public synchronized void addArgument (String option, String argument) {

    addOption(option);
    (optionMap.get(option)).add(argument);
  }

  /**
   * Records an undeclared trailing argument.
   *
   * @param argument free-form argument
   */
  public synchronized void addRemaining (String argument) {

    remainingList.add(argument);
  }

  /**
   * @return remaining undeclared arguments in encounter order
   */
  public synchronized String[] getRemaining () {

    return remainingList.toArray(new String[0]);
  }

  /**
   * @return options that were provided
   */
  public synchronized String[] getOptions () {

    return optionMap.keySet().toArray(new String[0]);
  }

  /**
   * Tests whether an option with the given long name was provided.
   */
  public synchronized boolean containsOption (String name) {

    return containsOption(name, null);
  }

  /**
   * Tests whether an option with the given flag was provided.
   */
  public synchronized boolean containsOption (Character flag) {

    return containsOption(null, flag);
  }

  /**
   * Tests whether an option was provided by name or flag.
   *
   * @param name long option name
   * @param flag single-character flag
   * @return {@code true} if the option exists
   */
  public synchronized boolean containsOption (String name, Character flag) {

    return optionMap.containsKey(name) || optionMap.containsKey((flag == null) ? null : flag.toString());
  }

  /**
   * Returns the first argument for a named option, or {@code null} if none exist.
   */
  public synchronized String getArgument (String name) {

    return getArgument(name, null);
  }

  /**
   * Returns the first argument for a flagged option, or {@code null} if none exist.
   */
  public synchronized String getArgument (char flag) {

    return getArgument(null, flag);
  }

  /**
   * Retrieves the first argument for an option referenced by name or flag.
   *
   * @param name long option name
   * @param flag single-character flag
   * @return first argument value or {@code null} if absent
   */
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

  /**
   * Returns all arguments for a named option or {@code null} if the option was not provided.
   */
  public synchronized String[] getArguments (String name) {

    return getArguments(name, null);
  }

  /**
   * Returns all arguments for a flagged option or {@code null} if the option was not provided.
   */
  public synchronized String[] getArguments (char flag) {

    return getArguments(null, flag);
  }

  /**
   * Retrieves all arguments for an option referenced by name or flag.
   *
   * @param name long option name
   * @param flag single-character flag
   * @return array of arguments or {@code null} if the option was not present
   */
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

  /**
   * Formats the options and their arguments for debugging.
   */
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
