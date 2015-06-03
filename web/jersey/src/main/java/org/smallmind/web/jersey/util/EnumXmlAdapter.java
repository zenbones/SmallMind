package org.smallmind.web.jersey.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;
import org.smallmind.nutsnbolts.util.EnumUtility;

public class EnumXmlAdapter<E extends Enum<E>> extends XmlAdapter<String, E> {

  private Class<E> enumClass;

  public EnumXmlAdapter () {

    enumClass = (Class<E>)GenericUtility.getTypeArguments(EnumXmlAdapter.class, this.getClass()).get(0);
  }

  @Override
  public E unmarshal (String value) {

    return (value == null) ? null : Enum.valueOf(enumClass, EnumUtility.toEnumName(value));
  }

  @Override
  public String marshal (E enumeration) {

    return (enumeration == null) ? null : enumeration.toString();
  }
}