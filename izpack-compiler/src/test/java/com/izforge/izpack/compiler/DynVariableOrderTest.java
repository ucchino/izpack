/*
 * IzPack - Copyright 2001-2012 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.compiler;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.jar.JarFile;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.izforge.izpack.api.data.DynamicVariable;
import com.izforge.izpack.compiler.container.TestCompilerContainer;
import com.izforge.izpack.test.Container;
import com.izforge.izpack.test.InstallFile;
import com.izforge.izpack.test.junit.PicoRunner;

/**
 * Tests for correct order of dynamic variable computation
 * 
 */
@RunWith(PicoRunner.class)
@Container(TestCompilerContainer.class)
public class DynVariableOrderTest
{
    static final String xmlDir="samples/dynvars/";  // Where we find our installer definitions
    
    private CompilerConfig compilerConfig;
    private TestCompilerContainer testContainer;

    List<String> orderedVarnames;

    public DynVariableOrderTest(TestCompilerContainer container, CompilerConfig compilerConfig)
    {
        this.testContainer = container;
        this.compilerConfig = compilerConfig;
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        compilerConfig.executeCompiler();
        JarFile jar = testContainer.getComponent(JarFile.class);
        InputStream ios = jar.getInputStream(jar.getEntry("resources/dynvariables"));
        Object object = new ObjectInputStream(ios).readObject();
        List<DynamicVariable> dynVars = (List<DynamicVariable>) object;
        orderedVarnames = new ArrayList<String>(dynVars.size());
        for (DynamicVariable var : dynVars) {
            orderedVarnames.add(var.getName());
        }
        StringBuffer sb = new StringBuffer(
                String.format("Installer built from '%s' has this ordering of variable computation:%n",
                               testContainer.getXmlFileName()));
        for (String name : orderedVarnames) {
            sb.append(name).append(", ");
        }
        System.out.println(sb);
    }

    /**
     * Check the default order of variables without dependencies
     */
    @Test
    @InstallFile(xmlDir+"checkOrder.xml")
    public void testDefaultOrder() 
    {
        // TODO 
        // Actual there is no deterministic order of variables.
        // Because of this a passed test is NOT a guarantee for correct implementation.
        // The order of variable computation can be correct by random and may fail on other examples.
        // 
        // Therefore a deterministic default ordering of variables without dependency would be
        // useful. Ideally in the order of definition in the installer.xml with static variables before
        // dynamic variables.

        // Until this, the test is skipped:
        //testOrder("static1", "static2", "static3", "static4", "static5", "dyn1", "dyn2", "dyn3", "dyn4", "dyn5");
    }

    /**
     * Test a simple dependency: a dynamic variable depends on a static variable and a second one standing alone
     */
    @Test
    @InstallFile(xmlDir+"simpleDependency.xml")
    public void testSimpleDependency() 
    {
        testOrder("static1", "dyn1");
        testContained("dyn2");
    }

    /**
     * Test a longer dependency of dynamic variables
     */
    @Test
    @InstallFile(xmlDir+"deeperDependency.xml")
    public void testDeeperDependency() 
    {
        testOrder("static1", "dyn1", "dyn2", "dyn3", "dyn4", "dyn5", "dyn6", "dyn7", "dyn8");
    }

    /**
     * Test a longer dependency of variables in reversed order
     */
    @Test
    @InstallFile(xmlDir+"forwardDependency.xml")
    public void testforwardDependency() 
    {
        testOrder("static1", "dyn8", "dyn7", "dyn6", "dyn5", "dyn4", "dyn3", "dyn2", "dyn1");
    }

    /**
     * Test a longer dependency of variables in mixed order
     */
    @Test
    @InstallFile(xmlDir+"mixedDependency.xml")
    public void testMixedDependency() 
    {
        testOrder("static1", "dyn1", "dyn5", "dyn4", "dyn6", "dyn3", "dyn7", "dyn2", "dyn8");
    }

    /**
     * Test two separate dependency sequences
     */
    @Test
    @InstallFile(xmlDir+"separateDependency.xml")
    public void testSeparateDependency() 
    {
        testOrder("dyn7", "dyn5", "dyn3", "dyn1");
        testOrder("dyn2", "dyn4", "dyn6", "dyn8");
    }

    /**
     * Test two separate dependency sequences merging together
     */
    @Test
    @InstallFile(xmlDir+"parallelDependency.xml")
    public void testParallelDependency() 
    {
        testOrder("dyn7", "dyn5", "dyn3", "dyn1", "dyn10");
        testOrder("dyn2", "dyn4", "dyn6", "dyn8", "dyn10");
    }

    /**
     * Test variables with values with indirect variable references
     */
    @Test
    @InstallFile(xmlDir+"complexValueDependency.xml")
    public void testComplexValueDependency() 
    {
        testOrder("file", "ini"); testOrder("key", "ini"); testOrder("section", "ini");
        testOrder("file", "opt"); testOrder("key", "opt");
        testOrder("file", "xml"); testOrder("key", "xml");

        testOrder("file", "jar_ini"); testOrder("entry", "jar_ini"); testOrder("key", "jar_ini"); testOrder("section", "jar_ini");
        testOrder("file", "jar_opt"); testOrder("entry", "jar_ini"); testOrder("key", "jar_opt");
        testOrder("file", "jar_xml"); testOrder("entry", "jar_ini"); testOrder("key", "jar_xml");

        testOrder("file", "zip_ini"); testOrder("entry", "jar_ini"); testOrder("key", "zip_ini"); testOrder("section", "zip_ini");
        testOrder("file", "zip_opt"); testOrder("entry", "jar_ini"); testOrder("key", "zip_opt");
        testOrder("file", "zip_xml"); testOrder("entry", "jar_ini"); testOrder("key", "zip_xml");

        testOrder("regkey", "reg"); testOrder("regvalue", "reg");

        testOrder("executable", "exec"); testOrder("dir", "exec"); testOrder("arg", "exec");
    }

    private void testOrder(String... names)
    {
        String name1 = names[0];
        String name2 ;
        testContained(name1);
        for (int i = 1; i < names.length; i++) {
            name2 = names[i];
            testContained(name2);
            assertTrue(String.format("'%s' must come before '%s' in variables-list",name1,name2), seachInList(name1) < seachInList(name2));
            name1 = name2;
        }
    }

    private void testContained(String name)
    {
        assertTrue(String.format("variable '%s' must be contained in variables-list",name), seachInList(name)>-1);
    }

    private int seachInList(String name)
    {
        ListIterator<String> it = orderedVarnames.listIterator();
        while (it.hasNext())
        {
            if (it.next().equals(name))
            {
                return it.previousIndex();
            }
        }
        return -1;
    }

}

