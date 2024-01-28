package com.abort;

import java.io.*;
import java.util.Properties;

public class ReadWriteFileUtil {

    public static String getProperty(String Property,String filePath) {
        try {
            Properties prop =loadProperties(filePath);
            return prop.getProperty(Property);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Properties loadProperties(String resourceName) throws IOException {
        Properties properties = null;
        InputStream inputStream = null;
        try {
            inputStream =  loadResource(resourceName);;
            if (inputStream != null) {
                properties = new Properties();
                properties.load(inputStream);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return properties;
    }

    public static InputStream loadResource(String resourceName) throws IOException {
        ClassLoader classLoader = ReadWriteFileUtil.class.getClassLoader();
        InputStream inputStream = null;
        if (classLoader != null) {
            inputStream = classLoader.getResourceAsStream(resourceName);
        }
        if (inputStream == null) {
            classLoader = ClassLoader.getSystemClassLoader();
            if (classLoader != null) {
                inputStream = classLoader.getResourceAsStream(resourceName);
            }
        }
        if (inputStream == null) {
            File file = new File(resourceName);
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            }
        }
        return inputStream;
    }

    /**
     This method is helpful in reading fromfile
     **/
    public static  String readFromFile(String filePath){
        System.out.println("File path is : "+filePath);
        String textToReturn = "";
        BufferedReader br = null;
        try {
            File file = new File(filePath);
            br = new BufferedReader(new FileReader(file));
            textToReturn = br.readLine().toString();
            String line = null;
            while ((line = br.readLine()) != null) {
                textToReturn = textToReturn+","+line;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return textToReturn;
    }
}

