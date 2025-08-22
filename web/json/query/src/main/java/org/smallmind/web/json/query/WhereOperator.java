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
package org.smallmind.web.json.query;

import java.util.Date;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.NumberComparator;

public enum WhereOperator {

  LT {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if ((ElementType.NUMBER.equals(op1.getElementType()) && ElementType.NUMBER.equals(op2.getElementType()))) {
          return NUMBER_COMPARATOR.compare((Number)op2.get(), (Number)op1.get()) < 0;
        } else if ((ElementType.DATE.equals(op1.getElementType()) && ElementType.DATE.equals(op2.getElementType()))) {
          return ((Date)op2.get()).before((Date)op1.get());
        }
      }

      throw new QueryProcessingException("The operator(%s) requires numeric or date inputs", name());
    }
  },
  LE {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if ((ElementType.NUMBER.equals(op1.getElementType()) && ElementType.NUMBER.equals(op2.getElementType()))) {
          return NUMBER_COMPARATOR.compare((Number)op2.get(), (Number)op1.get()) <= 0;
        } else if ((ElementType.DATE.equals(op1.getElementType()) && ElementType.DATE.equals(op2.getElementType()))) {
          return op2.get().equals(op1.get()) || ((Date)op2.get()).before((Date)op1.get());
        }
      }

      throw new QueryProcessingException("The operator(%s) requires numeric or date inputs", name());
    }
  },
  EQ {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if (ElementType.NULL.equals(op1.getElementType())) {
          return ElementType.NULL.equals(op2.getElementType());
        } else if (ElementType.NULL.equals(op2.getElementType())) {
          return false;
        } else if (op1.getElementType().equals(op2.getElementType())) {
          switch (op1.getElementType()) {
            case BOOLEAN:
              return op1.get().equals(op2.get());
            case NUMBER:
              return NUMBER_COMPARATOR.compare((Number)op1.get(), (Number)op2.get()) == 0;
            case STRING:
              return op1.get().equals(op2.get());
            case DATE:
              return op1.get().equals(op2.get());
            default:
              throw new QueryProcessingException(new UnknownSwitchCaseException(op1.getElementType().name()));
          }
        }
      }

      throw new QueryProcessingException("The operator(%s) is undefined for the operand types(%s and %s)", name(), op1.getOperandType().name(), op2.getOperandType().name());
    }
  },
  NE {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if (ElementType.NULL.equals(op1.getElementType())) {
          return !ElementType.NULL.equals(op2.getElementType());
        } else if (ElementType.NULL.equals(op2.getElementType())) {
          return true;
        } else if (op1.getElementType().equals(op2.getElementType())) {
          switch (op1.getElementType()) {
            case BOOLEAN:
              return !op1.get().equals(op2.get());
            case NUMBER:
              return NUMBER_COMPARATOR.compare((Number)op1.get(), (Number)op2.get()) != 0;
            case STRING:
              return !op1.get().equals(op2.get());
            case DATE:
              return !op1.get().equals(op2.get());
            default:
              throw new QueryProcessingException(new UnknownSwitchCaseException(op1.getElementType().name()));
          }
        }
      }

      throw new QueryProcessingException("The operator(%s) is undefined for the operand types(%s and %s)", name(), op1.getOperandType().name(), op2.getOperandType().name());
    }
  },
  GE {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if ((ElementType.NUMBER.equals(op1.getElementType()) && ElementType.NUMBER.equals(op2.getElementType()))) {
          return NUMBER_COMPARATOR.compare((Number)op2.get(), (Number)op1.get()) >= 0;
        } else if ((ElementType.DATE.equals(op1.getElementType()) && ElementType.DATE.equals(op2.getElementType()))) {
          return op2.get().equals(op1.get()) || ((Date)op2.get()).after((Date)op1.get());
        }
      }

      throw new QueryProcessingException("The operator(%s) requires numeric or date inputs", name());
    }
  },
  GT {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if ((ElementType.NUMBER.equals(op1.getElementType()) && ElementType.NUMBER.equals(op2.getElementType()))) {
          return NUMBER_COMPARATOR.compare((Number)op2.get(), (Number)op1.get()) > 0;
        } else if ((ElementType.DATE.equals(op1.getElementType()) && ElementType.DATE.equals(op2.getElementType()))) {
          return ((Date)op2.get()).after((Date)op1.get());
        }
      }
      throw new QueryProcessingException("The operator(%s) requires numeric or date inputs", name());
    }
  },
  EXISTS {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!OperandType.BOOLEAN.equals(op1.getOperandType())) {
        throw new QueryProcessingException("The operator(%s) requires a boolean operand", name());
      }
      if (OperandType.ARRAY.equals(op2.getOperandType())) {
        throw new QueryProcessingException("The operator(%s) does not accept array inputs", name());
      } else {
        return ElementType.NULL.equals(op2.getElementType()) ? Boolean.FALSE.equals(op1.get()) : Boolean.TRUE.equals(op1.get());
      }
    }
  },
  LIKE {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (!(OperandType.ARRAY.equals(op1.getOperandType()) || OperandType.ARRAY.equals(op2.getOperandType()))) {
        if (ElementType.STRING.equals(op1.getElementType())) {
          if (ElementType.NULL.equals(op2.getElementType())) {
            return false;
          } else if (ElementType.STRING.equals(op2.getElementType())) {
            switch (((String)op1.get()).length()) {
              case 0:
                return op2.get().equals("");
              case 1:
                return op1.get().equals(SINGLE_WILDCARD) || op2.get().equals(op1.get());
              case 2:
                return op1.get().equals(DOUBLE_WILDCARD) || (((String)op1.get()).charAt(0) == WILDCARD_CHAR) ? ((String)op2.get()).endsWith(((String)op1.get()).substring(1)) : (((String)op1.get()).charAt(1) == WILDCARD_CHAR) ? ((String)op2.get()).startsWith(((String)op1.get()).substring(0, 1)) : op2.get().equals(op1.get());
              default:
                if (((String)op1.get()).substring(1, ((String)op1.get()).length() - 1).indexOf(WILDCARD_CHAR) >= 0) {
                  throw new QueryProcessingException("The operation(%s) allows wild cards(%s) only at the  start or end of the operand", name(), SINGLE_WILDCARD);
                } else if (((String)op1.get()).startsWith(SINGLE_WILDCARD) && ((String)op1.get()).endsWith(SINGLE_WILDCARD)) {

                  return ((String)op2.get()).contains(((String)op1.get()).substring(1, ((String)op1.get()).length() - 1));
                } else if (((String)op1.get()).startsWith(SINGLE_WILDCARD)) {

                  return ((String)op2.get()).endsWith(((String)op1.get()).substring(1));
                } else if (((String)op1.get()).endsWith(SINGLE_WILDCARD)) {

                  return ((String)op2.get()).startsWith(((String)op1.get()).substring(0, ((String)op1.get()).length() - 1));
                } else {

                  return op2.get().equals(op1.get());
                }
            }
          }
        }
      }

      throw new QueryProcessingException("The operator(%s) requires a string operand and a string or null input", name());
    }
  },
  UNLIKE {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      return !LIKE.isTrue(op1, op2);
    }
  },
  MATCH {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      // This is a natural language query using the 'match' database function that can't be replicated outside the database
      throw new UnsupportedOperationException("The 'match' operator can't be emulated outside a supporting database");
    }
  },
  IN {
    @Override
    public boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2) {

      if (OperandType.ARRAY.equals(op1.getOperandType()) && (!OperandType.ARRAY.equals(op2.getOperandType()))) {
        if (op1.getElementType().equals(op2.getElementType())) {
          if (ElementType.NULL.equals(op2.getElementType())) {
            return true;
          } else {
            for (Object element : (Object[])op1.get()) {
              switch (op2.getElementType()) {
                case BOOLEAN:
                  if (op2.get().equals(element)) {
                    return true;
                  }
                  break;
                case NUMBER:
                  if (NUMBER_COMPARATOR.compare((Number)element, (Number)op2.get()) == 0) {
                    return true;
                  }
                  break;
                case STRING:
                  if (op2.get().equals(element)) {
                    return true;
                  }
                  break;
                case DATE:
                  if (op2.get().equals(element)) {
                    return true;
                  }
                  break;
                default:
                  throw new QueryProcessingException(new UnknownSwitchCaseException(op1.getElementType().name()));
              }
            }

            return false;
          }
        }
      }

      throw new QueryProcessingException("The operator(%s) requires an array operand and a singular non-null input matching the component type", name());
    }
  };

  private static final NumberComparator NUMBER_COMPARATOR = new NumberComparator();
  private static final String SINGLE_WILDCARD = "*";
  private static final String DOUBLE_WILDCARD = SINGLE_WILDCARD + SINGLE_WILDCARD;
  private static final char WILDCARD_CHAR = '*';

  public abstract boolean isTrue (WhereOperand<?> op1, WhereOperand<?> op2);
}
