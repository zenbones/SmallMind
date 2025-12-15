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
package org.smallmind.nutsnbolts.security;

import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

/**
 * Indicates whether a key is public or private and can construct keys from text using a given spec.
 */
public enum AsymmetricKeyType {

  PUBLIC {
    /**
     * Generates a public key using the provided spec and algorithm.
     *
     * @param algorithm the asymmetric algorithm
     * @param spec the encoding to use
     * @param provider optional security provider (or {@link SecurityProvider#DEFAULT})
     * @param raw raw key text
     * @return the generated public key
     * @throws IOException if the raw key cannot be parsed
     * @throws NoSuchProviderException if the provider is unknown
     * @throws NoSuchAlgorithmException if the algorithm is unavailable
     * @throws InvalidKeySpecException if the spec is invalid
     * @throws InappropriateKeySpecException if the spec is not suitable for a public key
     */
    @Override
    public Key constructKey (AsymmetricAlgorithm algorithm, AsymmetricKeySpec spec, SecurityProvider provider, String raw)
      throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InappropriateKeySpecException {

      return AsymmetricKeyType.keyFactoryInstance(algorithm, provider).generatePublic(spec.constructKeySpec(this, raw));
    }
  },
  PRIVATE {
    /**
     * Generates a private key using the provided spec and algorithm.
     *
     * @param algorithm the asymmetric algorithm
     * @param spec the encoding to use
     * @param provider optional security provider (or {@link SecurityProvider#DEFAULT})
     * @param raw raw key text
     * @return the generated private key
     * @throws IOException if the raw key cannot be parsed
     * @throws NoSuchProviderException if the provider is unknown
     * @throws NoSuchAlgorithmException if the algorithm is unavailable
     * @throws InvalidKeySpecException if the spec is invalid
     * @throws InappropriateKeySpecException if the spec is not suitable for a private key
     */
    @Override
    public Key constructKey (AsymmetricAlgorithm algorithm, AsymmetricKeySpec spec, SecurityProvider provider, String raw)
      throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InappropriateKeySpecException {

      return AsymmetricKeyType.keyFactoryInstance(algorithm, provider).generatePrivate(spec.constructKeySpec(this, raw));
    }
  };

  private static KeyFactory keyFactoryInstance (SecurityAlgorithm algorithm, SecurityProvider provider)
    throws NoSuchAlgorithmException, NoSuchProviderException {

    return SecurityProvider.DEFAULT.equals(provider) ? KeyFactory.getInstance(algorithm.getAlgorithmName()) : KeyFactory.getInstance(algorithm.getAlgorithmName(), provider.getProviderName());
  }

  /**
   * Constructs a key of this type using the given algorithm and encoding.
   *
   * @param algorithm the algorithm corresponding to the key
   * @param spec      the key encoding format
   * @param provider  optional security provider
   * @param raw       raw key text in the specified format
   * @return the generated {@link Key}
   * @throws IOException                   if parsing fails
   * @throws NoSuchProviderException       if the provider is unknown
   * @throws NoSuchAlgorithmException      if the algorithm is unavailable
   * @throws InvalidKeySpecException       if the spec is invalid
   * @throws InappropriateKeySpecException if the spec does not match this key type
   */
  public abstract Key constructKey (AsymmetricAlgorithm algorithm, AsymmetricKeySpec spec, SecurityProvider provider, String raw)
    throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InappropriateKeySpecException;
}
