package org.smallmind.sso.oauth.spi.server.repository;

import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.SelfDestructive;

public class CodeRegisterSelfDestructive implements SelfDestructive {

  private final CodeRegister codeRegister;

  public CodeRegisterSelfDestructive (CodeRegister codeRegister) {

    this.codeRegister = codeRegister;
  }

  public CodeRegister getCodeRegister () {

    return codeRegister;
  }

  @Override
  public void destroy (Stint timeoutStint) {


  }
}
