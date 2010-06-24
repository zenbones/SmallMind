package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.template.CommandGroup;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class GroupElementExtender extends AbstractElementExtender {

   CommandGroup commandGroup;

   public CommandGroup getCommandGroup () {

      return commandGroup;
   }

   public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {

      commandGroup = new CommandGroup();
   }

   public void completedChildElement (ElementExtender elementExtender)
      throws SAXException {

      commandGroup.addCommandStructure(((CommandElementExtender)elementExtender).getCommandtStructure());
   }
}
