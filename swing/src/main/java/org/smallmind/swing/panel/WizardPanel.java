package org.smallmind.swing.panel;

import java.awt.Component;
import org.smallmind.swing.dialog.WizardDialog;

public class WizardPanel extends TabPanel {

   private WizardDialog wizardDialog = null;
   private String title;

   public WizardPanel (String title, Component component) {

      super(component);

      this.title = title;
   }

   public String getTitle () {

      return title;
   }

   public WizardDialog getWizardDialog () {

      return wizardDialog;
   }

   public void setWizardDialog (WizardDialog wizardDialog) {

      if (this.wizardDialog != null) {
         throw new IllegalArgumentException("Parent Dialog has already been set for this Panel");
      }

      this.wizardDialog = wizardDialog;
   }

   public Object getResult () {

      return wizardDialog.getResult();
   }

}
