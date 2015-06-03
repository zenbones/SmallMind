package org.smallmind.web.jersey.util;

import java.io.IOException;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;

public enum Encoding {

  HEX {
    @Override
    public String encode (byte[] bytes) throws Exception {

      return EncryptionUtility.hexEncode(bytes);
    }

    @Override
    public byte[] decode (String encoded) {

      return EncryptionUtility.hexDecode(encoded);
    }
  },
  BASE_64 {
    @Override
    public String encode (byte[] bytes)
      throws IOException {

      return Base64Codec.encode(bytes);
    }

    @Override
    public byte[] decode (String encoded)
      throws IOException {

      return Base64Codec.decode(encoded);
    }
  };

  public abstract String encode (byte[] bytes)
    throws Exception;

  public abstract byte[] decode (String encoded)
    throws Exception;
}
