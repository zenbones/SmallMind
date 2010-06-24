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
