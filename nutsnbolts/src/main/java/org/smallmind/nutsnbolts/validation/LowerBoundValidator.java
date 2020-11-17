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
package org.smallmind.nutsnbolts.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.smallmind.nutsnbolts.reflection.bean.BeanAccessException;
import org.smallmind.nutsnbolts.reflection.bean.BeanInvocationException;
import org.smallmind.nutsnbolts.reflection.bean.BeanUtility;
import org.smallmind.nutsnbolts.util.NumberComparator;

public class LowerBoundValidator implements ConstraintValidator<LowerBound, Object> {

  private static final NumberComparator NUMBER_COMPARATOR = new NumberComparator();

  private LowerBound constraintAnnotation;

  @Override
  public void initialize (LowerBound constraintAnnotation) {

    this.constraintAnnotation = constraintAnnotation;
  }

  @Override
  public boolean isValid (Object value, ConstraintValidatorContext context) {

    if (value == null) {
      return true;
    } else {

      try {

        Number number1 = (Number)BeanUtility.executeGet(value, constraintAnnotation.first(), false);
        Number number2 = (Number)BeanUtility.executeGet(value, constraintAnnotation.second(), false);

        return ((number1 == null) || (number2 == null)) ? !constraintAnnotation.notNull() : NUMBER_COMPARATOR.compare(number1, number2) >= constraintAnnotation.value();
      } catch (BeanAccessException | BeanInvocationException exception) {
        throw new RuntimeException(exception);
      }
    }
  }
}