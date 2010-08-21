package org.smallmind.swing.file;

import java.io.File;
import java.util.LinkedList;

public class Directory extends File {

   private static final Directory[] NO_CHILDREN = new Directory[0];

   public Directory (String pathName) {

      super(pathName);

      if (!isDirectory()) {
         throw new IllegalStateException("The path name(" + pathName + ") does not represent a directory structure");
      }
   }

   public boolean hasChildren () {

      File[] files;

      if ((files = listFiles()) == null) {
         return false;
      }

      for (File file : files) {
         if (file.isDirectory()) {
            return true;
         }
      }

      return false;
   }

   public Directory[] getChildren () {

      Directory[] directories;
      LinkedList<Directory> directoryList;
      File[] files;

      if ((files = listFiles()) == null) {
         return NO_CHILDREN;
      }

      directoryList = new LinkedList<Directory>();

      for (File file : files) {
         if (file.isDirectory()) {
            directoryList.add(new Directory(file.getAbsolutePath()));
         }
      }

      directories = new Directory[directoryList.size()];
      directoryList.toArray(directories);

      return directories;
   }

}
