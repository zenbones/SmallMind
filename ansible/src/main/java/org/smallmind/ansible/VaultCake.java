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
package org.smallmind.ansible;

/**
 * Immutable value object holding the three binary components that make up a serialized Ansible vault
 * payload: the PBKDF2 salt, the HMAC-SHA256 authentication tag, and the AES-CTR ciphertext.
 *
 * <p>Instances are produced by {@link VaultTumbler#encrypt(byte[])} and consumed by
 * {@link VaultCodec} during serialization to the {@code $ANSIBLE_VAULT} text format.
 */
public class VaultCake {

  private final byte[] salt;
  private final byte[] hmac;
  private final byte[] encrypted;

  /**
   * Assembles a vault payload from its constituent parts.
   *
   * @param salt      the 32-byte random salt passed to PBKDF2WithHmacSHA256 during key derivation
   * @param hmac      the 32-byte HMAC-SHA256 tag computed over {@code encrypted}; used during
   *                  decryption to verify both integrity and password correctness
   * @param encrypted the AES-CTR ciphertext bytes produced from the plaintext
   */
  public VaultCake (byte[] salt, byte[] hmac, byte[] encrypted) {

    this.salt = salt;
    this.hmac = hmac;
    this.encrypted = encrypted;
  }

  /**
   * Returns the salt used during PBKDF2 key derivation.
   *
   * @return 32-byte salt; never {@code null}
   */
  public byte[] getSalt () {

    return salt;
  }

  /**
   * Returns the HMAC-SHA256 authentication tag computed over the ciphertext.
   *
   * <p>During decryption this value is compared against a freshly computed HMAC to confirm
   * that the supplied password is correct and that the ciphertext has not been tampered with.
   *
   * @return 32-byte HMAC; never {@code null}
   */
  public byte[] getHmac () {

    return hmac;
  }

  /**
   * Returns the AES-CTR encrypted payload.
   *
   * @return ciphertext bytes; never {@code null}
   */
  public byte[] getEncrypted () {

    return encrypted;
  }
}
