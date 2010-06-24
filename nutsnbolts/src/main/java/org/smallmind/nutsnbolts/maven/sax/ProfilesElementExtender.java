package org.smallmind.nutsnbolts.maven.sax;

import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;

public class ProfilesElementExtender extends AbstractElementExtender {

   @Override
   public void completedChildElement (ElementExtender elementExtender) {

      ((SettingsElementExtender)getParent()).completedChildElement(elementExtender);
   }
}