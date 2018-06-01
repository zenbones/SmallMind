package org.smallmind.web.json.dto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.xml.bind.annotation.adapters.XmlAdapter;

@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface Property {

  Class<? extends XmlAdapter> adapter () default DefaultXmlAdapter.class;

  Class<?> type ();

  Visibility visibility () default Visibility.BOTH;

  String field ();

  String name () default "";

  String purpose () default "";

  boolean required () default false;
}
