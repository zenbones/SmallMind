package org.smallmind.file.jailed;

import java.nio.file.Path;

public abstract class AbstractJailedPathTranslator implements JailedPathTranslator {

  public Path wrapPath (Path rootPath, JailedFileSystem jailedFileSystem, Path nativePath) {

    if (nativePath.isAbsolute()) {
      if (!nativePath.startsWith(rootPath)) {
        throw new SecurityException("No authorization for path");
      } else {

        StringBuilder pathBuilder = new StringBuilder();

        for (int index = rootPath.getNameCount(); index < nativePath.getNameCount(); index++) {
          pathBuilder.append(jailedFileSystem.getSeparator()).append(nativePath.getName(index));
        }

        return new JailedPath(jailedFileSystem, pathBuilder.toString());
      }
    } else {

      StringBuilder pathBuilder = new StringBuilder();

      for (int index = 0; index < nativePath.getNameCount(); index++) {
        pathBuilder.append('/').append(nativePath.getName(index));
      }

      return new JailedPath(jailedFileSystem, pathBuilder.toString());
    }
  }

  public Path unwrapPath (Path rootPath, Path jailedPath) {

    StringBuilder pathBuilder = new StringBuilder();

    for (int index = 0; index < jailedPath.getNameCount(); index++) {
      if (index > 0) {
        pathBuilder.append(getNativeFileSystem().getSeparator());
      }
      pathBuilder.append(jailedPath.getName(index));
    }

    return rootPath.resolve(pathBuilder.toString());
  }
}
