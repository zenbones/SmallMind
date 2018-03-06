package org.smallmind.license;

import java.nio.file.Path;

public interface PathFilter {

  boolean accept (Path path);
}
