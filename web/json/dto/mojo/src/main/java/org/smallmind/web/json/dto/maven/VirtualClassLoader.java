package org.smallmind.web.json.dto.maven;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;

public class VirtualClassLoader extends URLClassLoader implements Iterable<Class<?>> {

  private final HashMap<String, ByteArrayOutputStream> streamMap = new HashMap<>();

  public VirtualClassLoader (URL[] urls) {

    super(urls);
  }

  public ByteArrayOutputStream getOutputStream (String className) {

    ByteArrayOutputStream byteArrayOutputStream;

    streamMap.put(className, byteArrayOutputStream = new ByteArrayOutputStream());

    return byteArrayOutputStream;
  }

  @Override
  protected Class<?> findClass (String className)
    throws ClassNotFoundException {

    ByteArrayOutputStream byteArrayOutputStream;

    if ((byteArrayOutputStream = streamMap.get(className)) != null) {

      byte[] buffer = byteArrayOutputStream.toByteArray();

      return defineClass(className, buffer, 0, buffer.length);
    } else {
      throw new ClassNotFoundException(className);
    }
  }

  @Override
  public Iterator<Class<?>> iterator () {

    return new ClassIterator();
  }

  private class ClassIterator implements Iterator<Class<?>> {

    Iterator<String> classNameIterator = streamMap.keySet().iterator();

    @Override
    public boolean hasNext () {

      return classNameIterator.hasNext();
    }

    @Override
    public Class<?> next () {

      try {
        return findClass(classNameIterator.next());
      } catch (ClassNotFoundException classNotFoundException) {
        throw new RuntimeException(classNotFoundException);
      }
    }
  }
}
