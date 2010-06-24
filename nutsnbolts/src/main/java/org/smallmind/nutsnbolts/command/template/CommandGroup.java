package org.smallmind.nutsnbolts.command.template;

import java.util.LinkedList;

public class CommandGroup {

   private LinkedList<CommandStructure> structureList;
   private boolean optional;

   public CommandGroup () {

      this(false);
   }

   public CommandGroup (boolean optional) {

      this.optional = optional;

      structureList = new LinkedList<CommandStructure>();
   }

   public void setOptional (boolean optional) {

      this.optional = optional;
   }

   public boolean isOptional () {

      return optional;
   }

   public synchronized boolean isNominalGroup () {

      return (structureList.size() < 2);
   }

   public synchronized CommandStructure[] getCommandStructures () {

      CommandStructure[] commandStructures;

      commandStructures = new CommandStructure[structureList.size()];
      structureList.toArray(commandStructures);

      return commandStructures;
   }

   public synchronized void addCommandStructure (CommandStructure commandStructure) {

      structureList.add(commandStructure);
   }

}