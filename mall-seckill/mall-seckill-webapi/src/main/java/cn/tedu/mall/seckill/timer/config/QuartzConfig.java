package cn.tedu.mall.seckill.timer.config;

import cn.tedu.mall.seckill.timer.job.SeckillInitialJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // 向Spring容器中保存JobDetail对象
    @Bean
    public JobDetail initJobDetail(){
        return JobBuilder.newJob(SeckillInitialJob.class)
                .withIdentity("initSeckill")
                .storeDurably()
                .build();
    }
    // 向Spring容器中保存Trigger对象
    @Bean
    public Trigger initSeckillTrigger(){
        // 实际开发要写出正确的Cron表达式,让程序在11:55  13:55....运行
        // 0 55 11,13,15,17 * * ?
        // 但是学习过程中我们不可能去等这个时间,所有为了测试方便,我们设置它每分钟都运行
        CronScheduleBuilder cron=
                CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("initTrigger")
                .withSchedule(cron)
                .build();
    }




}
