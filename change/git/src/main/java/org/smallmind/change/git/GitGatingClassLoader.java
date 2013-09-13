package org.smallmind.change.git;

import org.smallmind.nutsnbolts.lang.GatingClassLoader;

public class GitGatingClassLoader extends GatingClassLoader {

  public GitGatingClassLoader (ClassLoader parent, int reloadInterval) {

    super(parent, reloadInterval);
  }
}
