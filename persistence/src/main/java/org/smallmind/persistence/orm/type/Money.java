package org.smallmind.persistence.orm.type;

import java.io.Serializable;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class Money implements Serializable {

   private static final Map<Character, Locale> CURRENCY_MAP = new HashMap<Character, Locale>();

   private Currency currency;
   private double amount;

   static {

      CURRENCY_MAP.put('$', Locale.US);
   }

   public static Locale getLocaleForCurrencySymbol (char symbol) {

      return CURRENCY_MAP.get(symbol);
   }

   public static Money createEmptyInstance (Locale locale) {

      return new Money(Currency.getInstance(locale), 0);
   }

   public Money () {
   }

   public Money (Currency currency, double amount) {

      this.currency = currency;
      this.amount = amount;
   }

   @XmlJavaTypeAdapter (CurrencyAdapter.class)
   public Currency getCurrency () {

      return currency;
   }

   public void setCurrency (Currency currency) {

      this.currency = currency;
   }

   public double getAmount () {

      return amount;
   }

   public void setAmount (double amount) {

      this.amount = amount;
   }
}
