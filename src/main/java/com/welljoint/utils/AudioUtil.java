package com.welljoint.utils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class AudioUtil {
    /**
     * 获取音频文件时长
     *
     * @param wavFilePath wav文件路径，支持本地和网络HTTP路径
     * @return 时长/微秒，可 /1000000D 得到秒
     * @throws Exception
     */
    public static long getMicrosecondLengthForWav(String wavFilePath) throws Exception {
        if (wavFilePath == null || wavFilePath.length() == 0) {
            return 0;
        }
        String bath = wavFilePath.split(":")[0];

        Clip clip = AudioSystem.getClip();
        AudioInputStream ais;
        if ("http".equals(bath.toLowerCase())||"https".equals(bath.toLowerCase())) {
            ais = AudioSystem.getAudioInputStream(new URL(wavFilePath));
        } else {
            ais = AudioSystem.getAudioInputStream(new File(wavFilePath));
        }
        clip.open(ais);
        return clip.getMicrosecondLength();
    }

    public static Double getSecondLengthForWav(String wavFilePath) {
        try {
            return getMicrosecondLengthForWav(wavFilePath)/1000000D;
        } catch (Exception e) {
            e.printStackTrace();
            return 0D;
        }
    }

    public static void main(String[] args) {
        File file = new File(FileUtil.fixPath("E:\\19年6月\\Goldwave\\B.wav"));

    }
}
