package org.smallmind.license;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import org.apache.maven.plugin.MojoExecutionException;

public class LicensedFileIterator implements Iterator<File>, Iterable<File> {

  private CompoundFileFilter compoundFileFilter;
  private LinkedList<File> directoryStack;
  private File currentFile;

  public LicensedFileIterator (File directory, FileFilter... fileFilters)
    throws MojoExecutionException {

    if (directory.exists()) {
      if (!directory.isDirectory()) {
        throw new MojoExecutionException("Specified file(" + directory.getAbsolutePath() + ") isn't a directory");
      }
      else {
        compoundFileFilter = new CompoundFileFilter(fileFilters);
        directoryStack = new LinkedList<File>();
        directoryStack.add(directory);
        currentFile = getNextFile();
      }
    }
  }

  private File getNextFile () {

    File file;

    while (!directoryStack.isEmpty()) {
      file = directoryStack.removeFirst();
      if (file.isDirectory()) {

        LinkedList<File> appendedList = new LinkedList<File>();

        for (File child : file.listFiles(compoundFileFilter)) {
          if (child.isFile()) {
            appendedList.addFirst(child);
          }
          else {
            appendedList.addLast(child);
          }
        }

        if (!appendedList.isEmpty()) {
          directoryStack.addAll(0, appendedList);
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


