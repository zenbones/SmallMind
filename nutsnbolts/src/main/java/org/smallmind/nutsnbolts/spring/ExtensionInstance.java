package org.smallmind.nutsnbolts.spring;

public interface ExtensionInstance {

  public abstract String[] getClasspathComponents ();

  public abstract void setClasspathComponents (String[] classpathComponents);
}
