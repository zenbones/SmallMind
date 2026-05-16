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
package org.smallmind.batch.spring;

import org.mockito.Mockito;
import org.springframework.batch.core.job.Job;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BatchJobRegistryTest {

  private static abstract class AbstractJob implements Job {

  }

  public void testRegisterIsUnsupported () {

    try {
      new BatchJobRegistry().register(Mockito.mock(Job.class));
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      Assert.assertTrue(expected.getMessage().contains("spring context"));
    }
  }

  public void testUnregisterIsUnsupported () {

    try {
      new BatchJobRegistry().unregister("anything");
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      Assert.assertTrue(expected.getMessage().contains("deregistered"));
    }
  }

  public void testGetJobDelegatesToApplicationContext () {

    BatchJobRegistry registry = new BatchJobRegistry();
    ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
    ContextRefreshedEvent event = Mockito.mock(ContextRefreshedEvent.class);
    Job job = Mockito.mock(Job.class);

    Mockito.when(event.getApplicationContext()).thenReturn(applicationContext);
    Mockito.when(applicationContext.getBean("loader", Job.class)).thenReturn(job);

    registry.onApplicationEvent(event);

    Assert.assertSame(registry.getJob("loader"), job);
  }

  public void testPostProcessBeanFactoryCollectsOnlyJobBeans ()
    throws Exception {

    BatchJobRegistry registry = new BatchJobRegistry();
    ConfigurableListableBeanFactory beanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);

    Mockito.when(beanFactory.getBeanDefinitionNames()).thenReturn(new String[] {"jobOne", "notAJob", "jobTwo", "unknownType"});
    Mockito.doReturn(AbstractJob.class).when(beanFactory).getType("jobOne");
    Mockito.doReturn(String.class).when(beanFactory).getType("notAJob");
    Mockito.doReturn(AbstractJob.class).when(beanFactory).getType("jobTwo");
    Mockito.when(beanFactory.getType("unknownType")).thenReturn(null);

    registry.postProcessBeanFactory(beanFactory);

    Assert.assertEquals(registry.getJobNames().size(), 2);
    Assert.assertTrue(registry.getJobNames().contains("jobOne"));
    Assert.assertTrue(registry.getJobNames().contains("jobTwo"));
    Assert.assertFalse(registry.getJobNames().contains("notAJob"));
    Assert.assertFalse(registry.getJobNames().contains("unknownType"));
  }

  public void testGetJobNamesIsUnmodifiable () {

    BatchJobRegistry registry = new BatchJobRegistry();

    try {
      registry.getJobNames().add("sneaky");
      Assert.fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // expected
    }
  }
}
