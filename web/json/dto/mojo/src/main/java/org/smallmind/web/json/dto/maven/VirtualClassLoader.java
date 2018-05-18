package org.smallmind.web.json.dto.maven;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import org.smallmind.nutsnbolts.lang.ClasspathClassGate;
import org.smallmind.nutsnbolts.lang.GatingClassLoader;

public class VirtualClassLoader extends GatingClassLoader implements Iterable<Class<?>> {

  private final HashMap<String, ByteArrayOutputStream> streamMap;

  public VirtualClassLoader (ClassLoader parent, String... dependencies) {

    this(parent, new HashMap<>(), dependencies);
  }

  private VirtualClassLoader (ClassLoader parent, HashMap<String, ByteArrayOutputStream> streamMap, String... dependencies) {

    super(parent, -1, new ClasspathClassGate(dependencies), new VirtualClassGate(streamMap));

    this.streamMap = streamMap;
  }

  public ByteArrayOutputStream getOutputStream (String className) {

    ByteArrayOutputStream byteArrayOutputStream;

    streamMap.put(className, byteArrayOutputStream = new ByteArrayOutputStream());

    return byteArrayOutputStream;
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
        return loadClass(classNameIterator.next(), true);
      } catch (ClassNotFoundException classNotFoundException) {
        throw new RuntimeException(classNotFoundException);
      }
    }
  }
}
