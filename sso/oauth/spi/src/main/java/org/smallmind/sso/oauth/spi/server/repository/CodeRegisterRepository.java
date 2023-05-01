package org.smallmind.sso.oauth.spi.server.repository;

public interface CodeRegisterRepository {

  void put (String code, int maxAgeSeconds, CodeRegister codeRegister);

  CodeRegister get (String code);
}
