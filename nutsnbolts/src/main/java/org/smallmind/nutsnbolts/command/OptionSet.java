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
 * Holds the options and their associated argument values produced by {@link org.smallmind.nutsnbolts.command.CommandLineParser}.
 */
public class OptionSet {

  private final HashMap<String, LinkedList<String>> optionMap = new HashMap<>();
  private final LinkedList<String> remainingList = new LinkedList<>();

  /**
   * Records an option that carries no arguments, doing nothing if the option is already present.
   *
   * @param option option name or single-character flag as a string key
   */
  public synchronized void addOption (String option) {

    if (!optionMap.containsKey(option)) {
      optionMap.put(option, new LinkedList<>());
    }
  }

  /**
   * Appends an argument value to an option, creating the option entry if it does not yet exist.
   *
   * @param option   option name or single-character flag as a string key
   * @param argument argument value to associate with the option
   */
  public synchronized void addArgument (String option, String argument) {

    addOption(option);
    (optionMap.get(option)).add(argument);
  }

  /**
   * Stores a non-option token that was not associated with any declared option.
   *
   * @param argument undeclared trailing token to preserve
   */
  public synchronized void addRemaining (String argument) {

    remainingList.add(argument);
  }

  /**
   * Returns all undeclared trailing arguments in the order they were encountered.
   *
   * @return array of undeclared argument tokens, empty if none were recorded
   */
  public synchronized String[] getRemaining () {

    return remainingList.toArray(new String[0]);
  }

  /**
   * Returns the keys of all options that were parsed, each representing either a long name or a single-character flag.
   *
   * @return array of option keys in no guaranteed order
   */
  public synchronized String[] getOptions () {

    return optionMap.keySet().toArray(new String[0]);
  }

  /**
   * Tests whether an option identified by its long name was parsed.
   *
   * @param name long option name to look up
   * @return {@code true} if the option was present in the parsed arguments
   */
  public synchronized boolean containsOption (String name) {

    return containsOption(name, null);
  }

  /**
   * Tests whether an option identified by its single-character flag was parsed.
   *
   * @param flag single-character flag to look up
   * @return {@code true} if the option was present in the parsed arguments
   */
  public synchronized boolean containsOption (Character flag) {

    return containsOption(null, flag);
  }

  /**
   * Tests whether an option was parsed, checking both the long name and the single-character flag.
   *
   * @param name long option name; checked first when non-{@code null}
   * @param flag single-character flag; checked when the name produces no match
   * @return {@code true} if either the name or the flag is found
   */
  public synchronized boolean containsOption (String name, Character flag) {

    return optionMap.containsKey(name) || optionMap.containsKey((flag == null) ? null : flag.toString());
  }

  /**
   * Returns the first argument value for the option identified by its long name, or {@code null} if the option was not parsed or has no arguments.
   *
   * @param name long option name to look up
   * @return first argument value, or {@code null} if absent
   */
  public synchronized String getArgument (String name) {

    return getArgument(name, null);
  }

  /**
   * Returns the first argument value for the option identified by its single-character flag, or {@code null} if the option was not parsed or has no arguments.
   *
   * @param flag single-character flag to look up
   * @return first argument value, or {@code null} if absent
   */
  public synchronized String getArgument (char flag) {

    return getArgument(null, flag);
  }

  /**
   * Retrieves the first argument value for an option referenced by either its long name or its single-character flag.
   *
   * @param name long option name; checked first when non-{@code null}
   * @param flag single-character flag; used as fallback when name produces no match
   * @return first argument value, or {@code null} if the option was not present or carries no arguments
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
   * Returns all argument values for the option identified by its long name, or {@code null} if the option was not parsed.
   *
   * @param name long option name to look up
   * @return array of all argument values, or {@code null} if the option was absent
   */
  public synchronized String[] getArguments (String name) {

    return getArguments(name, null);
  }

  /**
   * Returns all argument values for the option identified by its single-character flag, or {@code null} if the option was not parsed.
   *
   * @param flag single-character flag to look up
   * @return array of all argument values, or {@code null} if the option was absent
   */
  public synchronized String[] getArguments (char flag) {

    return getArguments(null, flag);
  }

  /**
   * Retrieves all argument values for an option referenced by either its long name or its single-character flag.
   *
   * @param name long option name; checked first when non-{@code null}
   * @param flag single-character flag; used as fallback when name produces no match
   * @return array of all argument values, or {@code null} if the option was not present
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
   * Returns a human-readable representation of all parsed options and their argument values, useful for debugging.
   *
   * @return bracketed string listing each option and its associated arguments
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
