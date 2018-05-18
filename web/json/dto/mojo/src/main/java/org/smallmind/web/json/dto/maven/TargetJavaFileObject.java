package org.smallmind.web.json.dto.maven;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

public class TargetJavaFileObject implements JavaFileObject {

  private ByteArrayOutputStream byteArrayOutputStream;

  public TargetJavaFileObject (ByteArrayOutputStream byteArrayOutputStream) {

    this.byteArrayOutputStream = byteArrayOutputStream;
  }

  @Override
  public Kind getKind () {

    return Kind.CLASS;
  }

  @Override
  public boolean isNameCompatible (String simpleName, Kind kind) {

    return Kind.CLASS.equals(kind);
  }

  @Override
  public NestingKind getNestingKind () {

    return null;
  }

  @Override
  public Modifier getAccessLevel () {

    return null;
  }

  @Override
  public URI toUri () {

    throw new UnsupportedOperationException();
  }

  @Override
  public String getName () {

    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream openInputStream () {

    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStream () {

    return byteArrayOutputStream;
  }

  @Override
  public Reader openReader (boolean ignoreEncodingErrors) {

    throw new UnsupportedOperationException();
  }

  @Override
  public CharSequence getCharContent (boolean ignoreEncodingErrors) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Writer openWriter () {

    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastModified () {

    return 0;
  }

  @Override
  public boolean delete () {

    return false;
  }
}
