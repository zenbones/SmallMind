package org.smallmind.license;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class PathTypeFilenameFilter implements PathFilter {

  private Pattern namePattern;

  public PathTypeFilenameFilter (String name) {

    namePattern = Pattern.compile(FileTypeRegExpTranslator.translate(name));
  }

  public boolean accept (Path path) {

    return namePattern.matcher(path.getFileName().toString()).matches();
  }
}
