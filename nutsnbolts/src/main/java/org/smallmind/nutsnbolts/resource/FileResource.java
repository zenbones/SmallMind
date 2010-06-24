package org.smallmind.nutsnbolts.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileResource implements Resource {

   private File file;
   private String id;

   public FileResource (String filePath)
      throws IOException {

      file = new File(filePath);
      id = "file:" + file.getCanonicalPath();
   }

   public String getId () {

      return id;
   }

   public InputStream getInputStream ()
      throws ResourceException {

      try {
         return new FileInputStream(file);
      }
      catch (FileNotFoundException fileNotFoundException) {
         throw new ResourceException(fileNotFoundException);
      }
   }
}
