package org.smallmind.sso.oauth.spi.server;

public class AuthorizationError {

  private final AuthorizationErrorType type;
  private final String description;

  public AuthorizationError (AuthorizationErrorType type, String description, Object... args) {

    this.type = type;
    this.description = ((args == null) || (args.length == 0)) ? description : String.format(description, args);
  }

  public AuthorizationErrorType getType () {

    return type;
  }

  public String getDescription () {

    return description;
  }
}
