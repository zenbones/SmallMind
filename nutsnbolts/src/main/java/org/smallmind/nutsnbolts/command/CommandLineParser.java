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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.command.template.ArgumentType;
import org.smallmind.nutsnbolts.command.template.EnumeratedArgument;
import org.smallmind.nutsnbolts.command.template.Option;
import org.smallmind.nutsnbolts.command.template.Template;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.Counter;

/**
 * Parses raw command line arguments against a {@link Template}, supporting GNU-style long options ({@code --name}) and single-character flags ({@code -f}).
 */
public class CommandLineParser {

  /**
   * Parses a command line using the supplied template, rejecting any non-option arguments.
   *
   * @param template template describing the valid options and their argument types
   * @param args     raw command line argument tokens
   * @return populated {@link OptionSet} reflecting all parsed options and their values
   * @throws CommandLineException if an unknown option is encountered, a required option is absent, or a dependent option is used without its parent
   */
  public static OptionSet parseCommands (Template template, String[] args)
    throws CommandLineException {

    return parseCommands(template, args, false);
  }

  /**
   * Parses a command line using the supplied template, optionally collecting non-option tokens as undeclared trailing arguments.
   *
   * @param template        template describing the valid options and their argument types
   * @param args            raw command line argument tokens
   * @param allowUndeclared when {@code true}, tokens that do not start with {@code -} or {@code --} are stored as remaining arguments instead of causing an error
   * @return populated {@link OptionSet} reflecting all parsed options and their values
   * @throws CommandLineException if an unknown option is encountered, a required option is absent, or a dependent option is used without its parent
   */
  public static OptionSet parseCommands (Template template, String[] args, boolean allowUndeclared)
    throws CommandLineException {

    OptionSet optionSet = new OptionSet();
    HashSet<Option> unusedSet = new HashSet<>(template.getOptionSet());
    HashSet<Option> usedSet = new HashSet<>();
    Counter argCounter = new Counter(-1);
    Option matchingOption;
    String matchingArgument;
    int flagIndex;

    while (argCounter.incAndGet() < args.length) {
      if (args[argCounter.get()].startsWith("--")) {
        if (args[argCounter.get()].length() == 2) {
          throw new CommandLineException("Missing option after '--'");
        } else {

          String name;

          if ((matchingOption = findUnusedOptionByName(unusedSet, name = args[argCounter.get()].substring(2))) == null) {

            Option usedOption;

            if ((usedOption = findUsedOptionByName(usedSet, name)) != null) {
              if (ArgumentType.MULTIPLE.equals(usedOption.getArgument().getType())) {
                matchingOption = usedOption;
              } else {
                throw new CommandLineException("The option name '--%s' has already been invoked", name);
              }
            } else {
              throw new CommandLineException("No such option name '--%s'", name);
            }
          } else {
            usedSet.add(matchingOption);
          }
        }

        switch (matchingOption.getArgument().getType()) {
          case NONE:
            optionSet.addOption(matchingOption.getName());
            break;
          case SINGLE:
            optionSet.addArgument(matchingOption.getName(), obtainArgument(null, argCounter, args));
            break;
          case MULTIPLE:
            optionSet.addArgument(matchingOption.getName(), obtainArgument(null, argCounter, args));
            break;
          case LIST:
            for (String argument : obtainArguments(null, argCounter, args)) {
              optionSet.addArgument(matchingOption.getName(), argument);
            }
            break;
          case ENUMERATED:
            if (!(((EnumeratedArgument)matchingOption.getArgument()).matches(matchingArgument = obtainArgument(null, argCounter, args)))) {
              throw new CommandLineException("Argument for the option(%s) is not within its bound %s", matchingOption.getName(), Arrays.toString(((EnumeratedArgument)matchingOption.getArgument()).getValues()));
            }

            optionSet.addArgument(matchingOption.getName(), matchingArgument);
            break;
          default:
            throw new UnknownSwitchCaseException(matchingOption.getArgument().getType().name());
        }
      } else if (args[argCounter.get()].startsWith("-")) {
        if (args[argCounter.get()].length() > 2) {
          throw new CommandLineException("The option(%s) is not a flag (flags are single characters)", args[argCounter.get()]);
        } else if (args[argCounter.get()].length() == 1) {
          throw new CommandLineException("Missing option after '-'");
        }

        flagIndex = 1;
        while (flagIndex < args[argCounter.get()].length()) {

          char flagChar;

          if ((matchingOption = findUnusedOptionByFlag(unusedSet, flagChar = args[argCounter.get()].charAt(flagIndex++))) == null) {

            Option usedOption;

            if ((usedOption = findUsedOptionByFlag(usedSet, flagChar)) != null) {
              if (ArgumentType.MULTIPLE.equals(usedOption.getArgument().getType())) {
                matchingOption = usedOption;
              } else {
                throw new CommandLineException("The option flag '-%s' has already been invoked", String.valueOf(flagChar));
              }
            } else {
              throw new CommandLineException("No such option flag '-%s'", String.valueOf(flagChar));
            }
          } else {
            usedSet.add(matchingOption);
          }

          switch (matchingOption.getArgument().getType()) {
            case NONE:
              optionSet.addOption(matchingOption.getName());
              break;
            case SINGLE:
              optionSet.addArgument(matchingOption.getName(), obtainArgument(args[argCounter.get()].substring(flagIndex), argCounter, args));
              flagIndex = args[argCounter.get()].length();
              break;
            case MULTIPLE:
              optionSet.addArgument(matchingOption.getName(), obtainArgument(args[argCounter.get()].substring(flagIndex), argCounter, args));
              flagIndex = args[argCounter.get()].length();
              break;
            case LIST:
              for (String argument : obtainArguments(args[argCounter.get()].substring(flagIndex), argCounter, args)) {
                optionSet.addArgument(matchingOption.getName(), argument);
              }
              flagIndex = args[argCounter.get()].length();
              break;
            case ENUMERATED:
              if (!(((EnumeratedArgument)matchingOption.getArgument()).matches(matchingArgument = obtainArgument(args[argCounter.get()].substring(flagIndex), argCounter, args)))) {
                throw new CommandLineException("Argument for the option with flag(%s) is not within its bound %s", matchingOption.getFlag().toString(), Arrays.toString(((EnumeratedArgument)matchingOption.getArgument()).getValues()));
              }

              optionSet.addArgument(matchingOption.getName(), matchingArgument);
              flagIndex = args[argCounter.get()].length();
              break;
            default:
              throw new UnknownSwitchCaseException(matchingOption.getArgument().getType().name());
          }
        }
      } else if (allowUndeclared) {
        optionSet.addRemaining(args[argCounter.get()]);
      } else {
        throw new CommandLineException("Was not expecting arguments, but an option, which must start with either '--' or '-'");
      }
    }

    for (Option unusedOption : unusedSet) {
      if (unusedOption.isRequired()) {
        throw new CommandLineException("Missing required option '%s'", getOptionName(unusedOption));
      }
    }

    for (Option usedOption : usedSet) {
      if (usedOption.getParent() != null) {
        if (!usedSet.contains(usedOption.getParent())) {
          throw new CommandLineException("Use of dependent option '%s' without specifying its parent '%s'", getOptionName(usedOption), getOptionName(usedOption.getParent()));
        }
      }
    }

    return optionSet;
  }

  /**
   * Returns a human-readable representation of an option for use in error messages, preferring the long name over the single-character flag.
   *
   * @param option option to format
   * @return string of the form {@code --name} or {@code -f}
   */
  private static String getOptionName (Option option) {

    return ((option.getName() != null) && (!option.getName().isEmpty())) ? "--" + option.getName() : "-" + option.getFlag().toString();
  }

  /**
   * Collects a variable-length sequence of argument values, stopping before the next token that begins with a dash.
   *
   * @param currentString already-consumed partial token, or {@code null} to read the next argument
   * @param argCounter    mutable index into {@code args}
   * @param args          full argument array
   * @return array of collected argument values
   * @throws CommandLineException if a required argument is missing or contains an unterminated quoted string
   */
  private static String[] obtainArguments (String currentString, Counter argCounter, String[] args)
    throws CommandLineException {

    String[] arguments;
    LinkedList<String> argumentList = new LinkedList<>();

    do {
      argumentList.add(obtainArgument(argumentList.isEmpty() ? currentString : null, argCounter, args));
    } while ((argCounter.get() + 1 < args.length) && (args[argCounter.get() + 1].charAt(0) != '-'));

    arguments = new String[argumentList.size()];
    argumentList.toArray(arguments);

    return arguments;
  }

  /**
   * Reads a single argument value, stitching together multiple tokens when the value is delimited by single or double quotes.
   *
   * @param currentString already-consumed partial token to use as the argument, or {@code null} to advance {@code argCounter} and read the next token
   * @param argCounter    mutable index into {@code args}
   * @param args          full argument array
   * @return the argument value as a string, including any surrounding quote characters
   * @throws CommandLineException if no argument is available or a quoted argument is not terminated
   */
  private static String obtainArgument (String currentString, Counter argCounter, String[] args)
    throws CommandLineException {

    StringBuilder argumentBuilder;

    if ((currentString != null) && (!currentString.isEmpty())) {
      argumentBuilder = new StringBuilder(currentString);
    } else if (args[argCounter.incAndGet()].charAt(0) != '-') {
      argumentBuilder = new StringBuilder(args[argCounter.get()]);
    } else {
      throw new CommandLineException("Missing argument for option marked as requiring arguments");
    }

    if ((argumentBuilder.charAt(0) == '\'') || (argumentBuilder.charAt(0) == '"')) {

      char delimiter = argumentBuilder.charAt(0);

      while ((argumentBuilder.charAt(argumentBuilder.length() - 1) != delimiter) && (argCounter.get() + 1 < args.length)) {
        argumentBuilder.append(' ').append(args[argCounter.incAndGet()]);
      }

      if (argumentBuilder.charAt(argumentBuilder.length() - 1) != delimiter) {
        throw new CommandLineException("Unterminated quote delimited argument");
      }

      return argumentBuilder.toString();
    } else {

      return argumentBuilder.toString();
    }
  }

  /**
   * Finds an option in the unused set whose flag matches the given character, removing it from the set when found.
   *
   * @param unusedSet set of options not yet encountered during parsing
   * @param flag      flag character to match
   * @return matching {@link Option}, or {@code null} if not found
   */
  private static Option findUnusedOptionByFlag (HashSet<Option> unusedSet, Character flag) {

    for (Option option : unusedSet) {
      if (flag.equals(option.getFlag())) {
        unusedSet.remove(option);

        return option;
      }
    }

    return null;
  }

  /**
   * Finds an option in the unused set whose long name matches the given string, removing it from the set when found.
   *
   * @param unusedSet set of options not yet encountered during parsing
   * @param name      long option name to match
   * @return matching {@link Option}, or {@code null} if not found
   */
  private static Option findUnusedOptionByName (HashSet<Option> unusedSet, String name) {

    for (Option option : unusedSet) {
      if (name.equals(option.getName())) {
        unusedSet.remove(option);

        return option;
      }
    }

    return null;
  }

  /**
   * Searches the used set for an option whose flag matches the given character without modifying the set.
   *
   * @param usedSet set of options already encountered during parsing
   * @param flag    flag character to match
   * @return matching {@link Option}, or {@code null} if not found
   */
  private static Option findUsedOptionByFlag (HashSet<Option> usedSet, Character flag) {

    for (Option option : usedSet) {
      if (flag.equals(option.getFlag())) {

        return option;
      }
    }

    return null;
  }

  /**
   * Searches the used set for an option whose long name matches the given string without modifying the set.
   *
   * @param usedSet set of options already encountered during parsing
   * @param name    long option name to match
   * @return matching {@link Option}, or {@code null} if not found
   */
  private static Option findUsedOptionByName (HashSet<Option> usedSet, String name) {

    for (Option option : usedSet) {
      if (name.equals(option.getName())) {

        return option;
      }
    }

    return null;
  }
}
