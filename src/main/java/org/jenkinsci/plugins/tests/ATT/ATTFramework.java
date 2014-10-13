/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.tests.ATT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.jenkinsci.plugins.tests.ATT.ATTRepository.TestSuite.Test;
import org.jenkinsci.plugins.tests.Group;
import org.jenkinsci.plugins.tests.ITest;
import org.jenkinsci.plugins.tests.ITestTool;

/**
 *
 * @author gavrielk
 */
public class ATTFramework implements ITestTool
{

    String testSuitePath = "";
    String testsRepositoryXMLPath = "";
    static String updateTestsListScript = "";
    static final File ATTFrameworkLog = new File("/var/log/jenkins/ATT.log");
    String name;
    ArrayList<ITest> tests = new ArrayList<>();
    ArrayList<Group> groups = new ArrayList<>();
    JAXBContext jaxbContext;
    String username;

    public ATTFramework() throws JAXBException
    {
        username = System.getProperty("user.name");
        this.testSuitePath = "/home/" + username + "/BuildSystem/cc-views/" + username + "_ATT_1.1_int/vobs/HostTool_AT/PyATT/Test_Suite";
        this.testsRepositoryXMLPath = this.testSuitePath + "/tests_repository.xml";
        ATTFramework.updateTestsListScript = this.testSuitePath + "/update_tests_list.sh";
        if (ATTFramework.ATTFrameworkLog.exists() == false)
        {
            try
            {
                ATTFramework.ATTFrameworkLog.createNewFile();
            } catch (IOException ex)
            {
                System.out.println("Log file (" + ATTFramework.ATTFrameworkLog.getAbsolutePath() + ") couldn't be created");
                Logger.getLogger(ATTFramework.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ClassLoader cl = org.jenkinsci.plugins.tests.ATT.ObjectFactory.class.getClassLoader();
        this.jaxbContext = JAXBContext.newInstance("org.jenkinsci.plugins.tests.ATT", cl);
    }

    public String getName()
    {
        return "ATT";
    }

    @Override
    public ArrayList<ITest> getTests() throws JAXBException
    {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setEventHandler(new ATTRepositoryValidationEventHandler());
        File testsRepositoryFile = new File(testsRepositoryXMLPath);
        Source source = new StreamSource(testsRepositoryFile);
        JAXBElement<ATTRepository> AttRepositoryElement = (JAXBElement<ATTRepository>) unmarshaller.unmarshal(source, ATTRepository.class);

        for (Test test : AttRepositoryElement.getValue().testSuite.getTest())
        {
            ATTTest attTest = new ATTTest(test.getName(), test.getPackage(), test.getModule(), test.getDescription());
            this.tests.add(attTest);
        }

        return this.tests;
    }

    //Oren
    public ArrayList<ITest> getOtherTests()
    {
        System.out.println("");
        System.out.println("In getOtherTests");
        String otherTestDir = "/home/" + this.username + "/BuildSystem/cc-views/" + username + "_Other-tests_int/vobs/test scripts/";
        System.out.println("otherTestDir : " + otherTestDir);
        File file = new File(otherTestDir);
        File[] listFiles = file.listFiles();
        System.out.println("File list ");

        for (File test : listFiles)
        {
            String testName = test.getName();
            int pointIndex = testName.indexOf(".");
            if (pointIndex != -1)
            {
                testName = testName.substring(0 , pointIndex);
            }

            System.out.println("test name : " + testName);
            OtherTest otherTest = new OtherTest(testName);
            this.tests.add(otherTest);
        }

        return this.tests;
    }

    public ArrayList<Group> getTestsByGroups() throws JAXBException
    {
        if (this.tests.isEmpty() == true)
        {
            getTests();
        }
        sortTestsToGroups();

        return this.groups;
    }

    private void sortTestsToGroups()
    {

        for (ITest test : this.tests)
        {
            boolean addedCurrTest = false;
            for (Group group : this.groups)
            {
                if (group.getName().equals(test.getGroup()) == true)
                {
                    addedCurrTest = group.add(test);
                }
            }
            if (addedCurrTest == false)
            {
                Group group = new Group(test.getGroup());
                group.add(test);
                this.groups.add(group);
            }
        }
    }

    public static synchronized int updateTestsList() throws IOException, InterruptedException
    {
        // Redirecting the output to the ATT.log file
        if (new File(ATTFramework.updateTestsListScript).exists() == true)
        {
            ProcessBuilder.Redirect redirect = ProcessBuilder.Redirect.to(ATTFramework.ATTFrameworkLog);
            ArrayList<String> command = new ArrayList<String>();
            command.add(ATTFramework.updateTestsListScript);
            System.out.println("Update command: " + command.get(0));
            return executeScript(command, redirect);
        } else
        {
            System.out.println("[ERROR] update_tests_list.sh not found under " + ATTFramework.updateTestsListScript);
            return 1;
        }
    }

    private static int executeScript(ArrayList<String> command, ProcessBuilder.Redirect redirect) throws IOException, InterruptedException
    {
        ProcessBuilder pb = new ProcessBuilder().inheritIO();
        pb.redirectErrorStream(true);

        pb.redirectOutput(redirect);
        return pb.command(command).start().waitFor();
    }

    public class ATTRepositoryValidationEventHandler implements ValidationEventHandler
    {

        @Override
        public boolean handleEvent(ValidationEvent ve)
        {
            if (ve.getSeverity() == ValidationEvent.FATAL_ERROR
                    || ve.getSeverity() == ValidationEvent.ERROR)
            {
                ValidationEventLocator locator = ve.getLocator();
                //Print message from valdation event
                System.out.println("Invalid booking document: "
                        + locator.getURL());
                System.out.println("Error: " + ve.getMessage());
                //Output line and column number
                System.out.println("Error at column "
                        + locator.getColumnNumber()
                        + ", line "
                        + locator.getLineNumber());
            }
            return true;
        }
    }
}
