package org.smallmind.nutsnbolts.command.template;

public class CommandStructure {

   private CommandArguments commandArguments;
   private String name;

   public CommandStructure (String name) {

      this(name, new CommandArguments());
   }

   public CommandStructure (String name, CommandArguments commandArguments) {

      this.name = name;
      this.commandArguments = commandArguments;
   }

   public String getName () {

      return name;
   }

   public CommandArguments getCommandArguments () {

      return commandArguments;
   }

}
