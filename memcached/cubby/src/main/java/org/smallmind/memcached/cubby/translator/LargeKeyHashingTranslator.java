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
package org.smallmind.memcached.cubby.translator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import org.smallmind.memcached.cubby.CubbyOperationException;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

/**
 * Decorator {@link KeyTranslator} that transparently hashes keys whose encoded form exceeds the
 * memcached 250-character limit.
 *
 * <p>The delegate translator is invoked first. If the resulting key is within the 250-character
 * limit it is returned as-is. When the limit is exceeded the <em>original</em> key (not the
 * encoded one) is hashed using SHA3-512 and the raw digest is Base64-encoded to produce a
 * compact, safe key. A SHA3-512 digest is 64 bytes, which Base64-encodes to 88 characters&mdash;
 * well within the 250-character limit.</p>
 *
 * <p>Typical usage is to wrap a {@link DefaultKeyTranslator}:</p>
 * <pre>{@code
 * KeyTranslator translator = new LargeKeyHashingTranslator(new DefaultKeyTranslator());
 * }</pre>
 */
public class LargeKeyHashingTranslator implements KeyTranslator {

  private final KeyTranslator keyTranslator;

  /**
   * Constructs a large-key hashing translator that wraps the given delegate.
   *
   * @param keyTranslator the delegate translator applied before the length check
   */
  public LargeKeyHashingTranslator (KeyTranslator keyTranslator) {

    this.keyTranslator = keyTranslator;
  }

  /**
   * Encodes the key using the delegate translator, falling back to a SHA3-512 hash when the
   * encoded result exceeds 250 characters.
   *
   * <p>When hashing is required the raw UTF-8 bytes of the original {@code key} are hashed
   * (not the delegate-encoded form) and the digest is then Base64-encoded to produce the
   * final memcached key.</p>
   *
   * @param key the original application-level cache key
   * @return a memcached-safe encoded key of at most 250 characters
   * @throws IOException             if encoding fails in the delegate or during Base64 encoding
   * @throws CubbyOperationException if the SHA3-512 algorithm is unavailable on this platform
   */
  @Override
  public String encode (String key)
    throws IOException, CubbyOperationException {

    String translatedKey;

    if ((translatedKey = keyTranslator.encode(key)).length() > 250) {
      try {

        //must start with no more than 187 bytes (which will base 64 encode to 250)
        return Base64Codec.encode(EncryptionUtility.hash(HashAlgorithm.SHA3_512, key.getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        throw new CubbyOperationException(noSuchAlgorithmException);
      }
    } else {

      return translatedKey;
    }
  }
}
