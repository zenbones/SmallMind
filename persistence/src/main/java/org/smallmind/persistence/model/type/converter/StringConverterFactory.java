package org.smallmind.persistence.model.type.converter;

import org.smallmind.persistence.model.bean.BeanAccessException;

public interface StringConverterFactory {

   public StringConverter getStringConverter (Class parameterClass)
      throws BeanAccessException;
}
