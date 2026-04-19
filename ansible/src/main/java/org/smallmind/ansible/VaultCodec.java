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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.smallmind.nutsnbolts.security.HexCodec;

/**
 * Static codec for reading and writing the Ansible vault text format.
 *
 * <p>An Ansible vault file is a text document with the following structure:
 * <pre>
 * $ANSIBLE_VAULT;1.1;AES256\n          (format 1.1, no vault id)
 * $ANSIBLE_VAULT;1.2;AES256;myid\n     (format 1.2, with vault id)
 * &lt;body&gt;
 * </pre>
 * The body encodes three binary fields — salt, HMAC, and ciphertext — each double-hex-encoded
 * (hex of the hex of the raw bytes) and separated by the two-character token {@code 0a}.  Long
 * lines in the body are broken at 80 characters with {@code \n} to match Ansible's own output.
 *
 * <p>All methods are static; this class is not intended to be instantiated.
 *
 * @see VaultTumbler
 * @see VaultCake
 */
public class VaultCodec {

  /**
   * Encrypts the content of {@code inputStream} into Ansible vault format 1.1 (no vault id).
   *
   * <p>Delegates to {@link #encrypt(InputStream, String, String)} with a {@code null} id.
   *
   * @param inputStream plaintext source; read to EOF but not closed by this method
   * @param password    vault password used for PBKDF2 key derivation in {@link VaultTumbler}
   * @return the complete vault text including the {@code $ANSIBLE_VAULT} header and body
   * @throws IOException         if reading from {@code inputStream} fails
   * @throws VaultCodecException if the JCA provider rejects the cryptographic operations
   */
  public static String encrypt (InputStream inputStream, String password)
    throws IOException, VaultCodecException {

    return encrypt(inputStream, password, null);
  }

  /**
   * Encrypts the content of {@code inputStream} into Ansible vault format 1.1 or 1.2.
   *
   * <p>When {@code id} is non-{@code null} the header is written in format 1.2
   * ({@code $ANSIBLE_VAULT;1.2;AES256;<id>}), which allows {@code ansible-vault} to match
   * vault secrets by label.  When {@code id} is {@code null} format 1.1 is used.
   *
   * <p>The body is constructed by double-hex-encoding each binary field (salt, HMAC, ciphertext),
   * joining them with the literal token {@code 0a} (the hex representation of a newline), and
   * wrapping the resulting string at 80-character intervals.
   *
   * @param inputStream plaintext source; read to EOF but not closed by this method
   * @param password    vault password used for PBKDF2 key derivation
   * @param id          vault label embedded in a format-1.2 header; pass {@code null} to use
   *                    format 1.1 without a label
   * @return the complete vault text including the {@code $ANSIBLE_VAULT} header and body
   * @throws IOException         if reading from {@code inputStream} fails
   * @throws VaultCodecException if the JCA provider rejects the cryptographic operations
   */
  public static String encrypt (InputStream inputStream, String password, String id)
    throws IOException, VaultCodecException {

    StringBuilder encryptedBuilder = new StringBuilder();
    VaultCake vaultCake;

    try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

      int singleByte;

      while ((singleByte = inputStream.read()) >= 0) {
        byteOutputStream.write(singleByte);
      }

      vaultCake = new VaultTumbler(password).encrypt(byteOutputStream.toByteArray());
    }

    encryptedBuilder.append(HexCodec.hexEncode(HexCodec.hexEncode(vaultCake.getSalt()).getBytes(StandardCharsets.UTF_8)));
    encryptedBuilder.append("0a");
    encryptedBuilder.append(HexCodec.hexEncode(HexCodec.hexEncode(vaultCake.getHmac()).getBytes(StandardCharsets.UTF_8)));
    encryptedBuilder.append("0a");
    encryptedBuilder.append(HexCodec.hexEncode(HexCodec.hexEncode(vaultCake.getEncrypted()).getBytes(StandardCharsets.UTF_8)));

    for (int index = (encryptedBuilder.length() / 80) * 80; index > 0; index -= 80) {
      encryptedBuilder.insert(index, '\n');
    }

    if (id != null) {
      encryptedBuilder.insert(0, '\n').insert(0, id).insert(0, "1.2;AES256;");
    } else {
      encryptedBuilder.insert(0, "1.1;AES256\n");
    }
    encryptedBuilder.insert(0, "$ANSIBLE_VAULT;");

    return encryptedBuilder.toString();
  }

  /**
   * Decrypts an Ansible vault stream and returns the original plaintext bytes.
   *
   * <p>The first line of the stream is parsed as the vault header.  Supported formats are
   * {@code $ANSIBLE_VAULT;1.1;AES256} and {@code $ANSIBLE_VAULT;1.2;AES256;<id>}.  The body
   * is then read in three fixed-size passes (salt, HMAC) followed by one open-ended pass
   * (ciphertext), each reversing the double-hex encoding applied during encryption.
   *
   * @param inputStream vault text source; read to EOF but not closed by this method
   * @param password    vault password used to re-derive the AES and HMAC keys via PBKDF2
   * @return decrypted plaintext bytes
   * @throws IOException            if reading from {@code inputStream} fails
   * @throws VaultPasswordException if the password is incorrect (HMAC mismatch)
   * @throws VaultCodecException    if the header format is unrecognized, the declared cipher is
   *                                not {@code AES256}, the body is truncated, or a JCA error occurs
   */
  public static byte[] decrypt (InputStream inputStream, String password)
    throws IOException, VaultCodecException {

    String header = readLine(inputStream);
    String[] headerParts = header.split(";", -1);

    if ((headerParts.length >= 3) && "$ANSIBLE_VAULT".equals(headerParts[0]) && (("1.1".equals(headerParts[1]) && (headerParts.length == 3)) || ("1.2".equals(headerParts[1]) && (headerParts.length == 4)))) {
      if (!"AES256".equals(headerParts[2])) {
        throw new VaultCodecException("Unknown cypher(%s)", headerParts[2]);
      } else {

        return new VaultTumbler(password, readBytes(inputStream, 32)).decrypt(readBytes(inputStream, 32), readBytes(inputStream));
      }
    } else {
      throw new VaultCodecException("Unknown header format(%s)", header);
    }
  }

  /**
   * Asserts that the next two bytes in the stream are the literal characters {@code 0} and {@code a},
   * which represent the double-hex-encoded newline separator between body fields.
   *
   * @param inputStream stream positioned immediately after a body field
   * @throws IOException         if reading from the stream fails
   * @throws VaultCodecException if the two bytes do not form the expected {@code 0a} token
   */
  private static void skip0A (InputStream inputStream)
    throws IOException, VaultCodecException {

    if ((inputStream.read() != '0') || (inputStream.read() != 'a')) {
      throw new VaultCodecException("Expected line terminator(0a)");
    }
  }

  /**
   * Reads exactly {@code length} decoded bytes from the stream and advances past the field separator.
   *
   * <p>Because each raw byte is double-hex-encoded it occupies four characters in the stream.
   * Embedded newlines (line-wrap characters) are silently skipped while reading.  After the
   * expected characters are consumed, {@link #skip0A} is called to consume the {@code 0a} separator.
   *
   * @param inputStream stream positioned at the start of a fixed-size body field
   * @param length      number of decoded bytes expected (e.g. 32 for the 32-byte salt or HMAC)
   * @return decoded byte array of exactly {@code length} bytes
   * @throws IOException         if reading from the stream fails
   * @throws VaultCodecException if fewer than {@code length * 4} non-newline characters are
   *                             available before EOF, or if the {@code 0a} separator is missing
   */
  private static byte[] readBytes (InputStream inputStream, int length)
    throws IOException, VaultCodecException {

    int quadrupleLength = length * 4;
    int bytesRead = 0;
    byte[] buffer = new byte[quadrupleLength];

    int singleByte;

    while ((singleByte = inputStream.read()) >= 0) {
      if (singleByte != '\n') {
        buffer[bytesRead++] = (byte)singleByte;
        if (bytesRead == quadrupleLength) {
          break;
        }
      }
    }

    if (bytesRead < quadrupleLength) {
      throw new VaultCodecException("Unable to read required bytes(%d)", length);
    } else {
      skip0A(inputStream);

      return HexCodec.hexDecode(HexCodec.hexDecode(buffer));
    }
  }

  /**
   * Reads all remaining bytes from the stream (skipping embedded newlines) and decodes them.
   *
   * <p>Used to read the variable-length ciphertext field, which is the last field in the vault
   * body and is therefore not terminated by a {@code 0a} separator.
   *
   * @param inputStream stream positioned at the start of the ciphertext field
   * @return decoded ciphertext bytes
   * @throws IOException if reading from the stream fails
   */
  private static byte[] readBytes (InputStream inputStream)
    throws IOException {

    try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

      int singleByte;

      while ((singleByte = inputStream.read()) >= 0) {
        if (singleByte != '\n') {
          byteOutputStream.write(singleByte);
        }
      }

      return HexCodec.hexDecode(HexCodec.hexDecode(byteOutputStream.toByteArray()));
    }
  }

  /**
   * Reads one line from the stream, returning the content without the terminating {@code \n}.
   *
   * <p>If the stream ends before a newline is encountered the accumulated bytes are returned
   * as-is, making this method suitable for single-line streams as well as multi-line ones.
   *
   * @param inputStream stream positioned at the start of a line
   * @return line content, excluding the newline character; may be empty if the line is blank
   * @throws IOException if reading from the stream fails
   */
  private static String readLine (InputStream inputStream)
    throws IOException {

    try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

      int singleByte;

      while ((singleByte = inputStream.read()) >= 0) {
        if (singleByte == '\n') {

          return byteOutputStream.toString();
        } else {
          byteOutputStream.write(singleByte);
        }
      }

      return byteOutputStream.toString();
    }
  }
}
