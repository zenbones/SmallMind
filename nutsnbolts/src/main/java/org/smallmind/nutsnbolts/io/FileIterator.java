package org.smallmind.nutsnbolts.io;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class FileIterator implements Iterator<File>, Iterable<File> {

   private FileFilter fileFilter;
   private LinkedList<File> directoryStack;
   private File currentFile;

   public FileIterator(File directory) {

      this(directory, null);
   }

   public FileIterator(File directory, FileFilter fileFilter) {

      if (!directory.exists()) {
         throw new IllegalArgumentException("Specified directory(" + directory.getAbsolutePath() + ") doesn't exist");
      } else if (!directory.isDirectory()) {
         throw new IllegalArgumentException("Specified file(" + directory.getAbsolutePath() + ") isn't a directory");
      }

      this.fileFilter = fileFilter;
      directoryStack = new LinkedList<File>();
      directoryStack.add(directory);

      currentFile = getNextFile();
   }

   private File getNextFile() {

      File file;

      while (!directoryStack.isEmpty()) {
         file = directoryStack.removeFirst();
         if (file.isDirectory()) {

            LinkedList<File> appendedList = new LinkedList<File>();

            for (File child : file.listFiles(fileFilter)) {
               if (child.isFile()) {
                  appendedList.addFirst(child);
               } else {
                  appendedList.addLast(child);
               }
            }

            if (!appendedList.isEmpty()) {
               directoryStack.addAll(0, appendedList);
            }
         } else {
            return file;
         }
      }

      return null;
   }

   public boolean hasNext() {

      return currentFile != null;
   }

   public File next() {

      File nextFile;

      if (currentFile == null) {
         throw new NoSuchElementException();
      }

      nextFile = currentFile;
      currentFile = getNextFile();

      return nextFile;
   }

   public void remove() {

      throw new UnsupportedOperationException();
   }

   public Iterator<File> iterator() {

      return this;
   }
}

