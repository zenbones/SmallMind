package org.smallmind.file.ephemeral;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

public class RegexPathMatcher implements PathMatcher {

  Pattern pattern;

  public RegexPathMatcher (Pattern pattern) {

    this.pattern = pattern;
  }

  @Override
  public boolean matches (Path path) {

    return pattern.matcher(path.toString()).matches();
  }
}
