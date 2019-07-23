/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

public class SHA3 {

  /**
   * Round contants
   */
  private static final long[] RC = {
    0x0000000000000001L, 0x0000000000008082L, 0x800000000000808AL, 0x8000000080008000L,
    0x000000000000808BL, 0x0000000080000001L, 0x8000000080008081L, 0x8000000000008009L,
    0x000000000000008AL, 0x0000000000000088L, 0x0000000080008009L, 0x000000008000000AL,
    0x000000008000808BL, 0x800000000000008BL, 0x8000000000008089L, 0x8000000000008003L,
    0x8000000000008002L, 0x8000000000000080L, 0x000000000000800AL, 0x800000008000000AL,
    0x8000000080008081L, 0x8000000000008080L, 0x0000000080000001L, 0x8000000080008008L};
  /**
   * Keccak-f round temporary
   */
  private long[] B;
  /**
   * Keccak-f round temporary
   */
  private long[] C;
  /**
   * The bitrate
   */
  private int r;
  /**
   * The capacity
   */
  private int c;
  /**
   * The output size
   */
  private int n;
  /**
   * The state size
   */
  private int b = 0;
  /**
   * The word size
   */
  private int w = 0;
  /**
   * The word mask
   */
  private long wmod = 0;
  /**
   * 12 + 2ℓ, the number of rounds
   */
  private int nr = 0;
  /**
   * The current state
   */
  private long[] S = null;
  /**
   * Left over water to fill the sponge with at next update
   */
  private byte[] M = null;
  /**
   * Pointer for {@link #M}
   */
  private int mptr = 0;

  /**
   * Initialise Keccak sponge
   *
   * @param r The bitrate
   * @param c The capacity
   */
  public SHA3 (int r, int c, int n) {

    this.r = r;
    this.c = c;
    this.n = n;

    reset();
  }

  public void reset () {

    int l;

    b = r + c;
    w = b / 25;
    l = lb(w);
    nr = 12 + (l << 1);
    wmod = w == 64 ? -1L : (1L << w) - 1L;
    S = new long[25];
    M = new byte[(r * b) >> 2];
    mptr = 0;

    B = new long[25];
    C = new long[5];
  }

  /**
   * Rotate a word
   *
   * @param x The value to rotate
   * @param n Rotation steps, may not be 0
   * @return The value rotated
   */
  private long rotate (long x, int n) {

    long m;
    return ((x >>> (w - (m = n % w))) + (x << m)) & wmod;
  }

  /**
   * Rotate a 64-bit word
   *
   * @param x The value to rotate
   * @param n Rotation steps, may not be 0
   * @return The value rotated
   */
  private long rotate64 (long x, int n) {

    return (x >>> (64 - n)) + (x << n);
  }

  /**
   * Binary logarithm
   *
   * @param x The value of which to calculate the binary logarithm
   * @return The binary logarithm
   */
  private int lb (int x) {

    int rc = 0;
    if ((x & 0xFF00) != 0) {
      rc += 8;
      x >>= 8;
    }
    if ((x & 0x00F0) != 0) {
      rc += 4;
      x >>= 4;
    }
    if ((x & 0x000C) != 0) {
      rc += 2;
      x >>= 2;
    }
    if ((x & 0x0002) != 0) rc += 1;
    return rc;
  }

  /**
   * Perform one round of computation
   *
   * @param A  The current state
   * @param rc Round constant
   */
  private void keccakFRound (long[] A, long rc) {
    /* ? step (step 1 of 3) */
    for (int i = 0, j = 0; i < 5; i++, j += 5)
      C[i] = (A[j] ^ A[j + 1]) ^ (A[j + 2] ^ A[j + 3]) ^ A[j + 4];

    long da, db, dc, dd, de;

    if (w == 64) {
      /* ? and ? steps, with last two part of ? */
      B[0] = A[0] ^ (da = C[4] ^ rotate64(C[1], 1));
      B[1] = rotate64(A[15] ^ (dd = C[2] ^ rotate64(C[4], 1)), 28);
      B[2] = rotate64(A[5] ^ (db = C[0] ^ rotate64(C[2], 1)), 1);
      B[3] = rotate64(A[20] ^ (de = C[3] ^ rotate64(C[0], 1)), 27);
      B[4] = rotate64(A[10] ^ (dc = C[1] ^ rotate64(C[3], 1)), 62);

      B[5] = rotate64(A[6] ^ db, 44);
      B[6] = rotate64(A[21] ^ de, 20);
      B[7] = rotate64(A[11] ^ dc, 6);
      B[8] = rotate64(A[1] ^ da, 36);
      B[9] = rotate64(A[16] ^ dd, 55);

      B[10] = rotate64(A[12] ^ dc, 43);
      B[11] = rotate64(A[2] ^ da, 3);
      B[12] = rotate64(A[17] ^ dd, 25);
      B[13] = rotate64(A[7] ^ db, 10);
      B[14] = rotate64(A[22] ^ de, 39);

      B[15] = rotate64(A[18] ^ dd, 21);
      B[16] = rotate64(A[8] ^ db, 45);
      B[17] = rotate64(A[23] ^ de, 8);
      B[18] = rotate64(A[13] ^ dc, 15);
      B[19] = rotate64(A[3] ^ da, 41);

      B[20] = rotate64(A[24] ^ de, 14);
      B[21] = rotate64(A[14] ^ dc, 61);
      B[22] = rotate64(A[4] ^ da, 18);
      B[23] = rotate64(A[19] ^ dd, 56);
      B[24] = rotate64(A[9] ^ db, 2);
    } else {
      /* ? and ? steps, with last two part of ? */
      B[0] = A[0] ^ (da = C[4] ^ rotate(C[1], 1));
      B[1] = rotate(A[15] ^ (dd = C[2] ^ rotate(C[4], 1)), 28);
      B[2] = rotate(A[5] ^ (db = C[0] ^ rotate(C[2], 1)), 1);
      B[3] = rotate(A[20] ^ (de = C[3] ^ rotate(C[0], 1)), 27);
      B[4] = rotate(A[10] ^ (dc = C[1] ^ rotate(C[3], 1)), 62);

      B[5] = rotate(A[6] ^ db, 44);
      B[6] = rotate(A[21] ^ de, 20);
      B[7] = rotate(A[11] ^ dc, 6);
      B[8] = rotate(A[1] ^ da, 36);
      B[9] = rotate(A[16] ^ dd, 55);

      B[10] = rotate(A[12] ^ dc, 43);
      B[11] = rotate(A[2] ^ da, 3);
      B[12] = rotate(A[17] ^ dd, 25);
      B[13] = rotate(A[7] ^ db, 10);
      B[14] = rotate(A[22] ^ de, 39);

      B[15] = rotate(A[18] ^ dd, 21);
      B[16] = rotate(A[8] ^ db, 45);
      B[17] = rotate(A[23] ^ de, 8);
      B[18] = rotate(A[13] ^ dc, 15);
      B[19] = rotate(A[3] ^ da, 41);

      B[20] = rotate(A[24] ^ de, 14);
      B[21] = rotate(A[14] ^ dc, 61);
      B[22] = rotate(A[4] ^ da, 18);
      B[23] = rotate(A[19] ^ dd, 56);
      B[24] = rotate(A[9] ^ db, 2);
    }

    /* ? step */
    for (int i = 0; i < 15; i++)
      A[i] = B[i] ^ ((~(B[i + 5])) & B[i + 10]);
    for (int i = 0; i < 5; i++) {
      A[i + 15] = B[i + 15] ^ ((~(B[i + 20])) & B[i]);
      A[i + 20] = B[i + 20] ^ ((~(B[i])) & B[i + 5]);
    }

    /* ? step */
    A[0] ^= rc;
  }

  /**
   * Perform Keccak-f function
   *
   * @param A The current state
   */
  private void keccakF (long[] A) {

    if (nr == 24)
      for (int i = 0; i < 24; i++)
        keccakFRound(A, RC[i]);
    else
      for (int i = 0; i < nr; i++)
        keccakFRound(A, RC[i] & wmod);
  }

  /**
   * Convert a chunk of byte:s to a word
   *
   * @param message The message
   * @param rr      Bitrate in bytes
   * @param ww      Word size in bytes
   * @param off     The offset in the message
   * @return Lane
   */
  private long toLane (byte[] message, int rr, int ww, int off) {

    long rc = 0;
    int n = Math.min(message.length, rr);
    for (int i = off + ww - 1; i >= off; i--)
      rc = (rc << 8) | ((i < n) ? (long)(message[i] & 255) : 0L);
    return rc;
  }

  /**
   * Convert a chunk of byte:s to a 64-bit word
   *
   * @param message The message
   * @param rr      Bitrate in bytes
   * @param off     The offset in the message
   * @return Lane
   */
  private long toLane64 (byte[] message, int rr, int off) {

    int n = Math.min(message.length, rr);
    return ((off + 7 < n) ? ((long)(message[off + 7] & 255) << 56) : 0L) |
             ((off + 6 < n) ? ((long)(message[off + 6] & 255) << 48) : 0L) |
             ((off + 5 < n) ? ((long)(message[off + 5] & 255) << 40) : 0L) |
             ((off + 4 < n) ? ((long)(message[off + 4] & 255) << 32) : 0L) |
             ((off + 3 < n) ? ((long)(message[off + 3] & 255) << 24) : 0L) |
             ((off + 2 < n) ? ((long)(message[off + 2] & 255) << 16) : 0L) |
             ((off + 1 < n) ? ((long)(message[off + 1] & 255) << 8) : 0L) |
             ((off < n) ? ((long)(message[off] & 255)) : 0L);
  }

  /**
   * pad 10*1
   *
   * @param msg The message to pad
   * @param r   The bitrate
   * @param len The length of the message
   * @return The message padded
   */
  private byte[] pad10star1 (byte[] msg, int len, int r) {

    int nrf = (len <<= 3) >> 3;
    int nbrf = len & 7;
    int ll = len % r;

    byte b = (byte)(nbrf == 0 ? 1 : ((msg[nrf] >> (8 - nbrf)) | (1 << nbrf)));

    byte[] message;
    if ((r - 8 <= ll) && (ll <= r - 2)) {
      message = new byte[nrf + 1];
      message[nrf] = (byte)(b ^ 128);
    } else {
      len = (nrf + 1) << 3;
      len = ((len - (len % r) + (r - 8)) >> 3) + 1;
      message = new byte[len];
      message[nrf] = b;
      message[len - 1] = -128;
    }
    System.arraycopy(msg, 0, message, 0, nrf);

    return message;
  }

  /**
   * Absorb the more of the message message to the Keccak sponge
   *
   * @param msg The partial message
   */
  public void update (byte[] msg) {

    update(msg, msg.length);
  }

  /**
   * Absorb the more of the message message to the Keccak sponge
   *
   * @param msg    The partial message
   * @param msglen The length of the partial message
   */
  public void update (byte[] msg, int msglen) {

    int rr = r >> 3;
    int ww = w >> 3;

    if (mptr + msglen > M.length)
      System.arraycopy(M, 0, M = new byte[(M.length + msglen) << 1], 0, mptr);
    System.arraycopy(msg, 0, M, mptr, msglen);
    int len = mptr += msglen;
    len -= len % ((r * b) >> 3);
    byte[] message;
    System.arraycopy(M, 0, message = new byte[len], 0, len);
    System.arraycopy(M, len, M, 0, mptr -= len);

    /* Absorbing phase */
    if (ww == 8)
      for (int i = 0; i < len; i += rr) {
        S[0] ^= toLane64(message, rr, i);
        S[5] ^= toLane64(message, rr, i + 8);
        S[10] ^= toLane64(message, rr, i + 16);
        S[15] ^= toLane64(message, rr, i + 24);
        S[20] ^= toLane64(message, rr, i + 32);
        S[1] ^= toLane64(message, rr, i + 40);
        S[6] ^= toLane64(message, rr, i + 48);
        S[11] ^= toLane64(message, rr, i + 56);
        S[16] ^= toLane64(message, rr, i + 64);
        S[21] ^= toLane64(message, rr, i + 72);
        S[2] ^= toLane64(message, rr, i + 80);
        S[7] ^= toLane64(message, rr, i + 88);
        S[12] ^= toLane64(message, rr, i + 96);
        S[17] ^= toLane64(message, rr, i + 104);
        S[22] ^= toLane64(message, rr, i + 112);
        S[3] ^= toLane64(message, rr, i + 120);
        S[8] ^= toLane64(message, rr, i + 128);
        S[13] ^= toLane64(message, rr, i + 136);
        S[18] ^= toLane64(message, rr, i + 144);
        S[23] ^= toLane64(message, rr, i + 152);
        S[4] ^= toLane64(message, rr, i + 160);
        S[9] ^= toLane64(message, rr, i + 168);
        S[14] ^= toLane64(message, rr, i + 176);
        S[19] ^= toLane64(message, rr, i + 184);
        S[24] ^= toLane64(message, rr, i + 192);
        keccakF(S);
      }
    else
      for (int i = 0; i < len; i += rr) {
        S[0] ^= toLane(message, rr, ww, i);
        S[5] ^= toLane(message, rr, ww, i + w);
        S[10] ^= toLane(message, rr, ww, i + 2 * w);
        S[15] ^= toLane(message, rr, ww, i + 3 * w);
        S[20] ^= toLane(message, rr, ww, i + 4 * w);
        S[1] ^= toLane(message, rr, ww, i + 5 * w);
        S[6] ^= toLane(message, rr, ww, i + 6 * w);
        S[11] ^= toLane(message, rr, ww, i + 7 * w);
        S[16] ^= toLane(message, rr, ww, i + 8 * w);
        S[21] ^= toLane(message, rr, ww, i + 9 * w);
        S[2] ^= toLane(message, rr, ww, i + 10 * w);
        S[7] ^= toLane(message, rr, ww, i + 11 * w);
        S[12] ^= toLane(message, rr, ww, i + 12 * w);
        S[17] ^= toLane(message, rr, ww, i + 13 * w);
        S[22] ^= toLane(message, rr, ww, i + 14 * w);
        S[3] ^= toLane(message, rr, ww, i + 15 * w);
        S[8] ^= toLane(message, rr, ww, i + 16 * w);
        S[13] ^= toLane(message, rr, ww, i + 17 * w);
        S[18] ^= toLane(message, rr, ww, i + 18 * w);
        S[23] ^= toLane(message, rr, ww, i + 19 * w);
        S[4] ^= toLane(message, rr, ww, i + 20 * w);
        S[9] ^= toLane(message, rr, ww, i + 21 * w);
        S[14] ^= toLane(message, rr, ww, i + 22 * w);
        S[19] ^= toLane(message, rr, ww, i + 23 * w);
        S[24] ^= toLane(message, rr, ww, i + 24 * w);
        keccakF(S);
      }
  }

  /**
   * Squeeze the Keccak sponge
   */
  public byte[] digest () {

    return digest(null);
  }

  /**
   * Absorb the last part of the message and squeeze the Keccak sponge
   *
   * @param msg The rest of the message
   */
  public byte[] digest (byte[] msg) {

    return digest(msg, msg == null ? 0 : msg.length);
  }

  /**
   * Absorb the last part of the message and squeeze the Keccak sponge
   *
   * @param msg    The rest of the message
   * @param msglen The length of the partial message
   */
  public byte[] digest (byte[] msg, int msglen) {

    byte[] message;

    if ((msg == null) || (msglen == 0)) {
      message = pad10star1(M, mptr, r);
    } else {
      if (mptr + msglen > M.length) {
        System.arraycopy(M, 0, M = new byte[M.length + msglen], 0, mptr);
      }

      System.arraycopy(msg, 0, M, mptr, msglen);
      message = pad10star1(M, mptr + msglen, r);
    }
    M = null;
    int len = message.length;
    byte[] rc = new byte[(n + 7) >> 3];
    int ptr = 0;

    int rr = r >> 3;
    int nn = n >> 3;
    int ww = w >> 3;

    /* Absorbing phase */
    if (ww == 8)
      for (int i = 0; i < len; i += rr) {
        S[0] ^= toLane64(message, rr, i);
        S[5] ^= toLane64(message, rr, i + 8);
        S[10] ^= toLane64(message, rr, i + 16);
        S[15] ^= toLane64(message, rr, i + 24);
        S[20] ^= toLane64(message, rr, i + 32);
        S[1] ^= toLane64(message, rr, i + 40);
        S[6] ^= toLane64(message, rr, i + 48);
        S[11] ^= toLane64(message, rr, i + 56);
        S[16] ^= toLane64(message, rr, i + 64);
        S[21] ^= toLane64(message, rr, i + 72);
        S[2] ^= toLane64(message, rr, i + 80);
        S[7] ^= toLane64(message, rr, i + 88);
        S[12] ^= toLane64(message, rr, i + 96);
        S[17] ^= toLane64(message, rr, i + 104);
        S[22] ^= toLane64(message, rr, i + 112);
        S[3] ^= toLane64(message, rr, i + 120);
        S[8] ^= toLane64(message, rr, i + 128);
        S[13] ^= toLane64(message, rr, i + 136);
        S[18] ^= toLane64(message, rr, i + 144);
        S[23] ^= toLane64(message, rr, i + 152);
        S[4] ^= toLane64(message, rr, i + 160);
        S[9] ^= toLane64(message, rr, i + 168);
        S[14] ^= toLane64(message, rr, i + 176);
        S[19] ^= toLane64(message, rr, i + 184);
        S[24] ^= toLane64(message, rr, i + 192);
        keccakF(S);
      }
    else
      for (int i = 0; i < len; i += rr) {
        S[0] ^= toLane(message, rr, ww, i);
        S[5] ^= toLane(message, rr, ww, i + w);
        S[10] ^= toLane(message, rr, ww, i + 2 * w);
        S[15] ^= toLane(message, rr, ww, i + 3 * w);
        S[20] ^= toLane(message, rr, ww, i + 4 * w);
        S[1] ^= toLane(message, rr, ww, i + 5 * w);
        S[6] ^= toLane(message, rr, ww, i + 6 * w);
        S[11] ^= toLane(message, rr, ww, i + 7 * w);
        S[16] ^= toLane(message, rr, ww, i + 8 * w);
        S[21] ^= toLane(message, rr, ww, i + 9 * w);
        S[2] ^= toLane(message, rr, ww, i + 10 * w);
        S[7] ^= toLane(message, rr, ww, i + 11 * w);
        S[12] ^= toLane(message, rr, ww, i + 12 * w);
        S[17] ^= toLane(message, rr, ww, i + 13 * w);
        S[22] ^= toLane(message, rr, ww, i + 14 * w);
        S[3] ^= toLane(message, rr, ww, i + 15 * w);
        S[8] ^= toLane(message, rr, ww, i + 16 * w);
        S[13] ^= toLane(message, rr, ww, i + 17 * w);
        S[18] ^= toLane(message, rr, ww, i + 18 * w);
        S[23] ^= toLane(message, rr, ww, i + 19 * w);
        S[4] ^= toLane(message, rr, ww, i + 20 * w);
        S[9] ^= toLane(message, rr, ww, i + 21 * w);
        S[14] ^= toLane(message, rr, ww, i + 22 * w);
        S[19] ^= toLane(message, rr, ww, i + 23 * w);
        S[24] ^= toLane(message, rr, ww, i + 24 * w);
        keccakF(S);
      }

    /* Squeezing phase */
    int olen = n;
    int j = 0;
    int ni = Math.min(25, rr);
    while (olen > 0) {
      int i = 0;
      while ((i < ni) && (j < nn)) {
        long v = S[(i % 5) * 5 + i / 5];
        for (int x = 0; x < ww; x++) {
          if (j < nn) {
            rc[ptr] = (byte)v;
            ptr += 1;
          }
          v >>= 8;
          j += 1;
        }
        i += 1;
      }
      olen -= r;
      if (olen > 0)
        keccakF(S);
    }
    return rc;
  }
}