package org.jenkinsci.plugins.tests;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.RootAction;
import hudson.security.Permission;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.types.resources.Files;
import org.jenkinsci.plugins.tests.ATT.ATTFramework;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

@Extension
public class TestsManager implements RootAction, Describable<TestsManager>
{

    public static String currentProfilesDBPath;
    public ArrayList<ITest> testRepository;
    public ArrayList<Profile> profiles;
    public String currentChosenProfile = "";
    private final String username;
    private final String otherPath;
    private final String ATTPath;
    private final String oldProfilesPath;

    public TestsManager()
    {
        username = System.getProperty("user.name");
        currentProfilesDBPath = "/home/" + username + "/profilesDB";
        otherPath = "/home/" + username + "/profilesDB/otherProfiles";
        ATTPath = "/home/" + username + "/profilesDB/profiles";
        oldProfilesPath = "/home/" + username + "/profilesDB";
        this.profiles = new ArrayList<Profile>();

    }

    @Override
    public String getIconFileName()
    {
        if (CheckBuildPermissions() == true)
        {
            return "/plugin/Tests/tests-icon.png";
        } else
        {
            return null;
        }
    }

    @Override
    public String getDisplayName()
    {
        if (CheckBuildPermissions() == true)
        {
            return "Tests";
        } else
        {
            return null;
        }
    }

    @Override
    public String getUrlName()
    {
        if (CheckBuildPermissions() == true)
        {
            return "Tests";
        } else
        {
            return null;
        }
    }

    private boolean CheckBuildPermissions()
    {
        for (Permission permission : Permission.getAll())
        {
            if (permission.name.equals("Build") == true)
            {
                if (Jenkins.getInstance().hasPermission(permission) == true)
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Descriptor getDescriptor()
    {
        return (TestsDescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public ArrayList<Profile> getOtherProfiles()
    {
        currentProfilesDBPath = otherPath;

        return getProfiles();
    }

    public ArrayList<Profile> getATTProfiles()
    {
        currentProfilesDBPath = ATTPath;

        return getProfiles();
    }

    public ArrayList<Profile> getProfiles()
    {
        System.out.println("--------           getProfiles()    -------");
        System.out.println("Path : " + currentProfilesDBPath);

        File profileDBDir = null;
        profileDBDir = new File(currentProfilesDBPath);

        if (profileDBDir.isDirectory() == true)
        {
            this.profiles = new ArrayList<Profile>();
            File[] profileFiles = profileDBDir.listFiles(new ProfilesFileFilter());
            for (File profileFile : profileFiles)
            {
                try
                {
                    Profile profile = new Profile(profileFile)
                    {
                    };
                    ArrayList<String> testsNameList = profile.getTestsNameList();
                    //  profile.removeAllTests();
                    for (String testName : testsNameList)
                    {
                        ITest test = null;
                        if (this.testRepository == null)
                        {
                            getTests();
                        }

                        // will check if the test wasn't removed from the test repository of the bash tests
                        if (test == null)
                        {
                            for (ITest testIt : this.testRepository)
                            {
                                if (testIt.getName().equals(testName))
                                {
                                    test = testIt;
                                    break;
                                }
                            }
                        }

                        if (test == null)
                        {
                            System.out.println("[ERROR] Test " + testName + " is listed in the profile " + profile.getName() + " but doesn't exist in the tests repository");
                        }
                    }

                    this.profiles.add(profile);
                } catch (IOException ex)
                {
                    System.out.println("[ERROR] Creating new profile for file: " + profileFile + " failed");
                    Logger.getLogger(TestsManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else
        {
            System.out.println("[ERROR] Profiles DB doesn't exist under " + profileDBDir.getAbsolutePath());
        }

        return this.profiles;
    }

    //Oren
    public ArrayList<Group> getTests()
    {
        ArrayList<Group> groupsList = null;
        try
        {
            ATTFramework attFramework = new ATTFramework();
            this.testRepository = attFramework.getTests();
            groupsList = attFramework.getTestsByGroups();

        } catch (JAXBException ex)
        {
            Logger.getLogger(TestsManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return groupsList;
    }

    //Oren
    public ArrayList<ITest> getOtherTests()
    {
        System.out.println("In   getOtherTests");

        ArrayList<Group> groupsList = null;
        try
        {
            ATTFramework attFramework = new ATTFramework();
            this.testRepository = attFramework.getOtherTests();

        } catch (JAXBException ex)
        {
            Logger.getLogger(TestsManager.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return testRepository;
    }

    public String getChosenProfile()
    {
        System.out.println("In getChosenProfile()");
        String chosenProfile = this.currentChosenProfile;
        this.currentChosenProfile = "";
        return chosenProfile;

    }

    @JavaScriptMethod
    public ArrayList<String> doGetTestsInProfile(String profileName)
    {
        Profile profile = null;
        System.out.println("in doGetTestsInProfile");
        if (profileName != null)
        {
            for (Profile profileIt : this.profiles)
            {
                if (profileName.equals(profileIt.getName()))
                {
                    profile = profileIt;
                }
            }
            if (profile != null)
            {
//                Gson gson;
                for (String test : profile.getTestsNameList())
                {
                    System.out.println(test);
                }
                System.out.println("returen NOT NULL");
                return profile.getTestsNameList();
            } else
            {
                System.out.println("Profile " + profileName + " not found");
            }
        } else
        {
            System.out.println("Profile name is empty");
        }
        return null;

    }

    @Extension
    public static final class TestsDescriptorImpl extends TestsConfigDescriptor
    {
    }

    private class ProfilesFileFilter implements FileFilter
    {

        @Override
        public boolean accept(File pathname)
        {
            return pathname.getName().endsWith(".profile");
        }
    }

    private boolean isBlank(String[] variables)
    {
        for (String variable : variables)
        {
            if (variable == null || variable.isEmpty() == true)
            {
                return true;
            }
        }
        return false;
    }

    private Profile getProfileByName(String profileName)
    {
        for (Profile p : this.profiles)
        {
            if (p.getName().equals((profileName)) == true)
            {
                return p;
            }
        }
        return null;
    }

    private ITest getTestByName(String testName)
    {
        for (ITest t : this.testRepository)
        {
            if (t.getName().equals((testName)) == true)
            {
                return t;
            }
        }
        return null;
    }

    // Set a new Corntab entry using TimerTrigger class
    public void doSubmit(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException
    {
        System.out.println("in doSubmit");
        Map<String, String> formOptionsMap = new HashMap();
        String formOption = null;
        formOptionsMap.put("profile-add-submit", "add");
        formOptionsMap.put("profile-remove-submit", "remove");
        formOptionsMap.put("profile-save-submit", "save");
        formOptionsMap.put("update-tests-list", "update");

        for (Map.Entry<String, String> entry : formOptionsMap.entrySet())
        {
            System.out.println(entry.getKey() + "/" + entry.getValue());
            formOption = ((req.getParameter(entry.getKey()) == null) ? null : entry.getValue());
            System.out.println("formOption ------  " + formOption);
            if (formOption != null)
            {
                break;
            }
        }

        String profileDIrPath = null;
        String profileName = req.getParameter("profile");
        switch (formOption)
        {
            case "add":

                String newProfileName = req.getParameter("profile-add-name");
                if (newProfileName != null && (!newProfileName.isEmpty()))
                {
                    String pathToNewProfileFile = currentProfilesDBPath + "/" + newProfileName + ".profile";
                    try
                    {
                        Profile profile = new Profile(new File(pathToNewProfileFile))
                        {
                        };
                        this.profiles.add(profile);
                    } catch (IOException ex)
                    {
                        System.out.println("[ERROR] Failed to create profile " + newProfileName + ", profile's file path is " + pathToNewProfileFile);
                        Logger.getLogger(TestsManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                break;

            case "remove":
                System.out.println("----------------case: remove----------------");

                if (profileName != null)
                {
                    Profile profile = getProfileByName(profileName);
                    if (profile != null)
                    {
                        profile.removeFromDB();
                        this.profiles.remove(profile);
                    }
                    this.currentChosenProfile = profileName;
                }
                break;

            case "save":

                System.out.println("----------------case: saveProfileSubmit----------------");

                if (profileName != null)
                {
                    Profile profile = getProfileByName(profileName);
                    profile.removeAllTests();
                    Object key;
                    Iterator keySetIt = req.getParameterMap().keySet().iterator();
                    while (keySetIt.hasNext())
                    {
                        key = keySetIt.next();
                        System.out.println("key :" + key);
                        if (key.toString().startsWith("test_"))
                        {
                            String testName = req.getParameter(key.toString()).replaceFirst("test_", "");
                            System.out.println("testName=" + testName);
                            if (testName != null)
                            {
                                try
                                {
                                    profile.addTest(getTestByName(testName));
                                } catch (IOException ex)
                                {
                                    System.out.println("[ERROR] Failed to add test: " + testName + ", to " + profileName + " profile");
                                    Logger.getLogger(TestsManager.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }
                    this.currentChosenProfile = profileName;
                    break;
                }

            case "update":

                try
                {
                    int exitCode = ATTFramework.updateTestsList();
                    if (exitCode != 0)
                    {
                        throw new IOException("[ERROR] updateTestsList return a non zero exit code: " + exitCode);
                    }
                } catch (InterruptedException | IOException ex)
                {
                    System.out.println("[ERROR] Failed to update the tests list");
                    Logger.getLogger(TestsManager.class.getName()).log(Level.SEVERE, null, ex);
                }
        }

        // to keep compatability to the old scripts will copy the file to the old profiles
        //will be deleted when when all change their baselines
        if ((!formOption.equals("update")) && (currentProfilesDBPath == ATTPath))
        {
            System.out.println("---------------------------------");

            // on every change will delete all the old files and copy the new ones
            File oldDB = new File(oldProfilesPath);
            String[] list = oldDB.list();
            for (String fileName : list)
            {
                File file = new File(oldProfilesPath + "/" + fileName);
                if (file.isFile())
                {
                    file.delete();
                }
            }

            File attNewProfiles = new File(ATTPath);
            list = attNewProfiles.list();
            for (String fileName : list)
            {
                File srcFile = new File(ATTPath + "/" + fileName);
                File destFile = new File(oldProfilesPath + "/" + srcFile.getName());
                FileUtils.copyFile(srcFile, destFile);
            }
        }
        
        // should be deleted all to this line

        rsp.sendRedirect2(req.getReferer());
    }

}
