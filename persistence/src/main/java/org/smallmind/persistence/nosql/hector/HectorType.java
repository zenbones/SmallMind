/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.persistence.nosql.hector;

import java.util.Date;
import org.smallmind.persistence.PersistenceException;

public enum HectorType {

  BYTE(new ByteTranslator()), SHORT(new ShortTranslator()), INTEGER(new IntegerTranslator()), LONG(new LongTranslator()), FLOAT(new FloatTranslator()), DOUBLE(new DoubleTranslator()), BOOLEAN(new BooleanTranslator()), CHARACTER(new CharacterTranslator()), STRING(new StringTranslator()), DATE(new DateTranslator()), ENUM(new EnumTranslator());

  private HectorTranslator hectorTranslator;

  private HectorType (HectorTranslator hectorTranslator) {

    this.hectorTranslator = hectorTranslator;
  }

  public HectorTranslator getHectorTranslator () {

    return hectorTranslator;
  }

  public static HectorTranslator getTranslator (Class fieldType, String fieldName) {

    if (Date.class.isAssignableFrom(fieldType)) {
      return HectorType.DATE.getHectorTranslator();
    }
    else if (fieldType.isEnum()) {
      return HectorType.ENUM.getHectorTranslator();
    }
    else if (CharSequence.class.isAssignableFrom(fieldType)) {
      return HectorType.STRING.getHectorTranslator();
    }
    else if (long.class.equals(fieldType) || (Long.class.equals(fieldType))) {
      return HectorType.LONG.getHectorTranslator();
    }
    else if (boolean.class.equals(fieldType) || (Boolean.class.equals(fieldType))) {
      return HectorType.BOOLEAN.getHectorTranslator();
    }
    else if (int.class.equals(fieldType) || (Integer.class.equals(fieldType))) {
      return HectorType.INTEGER.getHectorTranslator();
    }
    else if (double.class.equals(fieldType) || (Double.class.equals(fieldType))) {
      return HectorType.DOUBLE.getHectorTranslator();
    }
    else if (float.class.equals(fieldType) || (Float.class.equals(fieldType))) {
      return HectorType.FLOAT.getHectorTranslator();
    }
    else if (char.class.equals(fieldType) || (Character.class.equals(fieldType))) {
      return HectorType.CHARACTER.getHectorTranslator();
    }
    else if (short.class.equals(fieldType) || (Short.class.equals(fieldType))) {
      return HectorType.SHORT.getHectorTranslator();
    }
    else if (byte.class.equals(fieldType) || (Byte.class.equals(fieldType))) {
      return HectorType.BYTE.getHectorTranslator();
    }
    else {
      throw new PersistenceException("Unknown field(%s) type(%s)", fieldName, fieldType.getName());
    }
  }
}
