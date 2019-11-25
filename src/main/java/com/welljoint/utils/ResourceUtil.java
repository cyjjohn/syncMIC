package com.welljoint.utils;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;

public class ResourceUtil {

    public static HashMap<String,String> readProperties(String propertiesName) {
        HashMap<String,String> map = new HashMap<String,String>();
        ResourceBundle rb = ResourceBundle.getBundle(propertiesName);//文件名称会自动加上.properties后缀
        Enumeration<String> enu = rb.getKeys();
        while (enu.hasMoreElements()) {
            String key = enu.nextElement();
            String value = rb.getString(key);
            map.put(key, value);
        }
        return map;
    }

    /**
     * 读取配置文件中的对应某key的value
     *
     * @param propertiesName 文件名称
     * @param key 键名
     * @return String
     */
    public static String getValue(String propertiesName,String key){
        ResourceBundle rb = ResourceBundle.getBundle(propertiesName);
        return rb.getString(key);
    }
}