package org.smallmind.nutsnbolts.command.template;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.command.CommandException;
import org.smallmind.nutsnbolts.command.CommandSet;
import org.smallmind.nutsnbolts.command.sax.CommandDocumentExtender;
import org.smallmind.nutsnbolts.xml.SmallMindProtocolResolver;
import org.smallmind.nutsnbolts.xml.XMLEntityResolver;
import org.smallmind.nutsnbolts.xml.sax.ExtensibleSAXParser;
import org.xml.sax.InputSource;

public class CommandTemplate {

   private static final XMLEntityResolver SMALL_MIND_ENTITY_RESOLVER = new XMLEntityResolver(SmallMindProtocolResolver.getInstance());

   private LinkedList<CommandGroup> groupList;
   private String shortName;

   public static CommandTemplate createCommandTemplate (Class entryClass)
      throws CommandException {

      CommandTemplate commandTemplate;
      CommandDocumentExtender argumentDocumentExtender;
      InputStream argumentsInputStream;

      commandTemplate = new CommandTemplate(entryClass);
      argumentDocumentExtender = new CommandDocumentExtender(commandTemplate);

      try {
         if ((argumentsInputStream = CommandTemplate.class.getClassLoader().getResourceAsStream("arguments/" + entryClass.getCanonicalName().replace('.', '/') + ".args")) == null) {
            throw new CommandException("No arguments file found matching Class(%s)", entryClass.getCanonicalName());
         }
         ExtensibleSAXParser.parse(argumentDocumentExtender, new InputSource(argumentsInputStream), SMALL_MIND_ENTITY_RESOLVER);
      }
      catch (Exception e) {
         throw new CommandException(e);
      }

      return commandTemplate;
   }

   public CommandTemplate (Class entryClass) {

      shortName = entryClass.getSimpleName();
      groupList = new LinkedList<CommandGroup>();
   }

   public String getShortName () {

      return shortName;
   }

   public synchronized CommandGroup[] getCommandGroups () {

      CommandGroup[] commandGroups;

      commandGroups = new CommandGroup[groupList.size()];
      groupList.toArray(commandGroups);

      return commandGroups;
   }

   public synchronized void addCommandGroup (CommandGroup commandGroup) {

      groupList.add(commandGroup);
   }

   public synchronized String validateCommandSet (CommandSet commandSet) {

      Iterator<CommandGroup> groupIter;
      CommandGroup commandGroup;
      CommandStructure[] commandStructures;
      CommandArguments commandArguments;
      String[] templateArguments;
      String[] userArguments;
      boolean argumentMatch;
      int commandCount;

      groupIter = groupList.iterator();
      while (groupIter.hasNext()) {
         commandGroup = groupIter.next();
         commandCount = 0;

         commandStructures = commandGroup.getCommandStructures();
         for (CommandStructure commandStructure : commandStructures) {
            if (commandSet.containsCommand(commandStructure.getName())) {
               commandCount++;
               commandArguments = commandStructure.getCommandArguments();

               if (!commandArguments.areUnrestricted()) {
                  userArguments = commandSet.getArguments(commandStructure.getName());
                  for (String userArgument : userArguments) {
                     argumentMatch = false;

                     templateArguments = commandArguments.getArguments();
                     for (String templateArgument : templateArguments) {
                        if (userArgument.equals(templateArgument)) {
                           argumentMatch = true;
                           break;
                        }
                     }

                     if (!argumentMatch) {
                        return getCommandLineDescription();
                     }
                  }
               }
            }
         }

         if ((commandCount > 1) || ((!commandGroup.isOptional()) && (commandCount == 0))) {
            return getCommandLineDescription();
         }
      }

      return null;
   }

   public synchronized String getCommandLineDescription () {

      StringBuilder descriptionBuilder;
      Iterator<CommandGroup> groupIter;
      CommandGroup commandGroup;
      CommandStructure[] commandStructures;
      CommandArguments commandArguments;
      String[] arguments;

      descriptionBuilder = new StringBuilder(shortName);

      groupIter = groupList.iterator();
      while (groupIter.hasNext()) {
         commandGroup = groupIter.next();

         descriptionBuilder.append(' ');
         if (!commandGroup.isNominalGroup()) {
            if (commandGroup.isOptional()) {
               descriptionBuilder.append('<');
            }
            else {
               descriptionBuilder.append('{');
            }
         }

         commandStructures = commandGroup.getCommandStructures();
         for (int count = 0; count < commandStructures.length; count++) {
            if (count > 0) {
               descriptionBuilder.append(' ');
            }

            if (commandGroup.isOptional() && commandGroup.isNominalGroup()) {
               descriptionBuilder.append('<');
            }
            else {
               descriptionBuilder.append('[');
            }

            descriptionBuilder.append(commandStructures[count].getName());

            commandArguments = commandStructures[count].getCommandArguments();
            if (!commandArguments.areUnrestricted()) {
               descriptionBuilder.append('(');
               arguments = commandArguments.getArguments();
               for (int loop = 0; loop < arguments.length; loop++) {
                  if (loop > 0) {
                     descriptionBuilder.append('|');
                  }
                  descriptionBuilder.append(arguments[loop]);
               }
               descriptionBuilder.append(')');
            }

            if (commandGroup.isOptional() && commandGroup.isNominalGroup()) {
               descriptionBuilder.append('>');
            }
            else {
               descriptionBuilder.append(']');
            }
         }

         if (!commandGroup.isNominalGroup()) {
            if (commandGroup.isOptional()) {
               descriptionBuilder.append('>');
            }
            else {
               descriptionBuilder.append('}');
            }
         }
      }

      return descriptionBuilder.toString();
   }

}