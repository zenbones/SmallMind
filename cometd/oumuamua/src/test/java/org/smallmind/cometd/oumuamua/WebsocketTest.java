package org.smallmind.cometd.oumuamua;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.Test;

@Test
public class WebsocketTest {

  public void test ()
    throws Exception {

    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/smallmind/cometd/oumuamua/oumuamua-grizzly.xml", "org/smallmind/cometd/oumuamua/oumuamua.xml");

    Thread.sleep(300000);
  }
}
