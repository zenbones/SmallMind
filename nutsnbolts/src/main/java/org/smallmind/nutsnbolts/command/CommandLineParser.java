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

import java.util.Arrays;
import java.util.HashSet;
import org.smallmind.nutsnbolts.command.template.EnumeratedArgument;
import org.smallmind.nutsnbolts.command.template.Option;
import org.smallmind.nutsnbolts.command.template.Template;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class CommandLineParser {

  public static OptionSet parseCommands (Template template, String[] args)
    throws CommandLineException {

    OptionSet optionSet = new OptionSet();
    HashSet<Option> unusedSet = new HashSet<>(template.getOptionSet());
    Option matchingOption;
    String matchingArgument;
    int argIndex = 0;
    int flagIndex;

    while (argIndex < args.length) {
      if (args[argIndex].startsWith("-")) {
        if (args[argIndex].length() == 1) {
          throw new CommandLineException("Missing option after '-'");
        }

        flagIndex = 1;
        while (flagIndex < args[argIndex].length()) {
          if ((matchingOption = findUnusedOptionByFlag(unusedSet, args[argIndex].charAt(flagIndex++))) == null) {

            return null;
          }

          switch (matchingOption.getArgument().getType()) {
            case NONE:
              optionSet.addOption(matchingOption.getName());
              break;
            case SINGLE:
              if (flagIndex == args[argIndex].length()) {
                throw new CommandLineException("Missing argument for option with flag(%s)", matchingOption.getFlag().toString());
              }

              optionSet.addArgument(matchingOption.getName(), args[argIndex].substring(flagIndex));
              flagIndex = args[argIndex].length();
              break;
            case LIST:
              if (flagIndex == args[argIndex].length()) {
                throw new CommandLineException("Missing argument for option with flag(%s)", matchingOption.getFlag().toString());
              }

              optionSet.addArgument(matchingOption.getName(), args[argIndex].substring(flagIndex));

              

              flagIndex = args[argIndex].length();
              break;
            case ENUMERATED:
              if (flagIndex == args[argIndex].length()) {
                throw new CommandLineException("Missing argument for option with flag(%s)", matchingOption.getFlag().toString());
              }
              if (!(((EnumeratedArgument)matchingOption.getArgument()).matches(matchingArgument = args[argIndex].substring(flagIndex)))) {
                throw new CommandLineException("Argument for the option with flag(%s) is not within its bound %s", matchingOption.getFlag().toString(), Arrays.toString(((EnumeratedArgument)matchingOption.getArgument()).getValues()));
              }

              optionSet.addArgument(matchingOption.getName(), matchingArgument);
              flagIndex = args[argIndex].length();
              break;
            default:
              throw new UnknownSwitchCaseException(matchingOption.getArgument().getType().name());
          }
        }
      } else if (args[argIndex].startsWith("--")) {
        if (args[argIndex].length() == 2) {
          throw new CommandLineException("Missing option after '--'");
        }
        if ((matchingOption = findUnusedOptionByName(unusedSet, args[argIndex].substring(2))) == null) {

          return null;
        }

        switch (matchingOption.getArgument().getType()) {
          case NONE:
            optionSet.addOption(matchingOption.getName());
            break;
          case SINGLE:
            break;
          case LIST:
            break;
          case ENUMERATED:
            break;
          default:
            throw new UnknownSwitchCaseException(matchingOption.getArgument().getType().name());
        }
      } else {
        throw new CommandLineException("Expected an option, which must start with either '--' or '-'");
      }
    }

    return optionSet;
  }

  private static Option findUnusedOptionByFlag (HashSet<Option> unusedSet, Character flag) {

    for (Option option : unusedSet) {
      if (flag.equals(option.getFlag())) {
        unusedSet.remove(option);

        return option;
      }
    }

    return null;
  }

  private static Option findUnusedOptionByName (HashSet<Option> unusedSet, String name) {

    for (Option option : unusedSet) {
      if (name.equals(option.getName())) {
        unusedSet.remove(option);

        return option;
      }
    }

    return null;
  }
}
