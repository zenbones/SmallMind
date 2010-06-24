package org.smallmind.wicket.validator;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

public class PasswordComplexityValidator extends AbstractValidator {

   private static final PasswordComplexityValidator STATIC_INSTANCE = new PasswordComplexityValidator();

   public static PasswordComplexityValidator getInstance () {

      return STATIC_INSTANCE;
   }

   protected void onValidate (IValidatable iValidatable) {

      String password;
      int digitCount = 0;
      int punctuationCount = 0;

      password = (String)iValidatable.getValue();

      if (password.length() < 6) {
         error(iValidatable, "error.password.complexity.length");
      }

      for (int count = 0; count < password.length(); count++) {
         if (Character.isDigit(password.charAt(count))) {
            digitCount++;
         }
         else if (!Character.isLetter(password.charAt(count))) {
            punctuationCount++;
         }
      }

      if ((digitCount < 1) && (punctuationCount < 1)) {
         error(iValidatable, "error.password.complexity.safety");
      }
   }

}