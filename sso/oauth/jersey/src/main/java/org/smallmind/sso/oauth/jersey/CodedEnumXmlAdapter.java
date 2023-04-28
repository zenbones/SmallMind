package org.smallmind.sso.oauth.jersey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.smallmind.nutsnbolts.json.EnumXmlAdapter;
import org.smallmind.nutsnbolts.reflection.type.GenericUtility;

public abstract class CodedEnumXmlAdapter<E extends Enum<E>> extends XmlAdapter<String, E> {

  private final Class<E> enumClass;
  private final Method getCodeMethod;
  private final Method fromCodeMethod;

  public CodedEnumXmlAdapter ()
    throws NoSuchMethodException, SecurityException {

    enumClass = (Class<E>)GenericUtility.getTypeArgumentsOfSubclass(EnumXmlAdapter.class, this.getClass()).get(0);
    getCodeMethod = enumClass.getDeclaredMethod("getCode");
    fromCodeMethod = enumClass.getDeclaredMethod("fromCode", String.class);
  }

  @Override
  public E unmarshal (String value) {

    if (value == null) {

      return null;
    } else {
      try {

        return enumClass.cast(fromCodeMethod.invoke(enumClass, value));
      } catch (IllegalAccessException | InvocationTargetException exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  @Override
  public String marshal (E enumeration) {

    if (enumeration == null) {

      return null;
    } else {

      try {

        return (String)getCodeMethod.invoke(enumeration);
      } catch (IllegalAccessException | InvocationTargetException exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}
