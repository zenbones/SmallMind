package org.smallmind.nutsnbolts.maven.sax;

import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;
import org.smallmind.nutsnbolts.xml.sax.ElementExtender;

public class SettingsElementExtender extends AbstractElementExtender {

   private String profile;

   public SettingsElementExtender (String profile) {

      this.profile = profile;
   }

   @Override
   public void completedChildElement (ElementExtender elementExtender) {

      if ((elementExtender instanceof ProfileElementExtender) && ((ProfileElementExtender)elementExtender).getId().equals(profile)) {
         ((SettingsDocumentExtender)getParent()).setPropertyMap(((ProfileElementExtender)elementExtender).getPropertyMap());
      }
   }
}