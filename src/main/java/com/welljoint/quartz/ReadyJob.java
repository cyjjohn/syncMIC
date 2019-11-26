package com.welljoint.quartz;

import com.welljoint.utils.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;

//同步执行注解
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class ReadyJob implements Job {
    private Logger logger = LoggerFactory.getLogger(ReadyJob.class);
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        JobKey jobKey = jobDetail.getKey();
        String jobName = jobKey.getName();
        String jobGroup = jobKey.getGroup();
        Date calendar = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdf.format(calendar.getTime());
        logger.info(">>>任务:" + jobName + " 隶属于" + jobGroup + "正在执行,执行时间: "
                + dateStr);
        long startTime = System.currentTimeMillis();
        //开始

        try {
            //配置读取
            Map<String, String> config = ResourceUtil.readProperties("config");
            String micDir = config.get("micDir");
            String wavDir = config.get("wavDir");
            String xmlPath = config.get("templateDir") + config.get("xmlName");
            String csvPath = config.get("templateDir") + config.get("csvName");
            String wavPath = config.get("templateDir") + config.get("wavName");
            String charset = config.get("encode");
            int batchSize = Integer.parseInt(config.get("batchSize"));
//            String userId = config.get("user_id");
            String internalUser = config.get("internal_user_id");
            String switchId = config.get("switch_id");
//            String user_rta_state = config.get("user_rta_state");

            Collection<File> fileCollection = FileUtils.listFiles(
                    new File(wavDir),
                    FileFilterUtils.and(
                            EmptyFileFilter.NOT_EMPTY, // 为空判断
                            new SuffixFileFilter(".0.wav")),
                    FalseFileFilter.FALSE);
            if (fileCollection.isEmpty()) {
                logger.info("!!!Nothing here!!!");
            } else {
                int size=0;
                if (fileCollection.size() % batchSize == 0) {
                    size = fileCollection.size() / batchSize ;
                    logger.info("单批次数量["+batchSize+"],本次处理 {} 文件,分成["+size+"]个文件夹",fileCollection.size());
                }else {
                    size=fileCollection.size() / batchSize + 1;
                    logger.info("单批次数量["+batchSize+"],本次处理 {} 文件,分成["+size+"]个文件夹",fileCollection.size());
                }

                Date date = new Date();
                String dateFmt = DateUtil.format(date, "yyyyMMdd");

                //获取目录下有多少当天日期开头的文件夹,序号会紧接下去
                int dirSerial=0;
                File[] subdirs = new File(micDir).listFiles((FileFilter) DirectoryFileFilter.DIRECTORY);
                if (subdirs != null) {
                    for (File dir : subdirs) {
                        if (dir.getName().contains(dateFmt)) {
                            dirSerial++;
                        }
                    }
                }

                //遍历文件按文件名进行排序,，20190910 update
                Comparator<File> comparator = Comparator.comparing(File::getName);
                List<File> fileList = new ArrayList<File>(fileCollection);
                fileList.sort(comparator);

                //按批次创建文件夹
                for (int i = 0; i < size; i++) {
                    File dir = new File(micDir + dateFmt +"_"+ String.format("%04d",++dirSerial));
                    String dirPath = dir.getPath()+"/";
                    if (dir.mkdirs()) {
                        logger.info(" {} 创建成功",dir.getPath());
                    }else {
                        logger.error(" {} 创建失败,已存在该目录或权限不够",dir.getPath());
                    }
                    int index=0;
                    //文件移动和处理
                    Iterator<File> it = fileList.iterator();
                    while (it.hasNext()) {//因为要记录下标，不用foreach
                        File file = it.next();//录音文件0000000000_2_1_122.nmf.0.wav
                        if (index == batchSize) {
                            continue;
                        } else {
                            index++;
                        }
                        String fileFullName = file.getName();
                        String muteFileName = fileFullName.replace("0.wav", "1.wav");
                        File muteFile = new File(wavDir+muteFileName);
                        logger.info("当前正在处理文件 {} ",fileFullName);
                        String fileName = fileFullName.substring(0, fileFullName.indexOf("."));
                        FileUtils.moveFile(file, new File(dirPath + fileFullName));
                        logger.info("移动wav文件 {} 至 {} ",file.getPath(),dirPath + fileFullName);
                        if (muteFile.exists()) {
                            FileUtils.moveFile(muteFile, new File(dirPath + muteFileName));
                            logger.info("存在1.wav文件,移动文件 {} 至 {} ",muteFile.getPath(),dirPath + muteFileName);
                        } else {
                            FileUtils.copyFile(new File(wavPath), new File(dirPath + muteFileName));
                            logger.info("复制模板静音文件 {} 至 {} ",wavPath,dirPath + muteFileName);
                        }
                        Document doc = null;
                        try {
                            doc = XmlUtil.readDoc(new File(xmlPath));
                        } catch (DocumentException e) {
                            e.printStackTrace();
                        }
                        if (doc!=null) {
                            Element rootElement = doc.getRootElement();
                            String[] wavNameStr= fileName.split("_", -1);//读取wav文件名中的Value数组
                            int wavTime = 0;
                            wavTime = AudioUtil.getSecondLengthForWav(dirPath + fileFullName).intValue();//取录音时长
                            if (wavTime == 0) {
                                if (wavNameStr.length >=4) {
                                    wavTime = Integer.parseInt(wavNameStr[3]);
                                }
                                updateXmlTime(doc,xmlPath,charset,wavTime);
                            }else{
                                updateXmlTime(doc, xmlPath, charset,wavTime);
                            }

                            updateXmlValue(doc, rootElement, xmlPath, charset, "//InitiatorUser", internalUser);
                            updateXmlValue(doc, rootElement, xmlPath, charset, "//SwitchID", switchId);
//                            updateXmlValue(doc, rootElement, xmlPath, charset, "//Participant//Recording[RecordingSideType='Out']/../../UserID", userId);
                            updateXmlValue(doc, rootElement, xmlPath, charset, "//Participant[UserID=0]/Recordings/Recording/Storage/ArchivePath", fileFullName);
                            updateXmlValue(doc, rootElement, xmlPath, charset, "//Participant[UserID!=0]/Recordings/Recording/Storage/ArchivePath", fileFullName.replace("0.wav", "1.wav"));
                            updateXmlValue(doc,rootElement,xmlPath,charset,"//BusinessData[Name='SYSTEMID']/Value",wavNameStr[0]);
                            updateXmlValue(doc,rootElement,xmlPath,charset,"//BusinessData[Name='siAuthenticationLevel']/Value",wavNameStr[1]);

//                            logger.info("InitiatorUser:"+internalUser);
//                            logger.info("SwitchID:"+switchId);
//                            logger.info("UserID:"+userId);
//                            logger.info("ArchivePath:"+fileName);
//                            logger.info("BusinessData1:"+wavNameStr[0]);
//                            logger.info("BusinessData2:"+wavNameStr[1]);

                            //更新ID
                            List<Element> eleList=doc.selectNodes("//ID");
                            String idValue = DateUtil.format(new Date(), "yyyyMMddHHmmssSSS");
                            int cnt=1;
                            for (Element ele:eleList) {
                                if (!ele.getParent().getName().equals("Participant")) {
                                    updateXmlValue(doc, ele, xmlPath, charset, ".", idValue+Integer.toString(cnt));
                                    cnt++;
                                }
                            }
                        }
                        String fileShortName = fileFullName.substring(0, fileFullName.indexOf("."));
                        FileUtils.copyFile(new File(xmlPath), new File(dirPath + fileShortName + ".xml"));
                        logger.info("复制xml文件至 {} ",dirPath + fileShortName + ".xml");
                        it.remove();
                    }

                    FileUtils.copyFile(new File(csvPath), new File(dirPath + "MICUser.csv"));
                    logger.info("在 {} 生成csv文件",dirPath + "MICUser.csv");
                    FileUtils.touch(new File(dirPath + "_Ready"));
                    if (new File(dirPath + "_Ready").exists()) {
                        logger.info("在 {} 生成_Ready标识文件",dirPath);
                    }else{
                        logger.error(" {} _Ready文件生成出错",dirPath);
                    }
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }

        //结束
        long endTime = System.currentTimeMillis();
        logger.info(">>>Done,time used:{}ms", (endTime - startTime));

    }


    public void updateXmlTime (Document doc,String xmlPath, String charset,int wavTime){
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT")); // 设置时区为GMT
        Date date = new Date();
        Element rootElement = doc.getRootElement();
        XmlUtil.setNodesValue(rootElement, "//GMTStartTime", sdf2.format(date));
        XmlUtil.setNodesValue(rootElement, "//GMTEndTime", sdf2.format(DateUtil.offsiteDate(date, Calendar.SECOND, wavTime)));
        try {
            XmlUtil.xmlWriteToFile(doc, xmlPath, charset, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateXmlValue(Document doc,Element rootElement, String xmlPath, String charset, String xpath, String value) {
        XmlUtil.setNodesValue(rootElement, xpath, value);
        try {
            XmlUtil.xmlWriteToFile(doc, xmlPath, charset, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
