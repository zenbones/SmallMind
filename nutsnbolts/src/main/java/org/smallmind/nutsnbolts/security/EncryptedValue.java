package org.smallmind.nutsnbolts.security;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class EncryptedValue implements FactoryBean<String>, InitializingBean {

  public Key key;
  public String value;
  public String decryptedValue;

  @Override
  public Class<?> getObjectType () {

    return String.class;
  }

  public void setKey (Key key) {

    this.key = key;
  }

  public void setValue (String value) {

    this.value = value;
  }

  @Override
  public void afterPropertiesSet ()
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    decryptedValue = new String(EncryptionUtility.decrypt(key, value.getBytes()));
  }

  @Override
  public String getObject ()
    throws Exception {

    return decryptedValue;
  }
}
