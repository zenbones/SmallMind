package org.smallmind.nutsnbolts.security;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class EncryptedValue implements FactoryBean<String>, InitializingBean {

  private Decryptor decryptor;
  private String value;
  private String decryptedValue;

  @Override
  public Class<?> getObjectType () {

    return String.class;
  }

  public void setDecryptor (Decryptor decryptor) {

    this.decryptor = decryptor;
  }

  public void setValue (String value) {

    this.value = value;
  }

  @Override
  public void afterPropertiesSet ()
    throws Exception {

    decryptedValue = new String(decryptor.decrypt(value.getBytes()));
  }

  @Override
  public String getObject ()
    throws Exception {

    return decryptedValue;
  }
}
