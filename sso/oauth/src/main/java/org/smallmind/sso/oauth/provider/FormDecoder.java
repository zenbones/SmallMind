package org.smallmind.sso.oauth.provider;

import java.io.UnsupportedEncodingException;
import org.smallmind.nutsnbolts.http.URLCodec;

public class FormDecoder {

  public static ParameterMap decode (String parameterBlock)
    throws UnsupportedEncodingException {

    ParameterMap parameterMap = new ParameterMap();

    if ((parameterBlock != null) && (!parameterBlock.isEmpty())) {

      String[] parameters = parameterBlock.split("&");

      for (String parameter : parameters) {

        int equalsPos;

        if ((equalsPos = parameter.indexOf('=')) < 0) {
          parameterMap.put(URLCodec.urlDecode(parameter), "");
        } else {
          parameterMap.put(URLCodec.urlDecode(parameter.substring(0, equalsPos)), URLCodec.urlDecode(parameter.substring(equalsPos + 1)));
        }
      }
    }

    return parameterMap;
  }
}
