/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jenkinsci.plugins.tests.ATT;

import org.jenkinsci.plugins.tests.ITest;

/**
 *
 * @author oreny
 */
public class OtherTest implements ITest
{
    String name;


    public OtherTest(String name)
    {
        this.name = name;

    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getGroup() {
        return "BashTest";
    }
        
}
