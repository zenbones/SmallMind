package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.template.CommandTemplate;
import org.smallmind.nutsnbolts.util.StringUtilities;
import org.smallmind.nutsnbolts.xml.sax.AbstractDocumentExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;
import org.smallmind.nutsnbolts.xml.sax.SAXExtender;
import org.xml.sax.Attributes;

public class CommandDocumentExtender extends AbstractDocumentExtender {

   private CommandTemplate commandTemplate;

   public CommandDocumentExtender (CommandTemplate commandTemplate) {

      this.commandTemplate = commandTemplate;
   }

   public CommandTemplate getCommandTemplate () {

      return commandTemplate;
   }

   public ElementExtender getElementExtender (SAXExtender parent, String namespaceURI, String localName, String qName, Attributes atts)
      throws Exception {

      return (ElementExtender)Class.forName(CommandDocumentExtender.class.getPackage().getName() + "." + StringUtilities.toCamelCase(qName, '-') + "ElementExtender").newInstance();
   }
}
