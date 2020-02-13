package org.smallmind.ansible;

import java.io.PrintStream;
import java.util.function.BiConsumer;
import org.smallmind.nutsnbolts.command.template.Template;

public class VaultHelp {

  private Template template;
  private BiConsumer<Template, PrintStream> help;

  public VaultHelp (Template template, BiConsumer<Template, PrintStream> help) {

    this.template = template;
    this.help = help;
  }

  public Template getTemplate () {

    return template;
  }

  public void out (PrintStream stream) {

    help.accept(template, stream);
  }
}
