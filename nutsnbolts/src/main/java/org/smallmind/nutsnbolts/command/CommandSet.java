/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class CommandSet {

   private HashMap<String, LinkedList<String>> commandMap;

   public CommandSet () {

      commandMap = new HashMap<String, LinkedList<String>>();
   }

   public synchronized void addCommand (String command) {

      if (!commandMap.containsKey(command)) {
         commandMap.put(command, new LinkedList<String>());
      }
   }

   public synchronized void addArgument (String command, String argument) {

      addCommand(command);
      (commandMap.get(command)).add(argument);
   }

   public synchronized boolean containsCommand (String command) {

      return commandMap.containsKey(command);
   }

   public synchronized boolean containsAllCommands (String[] commands) {

      for (String command : commands) {
         if (!containsCommand(command)) {
            return false;
         }
      }

      return true;
   }

   public synchronized String[] getCommands () {

      String[] commands;

      commands = new String[commandMap.size()];
      commandMap.keySet().toArray(commands);

      return commands;
   }

   public synchronized String getArgument (String command) {

      LinkedList<String> argumentList;

      if ((argumentList = commandMap.get(command)) != null) {
         if (!argumentList.isEmpty()) {
            return argumentList.getFirst();
         }
      }

      return null;
   }

   public synchronized String[] getArguments (String command) {

      LinkedList<String> argumentList;
      String[] arguments = null;

      if ((argumentList = commandMap.get(command)) != null) {
         arguments = new String[argumentList.size()];
         argumentList.toArray(arguments);
      }

      return arguments;
   }

   public String toString () {

      StringBuilder lineBuilder;
      Iterator<String> commandIter;
      Iterator argumentIter;
      String command;
      String argument;
      boolean first = true;

      lineBuilder = new StringBuilder();
      commandIter = commandMap.keySet().iterator();
      while (commandIter.hasNext()) {
         command = commandIter.next();
         if (!first) {
            lineBuilder.append(' ');
         }
         lineBuilder.append("--");
         lineBuilder.append(command);
         first = false;

         argumentIter = (commandMap.get(command)).iterator();
         while (argumentIter.hasNext()) {
            argument = (String)argumentIter.next();
            lineBuilder.append(' ');
            if (argument.indexOf(' ') >= 0) {
               lineBuilder.append('"');
            }
            lineBuilder.append(argument);
            if (argument.indexOf(' ') >= 0) {
               lineBuilder.append('"');
            }
         }
      }

      return lineBuilder.toString();
   }

}
