package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.template.CommandStructure;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class CommandElementExtender extends AbstractElementExtender {

   CommandStructure commandStructure;

   public CommandStructure getCommandtStructure () {

      return commandStructure;
   }

   public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {

      commandStructure = new CommandStructure(atts.getValue("name"));
   }

   public void completedChildElement (ElementExtender elementExtender)
      throws SAXException {

      commandStructure.getCommandArguments().addArgument(((ArgumentElementExtender)elementExtender).getValue());
   }
}
