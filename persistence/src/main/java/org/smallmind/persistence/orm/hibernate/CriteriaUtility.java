package org.smallmind.persistence.orm.hibernate;

import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.persistence.query.Sort;
import org.smallmind.persistence.query.SortField;
import org.smallmind.persistence.query.Where;
import org.smallmind.persistence.query.WhereField;

public class CriteriaUtility {

  public class CriteriaUtility {

    public static Criteria apply (Where where, Criteria criteria) {

      if ((where != null) && (!where.isEmpty())) {
        for (WhereField whereField : where.getFields()) {
          switch (whereField.getOperation()) {
            case LT:
              criteria.add(Restrictions.lt(whereField.getName(), whereField.getValue().getValue()));
              break;
            case LE:
              criteria.add(Restrictions.le(whereField.getName(), whereField.getValue().getValue()));
              break;
            case EQ:

              Object value;

              criteria.add(((value = whereField.getValue().getValue()) == null) ? Restrictions.isNull(whereField.getName()) : Restrictions.eq(whereField.getName(), value));
              break;
            case GE:
              criteria.add(Restrictions.ge(whereField.getName(), whereField.getValue().getValue()));
              break;
            case GT:
              criteria.add(Restrictions.gt(whereField.getName(), whereField.getValue().getValue()));
              break;
            case LIKE:
              criteria.add(Restrictions.like(whereField.getName(), whereField.getValue().getValue()));
              break;
            case IN:
              criteria.add(Restrictions.in(whereField.getName(), (Object[])whereField.getValue().getValue()));
              break;
            default:
              throw new UnknownSwitchCaseException(whereField.getOperation().name());
          }
        }
      }

      return criteria;
    }

    public static Criteria apply (Sort sort, Criteria criteria) {

      if ((sort != null) && (!sort.isEmpty())) {
        for (SortField sortField : sort.getFields()) {
          switch (sortField.getDirection()) {
            case ASC:
              criteria.addOrder(Order.asc(sortField.getName()));
              break;
            case DESC:
              criteria.addOrder(Order.desc(sortField.getName()));
              break;
            default:
              throw new UnknownSwitchCaseException(sortField.getDirection().name());
          }
        }
      }

      return criteria;
    }
  }
}
