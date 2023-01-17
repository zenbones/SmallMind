package org.smallmind.license;

import java.nio.file.Path;
import java.util.regex.Pattern;

public class WildcardPathFilter implements PathFilter {

  private final Pattern namePattern;

  public WildcardPathFilter (String name) {

    namePattern = Pattern.compile(WildcardRegExTranslator.translate(name));
  }

  public boolean accept (Path path) {

    return namePattern.matcher(path.getFileName().toString()).matches();
  }
}
