package org.smallmind.license;

import java.io.File;
import java.io.FileFilter;

public class CompoundFileFilter implements FileFilter {

  private FileFilter[] fileFilters;

  public CompoundFileFilter (FileFilter... fileFilters) {

    this.fileFilters = fileFilters;
  }

  @Override
  public boolean accept (File file) {

    for (FileFilter fileFilter : fileFilters) {
      if (fileFilter.accept(file)) {

        return true;
      }
    }

    return false;
  }
}
