package org.smallmind.scribe.pen.probe;

import java.io.Serializable;
import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.MessageTranslator;

public class Statement implements Serializable {

   private Discriminator discriminator;
   private Level level;
   private String message;
   private Object[] args;

   public Statement (Discriminator discriminator, Level level, String message, Object... args) {

      this.discriminator = discriminator;
      this.level = level;
      this.message = message;
      this.args = args;
   }

   public Discriminator getDiscriminator () {

      return discriminator;
   }

   public Level getLevel () {

      return level;
   }

   public String getMessage () {

      return MessageTranslator.translateMessage(message, args);
   }
}
