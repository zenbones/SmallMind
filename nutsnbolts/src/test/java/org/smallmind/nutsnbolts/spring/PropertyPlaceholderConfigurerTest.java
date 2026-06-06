/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.spring;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PropertyPlaceholderConfigurerTest {

  private static Path writePropertiesFile (Path directory, String name, String content)
    throws Exception {

    Path file = directory.resolve(name);

    Files.writeString(file, content, StandardCharsets.UTF_8);

    return file;
  }

  @BeforeMethod
  public void installPerApplicationContext () {

    new PerApplicationContext();
  }

  public void testOrderRoundTripsThroughAccessor () {

    PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();

    configurer.setOrder(17);

    Assert.assertEquals(configurer.getOrder(), 17);
  }

  public void testDebugMapInitiallyEmptyAndUnmodifiable () {

    PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();

    Assert.assertTrue(configurer.getDebugMap().isEmpty());

    try {
      configurer.getDebugMap().put("k", "v");
      Assert.fail("Expected unmodifiable map");
    } catch (UnsupportedOperationException expected) {

    }
  }

  public void testPostProcessResolvesPlaceholdersFromExplicitLocation ()
    throws Exception {

    Path tempDir = Files.createTempDirectory("ppctest-");
    try {
      Path propertiesFile = writePropertiesFile(tempDir, "config.properties", "service.host=db.example.com\nservice.port=5432\n");

      DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
      GenericBeanDefinition targetDefinition = new GenericBeanDefinition();
      targetDefinition.setBeanClass(TargetBean.class);
      targetDefinition.getPropertyValues().add("host", "${service.host}");
      targetDefinition.getPropertyValues().add("port", "${service.port}");
      beanFactory.registerBeanDefinition("target", targetDefinition);

      PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
      configurer.setBeanFactory(beanFactory);
      configurer.setBeanName("propertyPlaceholderConfigurer");
      configurer.setLocations(Arrays.asList("file:" + propertiesFile.toString().replace('\\', '/')));

      configurer.postProcessBeanFactory(beanFactory);

      TargetBean target = (TargetBean)beanFactory.getBean("target");
      Assert.assertEquals(target.getHost(), "db.example.com");
      Assert.assertEquals(target.getPort(), 5432);
    } finally {
      try (var paths = Files.walk(tempDir)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception ignored) {

          }
        });
      }
    }
  }

  public void testDebugKeysPopulatesDebugMap ()
    throws Exception {

    Path tempDir = Files.createTempDirectory("ppctest-");
    try {
      Path propertiesFile = writePropertiesFile(tempDir, "config.properties", "app.label=hello\napp.secret=shh\nsystem.region=us-east-1\n");

      DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

      PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
      configurer.setBeanFactory(beanFactory);
      configurer.setBeanName("propertyPlaceholderConfigurer");
      configurer.setLocations(Arrays.asList("file:" + propertiesFile.toString().replace('\\', '/')));
      configurer.setDebugKeys(new String[] {"app.*"});

      configurer.postProcessBeanFactory(beanFactory);

      Assert.assertTrue(configurer.getDebugMap().containsKey("app.label"));
      Assert.assertTrue(configurer.getDebugMap().containsKey("app.secret"));
      Assert.assertFalse(configurer.getDebugMap().containsKey("system.region"));
      Assert.assertEquals(configurer.getDebugMap().get("app.label"), "hello");
    } finally {
      try (var paths = Files.walk(tempDir)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception ignored) {

          }
        });
      }
    }
  }

  @Test(expectedExceptions = RuntimeBeansException.class)
  public void testMissingResourceWithoutIgnoreFlagThrows () {

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
    configurer.setBeanFactory(beanFactory);
    configurer.setBeanName("propertyPlaceholderConfigurer");
    configurer.setLocations(Arrays.asList("file:/nonexistent/path/config.properties"));

    configurer.postProcessBeanFactory(beanFactory);
  }

  public void testMissingResourceWithIgnoreFlagSucceeds () {

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
    configurer.setBeanFactory(beanFactory);
    configurer.setBeanName("propertyPlaceholderConfigurer");
    configurer.setLocations(Arrays.asList("file:/nonexistent/path/config.properties"));
    configurer.setIgnoreResourceNotFound(true);

    configurer.postProcessBeanFactory(beanFactory);
  }

  public static class TargetBean {

    private String host;
    private int port;

    public String getHost () {

      return host;
    }

    public void setHost (String host) {

      this.host = host;
    }

    public int getPort () {

      return port;
    }

    public void setPort (int port) {

      this.port = port;
    }
  }
}
