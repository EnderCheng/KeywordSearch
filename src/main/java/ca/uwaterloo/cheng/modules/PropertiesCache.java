package ca.uwaterloo.cheng.modules;

import java.io.*;
import java.util.Properties;
import java.util.Set;

public class PropertiesCache
{
    public final static String property_file_name = "app.properties";
    private Properties configProp;

    public PropertiesCache()
    {
        configProp = new Properties();
    }

    public String read(String key) {
        File f = new File(property_file_name);
        if (f.exists()) {
            try {
                FileReader reader = new FileReader(property_file_name);
                Properties p = new Properties();
                p.load(reader);
                return p.getProperty(key);
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void write(String key, String value) {
        File f = new File(property_file_name);
        Properties p = new Properties();
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            else{
                FileReader reader = new FileReader(property_file_name);
                p.load(reader);
            }
            FileWriter writer = new FileWriter(property_file_name);
            p.setProperty(key, value);
            p.store(writer, "Update");
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}