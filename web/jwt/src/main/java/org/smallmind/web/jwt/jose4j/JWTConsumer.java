/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import static org.jose4j.jws.AlgorithmIdentifiers.NONE;

/**
 * A minimal JWT consumer that can process nested JWE/JWS tokens and configure signature/encryption requirements.
 */
public class JWTConsumer {

  private boolean skipSignatureVerification;
  private boolean skipVerificationKeyResolutionOnNone;
  private boolean requireSignature = true;
  private boolean requireEncryption;
  private boolean requireIntegrity;

  /**
   * Controls whether signature verification is performed.
   *
   * @param skipSignatureVerification {@code true} to bypass signature checks
   * @return this consumer for chaining
   */
  public JWTConsumer setSkipSignatureVerification (boolean skipSignatureVerification) {

    this.skipSignatureVerification = skipSignatureVerification;

    return this;
  }

  /**
   * Controls whether a verification key is applied when the header algorithm is {@code none}.
   *
   * @param skipVerificationKeyResolutionOnNone {@code true} to avoid applying the verification key for {@code none} algorithms
   * @return this consumer for chaining
   */
  public JWTConsumer setSkipVerificationKeyResolutionOnNone (boolean skipVerificationKeyResolutionOnNone) {

    this.skipVerificationKeyResolutionOnNone = skipVerificationKeyResolutionOnNone;

    return this;
  }

  /**
   * Configures whether a signature is required.
   *
   * @param requireSignature {@code true} to demand a signature
   * @return this consumer for chaining
   */
  public JWTConsumer setRequireSignature (boolean requireSignature) {

    this.requireSignature = requireSignature;

    return this;
  }

  /**
   * Configures whether encryption is required.
   *
   * @param requireEncryption {@code true} to demand encryption
   * @return this consumer for chaining
   */
  public JWTConsumer setRequireEncryption (boolean requireEncryption) {

    this.requireEncryption = requireEncryption;

    return this;
  }

  /**
   * Configures whether integrity protection (signature/MAC or symmetric AEAD encryption) is required.
   *
   * @param requireIntegrity {@code true} to demand integrity protection
   * @return this consumer for chaining
   */
  public JWTConsumer setRequireIntegrity (boolean requireIntegrity) {

    this.requireIntegrity = requireIntegrity;

    return this;
  }

  /**
   * Parses and verifies a JWT, automatically handling nested structures and returning the payload as the given type.
   *
   * @param jwt the compact JWT string
   * @param decryptionKey the key to use for signature verification or decryption
   * @param claimsClass the expected payload type
   * @param <T> the claim type
   * @return the deserialized claims object
   * @throws Exception if parsing, verification, or deserialization fails
   */
  public <T> T process (String jwt, Key decryptionKey, Class<T> claimsClass)
    throws Exception {

    T jwtClaims = null;
    LinkedList<JsonWebStructure> joseObjects = new LinkedList<>();
    String workingJwt = jwt;

    while (jwtClaims == null) {

      JsonWebStructure joseObject;

      joseObject = JsonWebStructure.fromCompactSerialization(workingJwt);
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

  /**
   * Validates the collected JOSE structures against configured requirements.
   *
   * @param jwt the original token for error reporting
   * @param verificationKey the key used to verify signatures
   * @param joseObjects the ordered list of JOSE layers (inner-most first)
   * @throws Exception if verification fails or requirements are not met
   */
  public void processContext (String jwt, Key verificationKey, LinkedList<JsonWebStructure> joseObjects)
    throws Exception {

    boolean hasSignature = false;
    boolean hasEncryption = false;
    boolean hasSymmetricEncryption = false;

    ArrayList<JsonWebStructure> originalJoseObjects = new ArrayList<>(joseObjects);

    for (int idx = originalJoseObjects.size() - 1; idx >= 0; idx--) {

      JsonWebStructure currentJoseObject = originalJoseObjects.get(idx);

      if (currentJoseObject instanceof JsonWebSignature) {

        JsonWebSignature jws = (JsonWebSignature)currentJoseObject;
        boolean isNoneAlg = NONE.equals(jws.getAlgorithmHeaderValue());

        if (!skipSignatureVerification) {

          if (!isNoneAlg || !skipVerificationKeyResolutionOnNone) {

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

  /**
   * Determines whether the JOSE object wraps another JWT.
   *
   * @param joseObject the JOSE structure to test
   * @return {@code true} if the content type denotes a nested JWT
   */
  private boolean isNestedJwt (JsonWebStructure joseObject) {

    String cty = joseObject.getContentTypeHeaderValue();

    return cty != null && (cty.equalsIgnoreCase("jwt") || cty.equalsIgnoreCase("application/jwt"));
  }
}
