/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.phalanx.wire;

import java.util.Date;

import org.smallmind.nutsnbolts.context.ContextException;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.phalanx.wire.mock.MockRequestTransport;
import org.smallmind.phalanx.wire.mock.MockResponseTransport;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MockWireTest {

    private ClassPathXmlApplicationContext context;
    private RequestTransport requestTransport;
    private ResponseTransport responseTransport;
    private WireTestingService wireTestingService;

    @BeforeClass
    public void beforeClass()
            throws Exception {

        new PerApplicationContext();

        //  This block loads the RabbitMQ backed Transport layer
    /*
    context = new ClassPathXmlApplicationContext("org/smallmind/foundation/foundation.xml", "org/smallmind/scribe/spring/test-logging.xml", "org/smallmind/phalanx/wire/rabbitmq-wire.xml");
    requestTransport = context.getBean("rabbitmqRequestTransport", RabbitMQRequestTransport.class);
    responseTransport = context.getBean("rabbitmqResponseTransport", RabbitMQResponseTransport.class);
    */

        //  This block loads the HornetQ backed Transport layer
    /*
    context = new ClassPathXmlApplicationContext("org/smallmind/foundation/foundation.xml", "org/smallmind/scribe/spring/test-logging.xml", "org/smallmind/phalanx/wire/hornetq-wire.xml");
    requestTransport = context.getBean("jmsRequestTransport", JmsRequestTransport.class);
    responseTransport = context.getBean("jmsResponseTransport", JmsResponseTransport.class);
    */

        //  This block loads the Mock backed Transport layer
        context = new ClassPathXmlApplicationContext("org/smallmind/foundation/foundation.xml", "org/smallmind/scribe/spring/test-logging.xml", "org/smallmind/phalanx/wire/mock-wire.xml");
        requestTransport = context.getBean("mockRequestTransport", MockRequestTransport.class);
        responseTransport = context.getBean("mockResponseTransport", MockResponseTransport.class);

        WireContextManager.register("test", TestWireContext.class);

        responseTransport.register(WireTestingService.class, new WireTestingServiceImpl());
        wireTestingService = (WireTestingService) WireProxyFactory.generateProxy(requestTransport, 1, "WireTestService", WireTestingService.class, new StaticParameterExtractor<>("test"), null, null);
    }

    @AfterClass
    public void afterClass()
            throws Exception {

        requestTransport.close();
        responseTransport.close();
        context.stop();
        context.close();
    }

    @Test
    public void testContext() {

        ContextFactory.pushContext(new TestWireContext("flibble"));
        try {
            Assert.assertTrue(wireTestingService.hasContext());
        } finally {
            ContextFactory.popContext(TestWireContext.class);
        }
    }

    @Test(dependsOnMethods = "testContext")
    public void testMissingContext() {

        ContextException capturedException = null;

        try {
            wireTestingService.requiresContext();
        } catch (ContextException contextException) {
            capturedException = contextException;
        }

        Assert.assertNotNull(capturedException);
    }

    @Test(dependsOnMethods = "testMissingContext")
    public void testPrimitiveArguments() {

        wireTestingService.doNothing();

        Assert.assertTrue(wireTestingService.echoBoolean(true));
        Assert.assertEquals(wireTestingService.echoByte((byte) 5), 5);
        Assert.assertEquals(wireTestingService.echoShort((short) 23), 23);
        Assert.assertEquals(wireTestingService.echoInt(4291), 4291);
        Assert.assertEquals(wireTestingService.echoLong(1234567L), 1234567L);
        Assert.assertEquals(wireTestingService.echoFloat(4.291F), 4.291F);
        Assert.assertEquals(wireTestingService.echoDouble(1.234567D), 1.234567D);
        Assert.assertEquals(wireTestingService.echoChar('G'), 'G');
    }

    @Test(dependsOnMethods = "testPrimitiveArguments")
    public void testComplexArguments() {

        Date now = new Date();
        Color[] colors = new Color[]{new Color("red"), new Color("white"), new Color("blue")};

        Assert.assertEquals(wireTestingService.echoString("The quick brown fox"), "The quick brown fox");
        Assert.assertEquals(wireTestingService.echoDate(now), now);
        Assert.assertEquals(wireTestingService.echoColors(colors), colors);
        Assert.assertEquals(wireTestingService.addNumbers(7, 8), (Integer) 15);
    }

    @Test(dependsOnMethods = "testComplexArguments")
    public void testFault() {

        WireTestingException capturedException = null;

        try {
            wireTestingService.throwError();
        } catch (WireTestingException wireTestingException) {
            capturedException = wireTestingException;
        }

        Assert.assertNotNull(capturedException);
    }
}