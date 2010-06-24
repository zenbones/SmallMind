package org.smallmind.persistence.orm.type;

import java.util.Currency;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class CurrencyAdapter extends XmlAdapter<String, Currency> {

   public Currency unmarshal (String value)
      throws Exception {

      return Currency.getInstance(value);
   }

   public String marshal (Currency currency)
      throws Exception {

      return currency.toString();
   }
}
