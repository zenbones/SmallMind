/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.command;

public class CommandLineParser {

  private static enum ArgumentType {

    ARGUMENT, COMMAND_SHORT, COMMAND_LONG
  }

  public static CommandSet parseCommands (String[] args)
    throws CommandException {

    CommandSet commandSet;
    String command;
    String parameter;
    ArgumentType argumentType;

    commandSet = new CommandSet();
    command = null;
    for (int count = 0; count < args.length; count++) {
      if (args[count].startsWith("--")) {
        argumentType = ArgumentType.COMMAND_LONG;
        parameter = args[count].substring(2);
      }
      else if (args[count].startsWith("-")) {
        argumentType = ArgumentType.COMMAND_SHORT;
        parameter = args[count].substring(1);
      }
      else {
        argumentType = ArgumentType.ARGUMENT;
        if (args[count].startsWith("\"")) {
          if (args[count].endsWith("\"")) {
            parameter = args[count].substring(1, args[count].length() - 1);
          }
          else {
            parameter = args[count].substring(1);
            do {
              count++;
              if (count == args.length) {
                throw new CommandException("Unterminated quoted argument after command (%s)", command);
              }
              else {
                if (args[count].endsWith("\"")) {
                  parameter += " " + args[count].substring(0, args[count].length() - 1);
                  break;
                }
                else {
                  parameter += " " + args[count];
                }
              }
            } while (true);
          }
        }
        else {
          parameter = args[count];
        }
      }

      switch (argumentType) {
        case ARGUMENT:
          if (command == null) {
            throw new CommandException("No command for argument (%s)", parameter);
          }
          commandSet.addArgument(command, parameter);
          break;
        case COMMAND_SHORT:
          if (parameter.equals("")) {
            throw new CommandException("Empty short command after prefix '-'");
          }
          for (int loop = 0; loop < parameter.length(); loop++) {
            commandSet.addCommand(parameter.substring(loop, loop + 1));
          }
          command = parameter.substring(parameter.length() - 1);
          break;
        case COMMAND_LONG:
          if (parameter.equals("")) {
            throw new CommandException("Empty long command after prefix '--'");
          }
          commandSet.addCommand(parameter);
          command = parameter;
      }
    }

    return commandSet;
  }
}
