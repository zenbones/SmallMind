package org.smallmind.license;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import org.apache.maven.plugin.MojoExecutionException;

public class LicensedFileIterable implements Iterable<File> {

  private final File directory;
  private final FileFilter[] fileFilters;

  public LicensedFileIterable (File directory, FileFilter[] fileFilters)
    throws MojoExecutionException {

    this.directory = directory;
    this.fileFilters = fileFilters;

    if (directory.exists()) {
      if (!directory.isDirectory()) {
        throw new MojoExecutionException("Specified file(" + directory.getAbsolutePath() + ") isn't a directory");
      }
    }
  }

  public Iterator<File> iterator () {

    return new LicensedFileIterator(directory, fileFilters);
  }

  private class LicensedFileIterator implements Iterator<File> {

    private CompoundFileFilter compoundFileFilter;
    private LinkedList<File> directoryStack;
    private File currentFile;

    private LicensedFileIterator (File directory, FileFilter... fileFilters) {

      compoundFileFilter = new CompoundFileFilter(fileFilters);
      directoryStack = new LinkedList<>();
      directoryStack.add(directory);
      currentFile = getNextFile();
    }

    private File getNextFile () {

      File file;

      while (!directoryStack.isEmpty()) {
        if ((file = directoryStack.removeFirst()).exists()) {
          if (file.isDirectory()) {

            LinkedList<File> appendedList = new LinkedList<>();

            for (File child : file.listFiles(compoundFileFilter)) {
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
  }
}


