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
