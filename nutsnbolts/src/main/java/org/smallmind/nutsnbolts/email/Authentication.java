package org.smallmind.nutsnbolts.email;

public class Authentication {

   public static enum AuthType {

      LOGIN
   }

   private String user;
   private String password;
   private AuthType authType;

   public Authentication (String user, String password, AuthType authType) {

      this.user = user;
      this.password = password;
      this.authType = authType;
   }

   public String getUser () {

      return user;
   }

   public String getPassword () {

      return password;
   }

   public AuthType getAuthType () {

      return authType;
   }

}
