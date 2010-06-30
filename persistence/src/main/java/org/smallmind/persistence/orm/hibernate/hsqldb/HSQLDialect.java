package org.smallmind.persistence.orm.hibernate.hsqldb;

public class HSQLDialect extends org.hibernate.dialect.HSQLDialect {

   @Override
   public String getSelectClauseNullString (int sqlType) {
      String typeName = getTypeName(sqlType, 1, 1, 0);
      //trim off the length/precision/scale
      int loc = typeName.indexOf('(');
      if (loc != -1) {
         typeName = typeName.substring(0, loc);
      }

      return "cast(null as " + typeName + ")";
   }
}
