package org.smallmind.sso.oauth.provider;

public class UserAndPassword {

  private final String user;
  private final String password;

  public UserAndPassword (String user, String password) {

    this.user = user;
    this.password = password;
  }

  public String getUser () {

    return user;
  }

  public String getPassword () {

    return password;
  }
}
