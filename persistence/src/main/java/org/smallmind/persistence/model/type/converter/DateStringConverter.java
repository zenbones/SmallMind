package org.smallmind.persistence.model.type.converter;

import java.util.Date;
import org.smallmind.persistence.model.bean.BeanInvocationException;
import org.smallmind.persistence.model.type.PrimitiveType;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateStringConverter implements StringConverter<Date> {

   private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime();

   public PrimitiveType getPrimitiveType () {

      return PrimitiveType.DATE;
   }

   public Date convert (String value)
      throws BeanInvocationException {

      if ((value == null) || (value.length() == 0)) {

         return null;
      }

      return ISO_DATE_TIME_FORMATTER.parseDateTime(value).toDate();
   }
}
