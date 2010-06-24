package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.template.CommandGroup;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.xml.sax.SAXException;

public class CommandsElementExtender extends AbstractElementExtender {

   public void addCommandGroup (CommandGroup commandGroup) {

      ((CommandDocumentExtender)getDocumentExtender()).getCommandTemplate().addCommandGroup(commandGroup);
   }

   public void completedChildElement (ElementExtender elementExtender)
      throws SAXException {

      if (elementExtender instanceof CommandElementExtender) {

         CommandGroup commandGroup;

         commandGroup = new CommandGroup();
         commandGroup.addCommandStructure(((CommandElementExtender)elementExtender).getCommandtStructure());
         addCommandGroup(commandGroup);
      }
      else if (elementExtender instanceof GroupElementExtender) {
         addCommandGroup(((GroupElementExtender)elementExtender).getCommandGroup());
      }
   }

}
