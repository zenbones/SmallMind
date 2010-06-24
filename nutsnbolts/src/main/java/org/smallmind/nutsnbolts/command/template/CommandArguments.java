package org.smallmind.nutsnbolts.command.template;

import java.util.LinkedList;

public class CommandArguments {

   private LinkedList<String> argumentList;

   public CommandArguments () {

      argumentList = new LinkedList<String>();
   }

   public synchronized boolean areUnrestricted () {

      return argumentList.isEmpty();
   }

   public synchronized String[] getArguments () {

      String[] arguments;

      arguments = new String[argumentList.size()];
      argumentList.toArray(arguments);

      return arguments;
   }

   public synchronized void addArgument (String argument) {

      argumentList.add(argument);
   }

}
