package org.smallmind.web.jersey.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeXmlAdapter extends XmlAdapter<String, DateTime> {

  private static DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.dateTime();
  private static DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

  @Override
  public DateTime unmarshal (String value) {

    return (value == null) ? null : (value.contains("T")) ? ISO_DATE_FORMATTER.parseDateTime(value) : DATE_FORMATTER.parseDateTime(value);
  }

  @Override
  public String marshal (DateTime dateTime) {

    return (dateTime == null) ? null : ISO_DATE_FORMATTER.print(dateTime);
  }
}