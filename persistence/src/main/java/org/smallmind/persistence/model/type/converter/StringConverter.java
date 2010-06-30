package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.bean.BeanInvocationException;
import org.smallmind.persistence.model.type.PrimitiveType;

public interface StringConverter<T> {

   public abstract PrimitiveType getPrimitiveType ();

   public abstract T convert (String value)
      throws BeanInvocationException;
}
