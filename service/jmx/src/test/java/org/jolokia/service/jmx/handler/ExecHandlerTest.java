package org.jolokia.service.jmx.handler;

/*
 * Copyright 2009-2013 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

import javax.management.*;

import org.jolokia.core.request.NotChangedException;
import org.jolokia.core.request.JolokiaExecRequest;
import org.jolokia.core.request.JolokiaRequestBuilder;
import org.jolokia.core.util.TestJolokiaContext;
import org.testng.annotations.*;

import static org.jolokia.core.util.RequestType.EXEC;
import static org.testng.Assert.*;

/**
 * @author roland
 * @since 19.04.11
 */
public class ExecHandlerTest {

    private ExecHandler handler;

    private ObjectName oName;

    private TestJolokiaContext ctx;

    @BeforeMethod
    public void createHandler() throws MalformedObjectNameException {
        ctx = new TestJolokiaContext();
        handler = new ExecHandler(ctx);
    }

    @BeforeClass
    public void registerMBean() throws MalformedObjectNameException, MBeanException, InstanceAlreadyExistsException, IOException, NotCompliantMBeanException, ReflectionException {
        oName = new ObjectName("jolokia:test=exec");

        MBeanServerConnection conn = getMBeanServer();
        conn.createMBean(ExecData.class.getName(),oName);
    }

    @AfterClass
    public void unregisterMBean() throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        MBeanServerConnection conn = getMBeanServer();
        conn.unregisterMBean(oName);
    }

    @Test
    public void simple() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, MBeanException, AttributeNotFoundException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC, oName).
                operation("simple").
                build();
        assertEquals(handler.getType(), EXEC);
        Object res = handler.handleRequest(getMBeanServer(),request);
        assertNull(res);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void simpleWithWrongArguments() throws InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, MalformedObjectNameException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC, oName).
                operation("simple").
                arguments("blub","bla").
                build();
        handler.handleRequest(getMBeanServer(),request);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void illegalRequest() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC, oName).build();
        handler.handleRequest(getMBeanServer(),request);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void illegalOperationName() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC, oName).operation("koan titel").build();
        handler.handleRequest(getMBeanServer(),request);
    }


    @Test
    public void execWithArgumentsAndReturn() throws Exception {
        List list = new ArrayList();
        list.add("wollscheid");

        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC,oName).
                operation("withArgs").
                arguments(10L,list,Boolean.TRUE)
                .build();
        Map result = (Map) handler.handleRequest(getMBeanServer(),request);
        assertEquals(result.get("long"),10L);
        assertTrue(result.get("list") instanceof List);
        assertEquals(((List) result.get("list")).get(0), "wollscheid");
        assertTrue((Boolean) result.get("boolean"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void overloadedFailed() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC,oName).
                operation("overloaded").
                build();
        handler.handleRequest(getMBeanServer(),request);
    }

    @Test
    public void overloaded() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC,oName).
                operation("overloaded(int)").
                arguments(10).
                build();
        Integer res = (Integer) handler.handleRequest(getMBeanServer(),request);
        assertEquals(res,Integer.valueOf(1));

        request = new JolokiaRequestBuilder(EXEC,oName).
                operation("overloaded(int,java.lang.String)").
                arguments(10,"bla").
                build();
        res = (Integer) handler.handleRequest(getMBeanServer(),request);
        assertEquals(res,Integer.valueOf(2));

        request = new JolokiaRequestBuilder(EXEC,oName).
                operation("overloaded(boolean)").
                arguments(true).
                build();
        res = (Integer) handler.handleRequest(getMBeanServer(),request);
        assertEquals(res,Integer.valueOf(3));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void overloadedWrongSignature() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException, NotChangedException {
        JolokiaExecRequest request = new JolokiaRequestBuilder(EXEC,oName).
                operation("overloaded(java.lang.Integer)").
                arguments(1).
                build();
        handler.handleRequest(getMBeanServer(),request);
    }

    private MBeanServerConnection getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

}

