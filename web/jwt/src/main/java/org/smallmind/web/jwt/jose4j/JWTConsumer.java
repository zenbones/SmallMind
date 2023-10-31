/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.jwt.jose4j;

import java.security.Key;
import java.util.ArrayList;
import java.util.LinkedList;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.KeyPersuasion;
import org.smallmind.web.json.scaffold.util.JsonCodec;

public class JWTConsumer {

  private boolean skipSignatureVerification;
  private boolean requireSignature = true;
  private boolean requireEncryption;
  private boolean requireIntegrity;

  public JWTConsumer setSkipSignatureVerification (boolean skipSignatureVerification) {

    this.skipSignatureVerification = skipSignatureVerification;

    return this;
  }

  public JWTConsumer setRequireSignature (boolean requireSignature) {

    this.requireSignature = requireSignature;

    return this;
  }

  public JWTConsumer setRequireEncryption (boolean requireEncryption) {

    this.requireEncryption = requireEncryption;

    return this;
  }

  public JWTConsumer setRequireIntegrity (boolean requireIntegrity) {

    this.requireIntegrity = requireIntegrity;

    return this;
  }

  public <T> T process (String jwt, Key decryptionKey, Class<T> claimsClass)
    throws Exception {

    T jwtClaims = null;
    LinkedList<JsonWebStructure> joseObjects = new LinkedList<>();
    String workingJwt = jwt;

    while (jwtClaims == null) {

      JsonWebStructure joseObject = JsonWebStructure.fromCompactSerialization(workingJwt);
      String payload;

      if (joseObject instanceof JsonWebSignature) {

        JsonWebSignature jws = (JsonWebSignature)joseObject;

        payload = jws.getUnverifiedPayload();
      } else {

        JsonWebEncryption jwe = (JsonWebEncryption)joseObject;

        jwe.setKey(decryptionKey);
        payload = jwe.getPayload();
      }

      if (this.isNestedJwt(joseObject)) {
        workingJwt = payload;
      } else {
        jwtClaims = JsonCodec.read(payload, claimsClass);
      }

      joseObjects.addFirst(joseObject);
    }

    processContext(jwt, decryptionKey, joseObjects);

    return jwtClaims;
  }

  private void processContext (String jwt, Key verificationKey, LinkedList<JsonWebStructure> joseObjects)
    throws Exception {

    ArrayList<JsonWebStructure> originalJoseObjects = new ArrayList<>(joseObjects);
    boolean hasSignature = false;
    boolean hasEncryption = false;
    boolean hasSymmetricEncryption = false;

    for (int idx = originalJoseObjects.size() - 1; idx >= 0; --idx) {

      JsonWebStructure currentJoseObject = originalJoseObjects.get(idx);

      if (currentJoseObject instanceof JsonWebSignature) {
        JsonWebSignature jws = (JsonWebSignature)currentJoseObject;
        boolean isNoneAlg = "none".equals(jws.getAlgorithmHeaderValue());
        if (!this.skipSignatureVerification) {
          if (!isNoneAlg) {
            jws.setKey(verificationKey);
          }

          if (!jws.verifySignature()) {
            throw new InvalidJWTSignatureException(jws.getAlgorithmHeaderValue());
          }
        }

        if (!isNoneAlg) {
          hasSignature = true;
        }
      } else {
        JsonWebEncryption jwe = (JsonWebEncryption)currentJoseObject;

        hasEncryption = true;
        hasSymmetricEncryption = jwe.getKeyManagementModeAlgorithm().getKeyPersuasion() == KeyPersuasion.SYMMETRIC;
      }
    }

    if (this.requireSignature && !hasSignature) {
      throw new InvalidJWTException("The JWT has no signature but the JWT Consumer is configured to require one: " + jwt);
    } else if (this.requireEncryption && !hasEncryption) {
      throw new InvalidJWTException("The JWT has no encryption but the JWT Consumer is configured to require it: " + jwt);
    } else if (this.requireIntegrity && !hasSignature && !hasSymmetricEncryption) {
      throw new InvalidJWTException("The JWT has no integrity protection (signature/MAC or symmetric AEAD encryption) but the JWT Consumer is configured to require it: " + jwt);
    }
  }

  private boolean isNestedJwt (JsonWebStructure joseObject) {

    String cty = joseObject.getContentTypeHeaderValue();

    return cty != null && (cty.equalsIgnoreCase("jwt") || cty.equalsIgnoreCase("application/jwt"));
  }
}
