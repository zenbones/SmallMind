package org.smallmind.nutsnbolts.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileResource extends AbstractResource {

   public FileResource (String path) {

      super(path);
   }

   public String getScheme () {

      return "file";
   }

   public InputStream getInputStream ()
      throws FileNotFoundException {

      return new FileInputStream(new File(getPath()));

   }
}
