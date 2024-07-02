package org.smallmind.nutsnbolts.security;

import java.security.Provider;
import java.security.Security;
import org.springframework.beans.factory.InitializingBean;

public class SecurityProviderInitializingBean implements InitializingBean {

  private Provider[] providers;

  public void setProviders (Provider[] providers) {

    this.providers = providers;
  }

  @Override
  public void afterPropertiesSet ()
    throws Exception {

    for (Provider provider : providers) {
      Security.addProvider(provider);
    }
  }
}
