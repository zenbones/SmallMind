package org.smallmind.web.json.dto.maven;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

public class VirtualJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

  private final VirtualClassLoader classLoader;

  public VirtualJavaFileManager (JavaFileManager fileManager, VirtualClassLoader classLoader) {

    super(fileManager);

    this.classLoader = classLoader;
  }

  @Override
  public ClassLoader getClassLoader (final Location location) {

    return classLoader;
  }

  @Override
  public JavaFileObject getJavaFileForOutput (Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {

    return new TargetJavaFileObject(classLoader.getOutputStream(className));
  }
}
