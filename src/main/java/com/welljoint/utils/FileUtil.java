package com.welljoint.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;

public class FileUtil {
    /** The Unix separator character. */
    private static final char UNIX_SEPARATOR = '/';

    /** The Windows separator character. */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * 创建文件，如果这个文件存在，直接返回这个文件
     * @param fullFilePath 文件的全路径，使用POSIX风格
     * @return 文件，若路径为null，返回null
     * @throws IOException
     */
    public static File touch(String fullFilePath) throws IOException {
        if(fullFilePath == null) {
            return null;
        }
        File file = new File(fullFilePath);

        file.getParentFile().mkdirs();
        if(!file.exists()) file.createNewFile();
        return file;
    }

    /**
     * 获得一个带缓存的写入对象
     * @param path 输出路径，绝对路径
     * @param charset 字符集
     * @param isAppend 是否追加
     * @return BufferedReader对象
     * @throws IOException
     */
    public static BufferedWriter getBufferedWriter(String path, String charset, boolean isAppend) throws IOException {
        return new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(touch(path), isAppend), charset
                )
        );
    }

    public static String fixPath(String path) {
        if(path!=null){
            path=path.replaceAll("//", "/");
            path=path.replaceAll("\\\\", "/");
            if(!path.substring(path.length()-1,path.length()).equals("/")){
                path=path+"/";
            }
        }
        return path;
    }
}
