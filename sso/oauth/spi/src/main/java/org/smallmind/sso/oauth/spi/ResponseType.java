package org.smallmind.sso.oauth.spi;

public enum ResponseType {

  CODE("code"),
  TOKEN("token");

  private final String code;

  ResponseType (String code) {

    this.code = code;
  }

  public static ResponseType fromCode (String code) {

    for (ResponseType responseType : ResponseType.values()) {
      if (responseType.getCode().equals(code)) {

        return responseType;
      }
    }

    return null;
  }

  public String getCode () {

    return code;
  }
}
