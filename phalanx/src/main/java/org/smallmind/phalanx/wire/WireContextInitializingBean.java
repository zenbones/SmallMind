package org.smallmind.phalanx.wire;

import org.springframework.beans.factory.InitializingBean;

public class WireContextInitializingBean implements InitializingBean {

  private Class<? extends WireContext> contextClass;
  private String handle;

  public void setHandle (String handle) {

    this.handle = handle;
  }

  public void setContextClass (Class<? extends WireContext> contextClass) {

    this.contextClass = contextClass;
  }

  @Override
  public void afterPropertiesSet () throws Exception {

    WireContextManager.register(handle, contextClass);
  }
}

