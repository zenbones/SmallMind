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
package org.smallmind.nutsnbolts.security;

import java.io.IOException;
import java.io.StringReader;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.jcajce.spec.OpenSSHPublicKeySpec;
import org.bouncycastle.util.io.pem.PemReader;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.key.AsymmetricKeyType;

public enum AsymmetricKeySpec {

  OPENSSH {
    @Override
    public KeySpec generateKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {

        int lastSpacePos;

        if (raw.startsWith("ssh-")) {

          int firstSpacePos;

          if ((firstSpacePos = raw.indexOf(' ')) <= 0) {
            throw new IOException("The raw key requires a space separator after the ssh prologue");
          } else {
            raw = raw.substring(firstSpacePos + 1);
          }
        }

        // remove the email address cooment
        if ((lastSpacePos = raw.lastIndexOf(' ')) >= 0) {
          raw = raw.substring(0, lastSpacePos);
        }

        return new OpenSSHPublicKeySpec(Base64Codec.decode(raw));
      } else {
        throw new InappropriateKeySpecException(type.name());
      }
    }
  },
  PKCS8 {
    @Override
    public KeySpec generateKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {
        throw new InappropriateKeySpecException(type.name());
      } else {
        raw = raw.trim();

        if (!raw.startsWith("-----")) {
          raw = "-----BEGIN PRIVATE KEY-----\n" + raw;
        }
        if (!raw.endsWith("-----")) {
          raw = raw + "\n-----END PRIVATE KEY-----";
        }

        return new PKCS8EncodedKeySpec(new PemReader(new StringReader(raw)).readPemObject().getContent());
      }
    }
  },
  X509 {
    @Override
    public KeySpec generateKeySpec (AsymmetricKeyType type, String raw)
      throws IOException, InappropriateKeySpecException {

      if (AsymmetricKeyType.PUBLIC.equals(type)) {
        raw = raw.trim();

        if (raw.startsWith("-----")) {

          int lineReturnPos;

          if ((lineReturnPos = raw.indexOf('\n')) < 0) {
            throw new IOException("The raw key requires a line separator after the ssh prologue");
          } else {
            raw = raw.substring(lineReturnPos + 1);
          }
        }
        if (raw.endsWith("-----")) {

          int lastLineReturnPos;

          if ((lastLineReturnPos = raw.lastIndexOf('\n')) < 0) {
            throw new IOException("The raw key requires a line separator before the ssh epilogue");
          } else {
            raw = raw.substring(0, lastLineReturnPos);
          }
        }

        return new X509EncodedKeySpec(Base64Codec.decode(raw.replaceAll("\\s", "")));
      } else {
        throw new InappropriateKeySpecException(type.name());
      }
    }
  };

  public abstract KeySpec generateKeySpec (AsymmetricKeyType type, String raw)
    throws IOException, InappropriateKeySpecException;
}
