package org.smallmind.web.json.dto.maven;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;

public class VirtualJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

  public VirtualJavaFileManager (JavaFileManager fileManager) {

    super(fileManager);
  }
}
