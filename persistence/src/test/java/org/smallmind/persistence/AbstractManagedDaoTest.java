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
package org.smallmind.persistence;

import java.io.Serializable;
import org.smallmind.persistence.orm.ORMInitializationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the id-class dispatch in {@link AbstractManagedDao#getIdFromString(String)}. Each fixture binds
 * concrete generic type arguments on its subclass so the constructor's {@code TypeInference} resolves a
 * real id class, then exercises one conversion branch: {@code String}, enum, the reference wrappers that
 * generics can actually reify ({@code Long}/{@code Integer}/{@code Short}/{@code Byte}/{@code Double}/
 * {@code Float}/{@code Boolean}/{@code Character}), the {@link Identifier} static {@code fromString}
 * reflection path, the non-static {@code fromString} failure, and the unsupported-type throw.
 */
@Test(groups = "unit")
public class AbstractManagedDaoTest {

  public void testStringId () {

    Assert.assertEquals(new StringDao().getIdFromString("hello"), "hello");
  }

  public void testEnumId () {

    Assert.assertEquals(new ColorDao().getIdFromString("GREEN"), Color.GREEN);
  }

  public void testLongId () {

    Assert.assertEquals(new LongDao().getIdFromString("42"), Long.valueOf(42L));
  }

  public void testIntegerId () {

    Assert.assertEquals(new IntegerDao().getIdFromString("7"), Integer.valueOf(7));
  }

  public void testShortId () {

    Assert.assertEquals(new ShortDao().getIdFromString("9"), Short.valueOf((short)9));
  }

  public void testByteId () {

    Assert.assertEquals(new ByteDao().getIdFromString("3"), Byte.valueOf((byte)3));
  }

  public void testDoubleId () {

    Assert.assertEquals(new DoubleDao().getIdFromString("1.5"), Double.valueOf(1.5D));
  }

  public void testFloatId () {

    Assert.assertEquals(new FloatDao().getIdFromString("2.25"), Float.valueOf(2.25F));
  }

  public void testBooleanId () {

    Assert.assertEquals(new BooleanDao().getIdFromString("true"), Boolean.TRUE);
  }

  public void testCharacterId () {

    Assert.assertEquals(new CharacterDao().getIdFromString("Zebra"), Character.valueOf('Z'));
  }

  public void testIdentifierFromStringReflectionPath () {

    Assert.assertEquals(new GoodIdentifierDao().getIdFromString("abc"), new GoodIdentifier("abc"));
  }

  @Test(groups = "unit", expectedExceptions = ORMInitializationException.class)
  public void testIdentifierWithNonStaticFromStringThrows () {

    new BadIdentifierDao().getIdFromString("abc");
  }

  @Test(groups = "unit", expectedExceptions = ORMInitializationException.class)
  public void testUnsupportedIdTypeThrows () {

    new UnsupportedDao().getIdFromString("anything");
  }

  private abstract static class IdDurable<I extends Serializable & Comparable<I>> implements Durable<I> {

    private I id;

    @Override
    public I getId () {

      return id;
    }

    @Override
    public void setId (I id) {

      this.id = id;
    }

    @Override
    public int compareTo (Durable<I> durable) {

      return (id == null) ? ((durable.getId() == null) ? 0 : -1) : ((durable.getId() == null) ? 1 : id.compareTo(durable.getId()));
    }
  }

  public enum Color {

    RED, GREEN, BLUE
  }

  private static class ColorDurable extends IdDurable<Color> {

  }

  public static final class GoodIdentifier implements Identifier<GoodIdentifier> {

    private final String value;

    public GoodIdentifier (String value) {

      this.value = value;
    }

    public static GoodIdentifier fromString (String value) {

      return new GoodIdentifier(value);
    }

    @Override
    public int compareTo (GoodIdentifier other) {

      return value.compareTo(other.value);
    }

    @Override
    public int hashCode () {

      return value.hashCode();
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof GoodIdentifier) && value.equals(((GoodIdentifier)obj).value);
    }
  }

  private static class GoodIdentifierDurable extends IdDurable<GoodIdentifier> {

  }

  public static final class BadIdentifier implements Identifier<BadIdentifier> {

    private final String value;

    public BadIdentifier (String value) {

      this.value = value;
    }

    public BadIdentifier fromString (String value) {

      return new BadIdentifier(value);
    }

    @Override
    public int compareTo (BadIdentifier other) {

      return value.compareTo(other.value);
    }
  }

  private static class BadIdentifierDurable extends IdDurable<BadIdentifier> {

  }

  private static class StringDurable extends IdDurable<String> {

  }

  private static class LongDurable extends IdDurable<Long> {

  }

  private static class IntegerDurable extends IdDurable<Integer> {

  }

  private static class ShortDurable extends IdDurable<Short> {

  }

  private static class ByteDurable extends IdDurable<Byte> {

  }

  private static class DoubleDurable extends IdDurable<Double> {

  }

  private static class FloatDurable extends IdDurable<Float> {

  }

  private static class BooleanDurable extends IdDurable<Boolean> {

  }

  private static class CharacterDurable extends IdDurable<Character> {

  }

  private static class StringDao extends AbstractManagedDao<String, StringDurable> {

    public StringDao () {

      super("test");
    }
  }

  private static class ColorDao extends AbstractManagedDao<Color, ColorDurable> {

    public ColorDao () {

      super("test");
    }
  }

  private static class LongDao extends AbstractManagedDao<Long, LongDurable> {

    public LongDao () {

      super("test");
    }
  }

  private static class IntegerDao extends AbstractManagedDao<Integer, IntegerDurable> {

    public IntegerDao () {

      super("test");
    }
  }

  private static class ShortDao extends AbstractManagedDao<Short, ShortDurable> {

    public ShortDao () {

      super("test");
    }
  }

  private static class ByteDao extends AbstractManagedDao<Byte, ByteDurable> {

    public ByteDao () {

      super("test");
    }
  }

  private static class DoubleDao extends AbstractManagedDao<Double, DoubleDurable> {

    public DoubleDao () {

      super("test");
    }
  }

  private static class FloatDao extends AbstractManagedDao<Float, FloatDurable> {

    public FloatDao () {

      super("test");
    }
  }

  private static class BooleanDao extends AbstractManagedDao<Boolean, BooleanDurable> {

    public BooleanDao () {

      super("test");
    }
  }

  private static class CharacterDao extends AbstractManagedDao<Character, CharacterDurable> {

    public CharacterDao () {

      super("test");
    }
  }

  private static class GoodIdentifierDao extends AbstractManagedDao<GoodIdentifier, GoodIdentifierDurable> {

    public GoodIdentifierDao () {

      super("test");
    }
  }

  private static class BadIdentifierDao extends AbstractManagedDao<BadIdentifier, BadIdentifierDurable> {

    public BadIdentifierDao () {

      super("test");
    }
  }

  private static class UnsupportedDurable extends IdDurable<java.math.BigInteger> {

  }

  private static class UnsupportedDao extends AbstractManagedDao<java.math.BigInteger, UnsupportedDurable> {

    public UnsupportedDao () {

      super("test");
    }
  }
}
