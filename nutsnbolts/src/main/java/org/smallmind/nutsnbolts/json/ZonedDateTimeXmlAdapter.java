package org.smallmind.nutsnbolts.json;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class ZonedDateTimeXmlAdapter extends XmlAdapter<String, ZonedDateTime> {

  private static DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

  @Override
  public ZonedDateTime unmarshal (String value) {

    return (value == null) ? null : (ZonedDateTime)ISO_FORMATTER.parse(value);
  }

  @Override
  public String marshal (ZonedDateTime zonedDateTime) {

    return (zonedDateTime == null) ? null : ISO_FORMATTER.format(zonedDateTime);
  }
}