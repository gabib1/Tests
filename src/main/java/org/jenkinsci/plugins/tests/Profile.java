/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gavrielk
 */
public abstract class Profile
{

    String name;
    File profileFile;

//    public Profile(String name)
//    {
//        setName(name);
//        this.tests = new ArrayList<ITest>();
//    }
    public Profile(File profileFile) throws IOException
    {
        this.profileFile = profileFile;
        if (this.profileFile.exists() == false)
        {
            this.profileFile.createNewFile();
        }
        setName(this.profileFile.getName().substring(0, this.profileFile.getName().indexOf(".profile")));

    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void addTest(ITest test) throws IOException
    {
        try (FileWriter out = new FileWriter(this.profileFile, true))
        {
            out.write(test.getGroup() + "|" + test.getName() + "\n");
            out.close();
        }
    }

    public void removeAllTests()
    {
        try (PrintWriter writer = new PrintWriter(this.profileFile))
        {
            writer.print("");
            writer.close();
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removeFromDB()
    {
        this.profileFile.delete();
    }

    /**
     * Note: this method uses the file that is held as a data member and passed
     * in the constructor
     *
     * @return List of names of all tests this profile holds
     */
    public ArrayList<String> getTestsNameList()
    {
        System.out.println("^^^^^^^^^       getTestsNameList()     ^^^^^^^^^^^^^");
        ArrayList<String> testsList = new ArrayList<>();
        InputStream fis;
        BufferedReader br;
        String line;

        try
        {
            fis = new FileInputStream(this.profileFile);
            br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
            while ((line = br.readLine()) != null)
            {
                System.out.println("line---------:       " + line);
                String testName = line.substring(line.indexOf('|') + 1);
                System.out.println("testName   :       " + testName);

                testsList.add(testName);
            }

            br.close();

        } catch (FileNotFoundException | ArrayIndexOutOfBoundsException | NullPointerException ex)
        {
            System.out.println("File not found");
//            Logger.getLogger(ProjectConfiguration.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex)
        {
            Logger.getLogger(Profile.class.getName()).log(Level.SEVERE, null, ex);
        }

        return testsList;
    }
}
