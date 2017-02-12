package org.smallmind.web.jersey.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;

public class FormattedJsonProcessingException extends JsonProcessingException {

  public FormattedJsonProcessingException (String message, Object... args) {

    super(String.format(message, args));
  }
}
