package com.welljoint.utils;

public class StringUtil extends org.apache.commons.lang3.StringUtils{
    /**
     * 转换为Double类型
     */
    public static Double toDouble(Object val){
        if (val == null){
            return 60D;//defaultValue
        }
        try {
            return Double.valueOf(trim(val.toString()));
        } catch (Exception e) {
            return 60D;
        }
    }

    /**
     * 转换为Float类型
     */
    public static Float toFloat(Object val){
        return toDouble(val).floatValue();
    }

    /**
     * 转换为Long类型
     */
    public static Long toLong(Object val){
        return toDouble(val).longValue();
    }

    /**
     * 转换为Integer类型
     */
    public static Integer toInteger(Object val){
        return toLong(val).intValue();
    }

}
