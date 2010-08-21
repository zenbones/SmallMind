package org.smallmind.swing.event;

import java.io.File;
import java.util.EventObject;

public class DirectoryChoiceEvent extends EventObject {

   private File file;

   public DirectoryChoiceEvent (Object source, File file) {

      super(source);

      this.file = file;
   }

   public File getChosenDirectory () {

      return file;
   }

}
