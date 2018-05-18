package org.smallmind.web.json.dto.maven;

import java.io.IOException;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

public class VirtualJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

  public VirtualJavaFileManager (JavaFileManager fileManager) {

    super(fileManager);
  }
}
