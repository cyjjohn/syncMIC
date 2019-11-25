package com.welljoint;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class App
{
    public static void main( String[] args )
    {
        Logger logger = LoggerFactory.getLogger(App.class);
        try {
            logger.info("----- App start -----");
            //创建Scheduler
            Scheduler scheduler=StdSchedulerFactory.getDefaultScheduler();
            //调度执行
            scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
