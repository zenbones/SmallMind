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
package org.smallmind.nutsnbolts.security.key;

import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import org.bouncycastle.jcajce.spec.OpenSSHPrivateKeySpec;
import org.bouncycastle.jcajce.spec.OpenSSHPublicKeySpec;
import org.bouncycastle.util.io.pem.PemReader;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.SecurityAlgorithm;
import org.smallmind.nutsnbolts.security.SecurityProvider;

public enum AsymmetricKeyType {

  PUBLIC {
    @Override
    public Key generateKey (SecurityAlgorithm algorithm, SecurityProvider provider, String raw)
      throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {

      return AsymmetricKeyType.keyFactoryInstance(algorithm, provider).generatePublic(new OpenSSHPublicKeySpec(Base64Codec.decode(raw)));
    }
  },
  PRIVATE {
    @Override
    public Key generateKey (SecurityAlgorithm algorithm, SecurityProvider provider, String raw)
      throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {

      return AsymmetricKeyType.keyFactoryInstance(algorithm, provider).generatePrivate(new OpenSSHPrivateKeySpec(new PemReader(new StringReader(raw)).readPemObject().getContent()));
    }
  };

  private static KeyFactory keyFactoryInstance (SecurityAlgorithm algorithm, SecurityProvider provider)
    throws NoSuchAlgorithmException, NoSuchProviderException {

    return SecurityProvider.DEFAULT.equals(provider) ? KeyFactory.getInstance(algorithm.getAlgorithmName()) : KeyFactory.getInstance(algorithm.getAlgorithmName(), provider.getProviderName());
  }

  public abstract Key generateKey (SecurityAlgorithm algorithm, SecurityProvider provider, String raw)
    throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException;
}
