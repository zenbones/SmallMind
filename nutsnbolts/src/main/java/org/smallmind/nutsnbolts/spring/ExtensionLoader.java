package org.smallmind.nutsnbolts.spring;

import java.io.File;
import org.smallmind.nutsnbolts.lang.ClasspathClassGate;
import org.smallmind.nutsnbolts.lang.GatingClassLoader;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class ExtensionLoader<E extends ExtensionInstance> {

  public ExtensionLoader (Class<E> extensionInstanceClass, String springFileName)
    throws ExtensionLoaderException {

    File extensionFile = new File(System.getProperty("user.dir") + "/" + springFileName);

    if (extensionFile.exists()) {

      FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(extensionFile.getAbsolutePath());
      E extensionInstance;

      try {
        extensionInstance = applicationContext.getBean(extensionInstanceClass);
      }
      catch (Throwable throwable) {
        throw new ExtensionLoaderException(throwable, "Unable to execute extension configuration(%s)", extensionFile.getAbsolutePath());
      }

      if ((extensionInstance.getClasspathComponents() != null) && (extensionInstance.getClasspathComponents().length > 0)) {

        File classpathComponentFile;
        String[] normalizedPathComponents = new String[extensionInstance.getClasspathComponents().length];
        int componentIndex = 0;

        for (String classpathComponent : extensionInstance.getClasspathComponents()) {
          classpathComponentFile = new File(classpathComponent);
          normalizedPathComponents[componentIndex++] = classpathComponentFile.isAbsolute() ? classpathComponent : System.getProperty("user.dir") + '/' + classpathComponent;
        }

        Thread.currentThread().setContextClassLoader(new GatingClassLoader(Thread.currentThread().getContextClassLoader(), -1, new ClasspathClassGate(normalizedPathComponents)));
      }
    }
  }
}