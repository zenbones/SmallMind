package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.template.CommandGroup;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.xml.sax.SAXException;

public class OptionalElementExtender extends AbstractElementExtender {

   public void completedChildElement (ElementExtender elementExtender)
      throws SAXException {

      CommandGroup commandGroup;

      if (elementExtender instanceof CommandElementExtender) {
         commandGroup = new CommandGroup(true);
         commandGroup.addCommandStructure(((CommandElementExtender)elementExtender).getCommandtStructure());
      }
      else {
         commandGroup = ((GroupElementExtender)elementExtender).getCommandGroup();
         commandGroup.setOptional(true);
      }

      ((CommandsElementExtender)getParent()).addCommandGroup(commandGroup);
   }
}
