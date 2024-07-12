/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.nutsnbolts.security.key;

import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import org.smallmind.nutsnbolts.security.AsymmetricAlgorithm;
import org.smallmind.nutsnbolts.security.AsymmetricKeySpec;
import org.smallmind.nutsnbolts.security.InappropriateKeySpecException;
import org.smallmind.nutsnbolts.security.SecurityAlgorithm;
import org.smallmind.nutsnbolts.security.SecurityProvider;

public enum AsymmetricKeyType {

  PUBLIC {
    @Override
    public Key generateKey (AsymmetricAlgorithm algorithm, AsymmetricKeySpec spec, SecurityProvider provider, String raw)
      throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InappropriateKeySpecException {

      return AsymmetricKeyType.keyFactoryInstance(algorithm, provider).generatePublic(spec.generateKeySpec(this, raw));
    }
  },
  PRIVATE {
    @Override
    public Key generateKey (AsymmetricAlgorithm algorithm, AsymmetricKeySpec spec, SecurityProvider provider, String raw)
      throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InappropriateKeySpecException {

      return AsymmetricKeyType.keyFactoryInstance(algorithm, provider).generatePrivate(spec.generateKeySpec(this, raw));
    }
  };

  private static KeyFactory keyFactoryInstance (SecurityAlgorithm algorithm, SecurityProvider provider)
    throws NoSuchAlgorithmException, NoSuchProviderException {

    return SecurityProvider.DEFAULT.equals(provider) ? KeyFactory.getInstance(algorithm.getAlgorithmName()) : KeyFactory.getInstance(algorithm.getAlgorithmName(), provider.getProviderName());
  }

  public abstract Key generateKey (AsymmetricAlgorithm algorithm, AsymmetricKeySpec spec, SecurityProvider provider, String raw)
    throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InappropriateKeySpecException;
}
