package org.smallmind.web.json.dto.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.SimpleJavaFileObject;

public class SourceJavaFileObject extends SimpleJavaFileObject {

  private Path sourcePath;

  public SourceJavaFileObject (Path sourcePath) {

    super(sourcePath.toUri(), Kind.SOURCE);

    this.sourcePath = sourcePath;
  }

  @Override
  public CharSequence getCharContent (boolean ignoreEncodingErrors)
    throws IOException {

    return new String(Files.readAllBytes(sourcePath));
  }
}
