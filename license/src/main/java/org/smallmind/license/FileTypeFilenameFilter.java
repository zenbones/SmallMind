package org.smallmind.license;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class FileTypeFilenameFilter implements FileFilter {

  private Pattern namePattern;

  public FileTypeFilenameFilter (String name) {

    namePattern = Pattern.compile(FileTypeRegExpTranslator.translate(name));
  }

  public boolean accept (File file) {

    return file.isDirectory() || namePattern.matcher(file.getName()).matches();
  }

}
