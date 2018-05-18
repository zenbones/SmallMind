package org.smallmind.web.json.dto.maven;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.HashMap;
import org.smallmind.nutsnbolts.lang.ClassGate;
import org.smallmind.nutsnbolts.lang.ClassStreamTicket;

public class VirtualClassGate implements ClassGate {

  private final HashMap<String, ByteArrayOutputStream> streamMap;

  public VirtualClassGate (HashMap<String, ByteArrayOutputStream> streamMap) {

    this.streamMap = streamMap;
  }

  @Override
  public long getLastModDate (String path) {

    return ClassGate.STATIC_CLASS;
  }

  @Override
  public CodeSource getCodeSource () {

    return null;
  }

  @Override
  public ClassStreamTicket getTicket (String name) {

    ByteArrayOutputStream byteArrayOutputStream;

    if ((byteArrayOutputStream = streamMap.get(name)) != null) {

      return new ClassStreamTicket(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), ClassGate.STATIC_CLASS);
    }

    return null;
  }

  @Override
  public URL getResource (String path) {

    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getResourceAsStream (String path) {

    throw new UnsupportedOperationException();
  }
}
