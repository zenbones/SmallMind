package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class FileIterator implements Iterator<File>, Iterable<File> {

   private FileFilter fileFilter;
   private LinkedList<LinkedList<File>> directoryStack;
   private File currentFile;

   public FileIterator (File directory) {

      this(directory, null);
      currentFile = getNextFile();
   }

   public FileIterator (File directory, FileFilter fileFilter) {

      if (!directory.exists()) {
         throw new IllegalArgumentException("Specified directory(" + directory.getAbsolutePath() + ") doesn't exist");
      }
      else if (!directory.isDirectory()) {
         throw new IllegalArgumentException("Specified file(" + directory.getAbsolutePath() + ") isn't a directory");
      }

      LinkedList<File> rootList;

      this.fileFilter = fileFilter;

      rootList = new LinkedList<File>();
      rootList.add(directory);
      directoryStack = new LinkedList<LinkedList<File>>();
      directoryStack.add(rootList);

      currentFile = getNextFile();
   }

   private File getNextFile () {

      File file;
      File[] listing;

      while (!directoryStack.isEmpty()) {
         file = directoryStack.getFirst().removeFirst();
         if (directoryStack.getFirst().isEmpty()) {
            directoryStack.removeFirst();
         }
         if (file.isDirectory()) {
            if ((listing = file.listFiles(fileFilter)).length > 0) {
               directoryStack.addFirst(new LinkedList<File>(Arrays.asList(listing)));
            }
         }
         else {
            return file;
         }
      }

      return null;
   }

   public boolean hasNext () {

      return currentFile != null;
   }

   public File next () {

      File nextFile;

      if (currentFile == null) {
         throw new NoSuchElementException();
      }

      nextFile = currentFile;
      currentFile = getNextFile();

      return nextFile;
   }

   public void remove () {

      throw new UnsupportedOperationException();
   }

   public Iterator<File> iterator () {

      return this;
   }
}

