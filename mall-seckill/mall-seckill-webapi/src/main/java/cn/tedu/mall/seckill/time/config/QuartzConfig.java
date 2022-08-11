package cn.tedu.mall.seckill.time.config;

import cn.tedu.mall.seckill.time.job.SeckillInitialJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QuartzConfig {

    // 定义JobDetail
    @Bean
    public JobDetail initJobDetail(){
        log.info("预热任务绑定");
        return JobBuilder.newJob(SeckillInitialJob.class)
                .withIdentity("initialSeckill")
                .storeDurably()
                .build();
    }
    // 定义触发器Trigger
    @Bean
    public Trigger initSeckillTrigger(){
        log.info("预热触发器运行");
        // 学习过程中,我们定义的Cron设置每分钟运行一次
        CronScheduleBuilder cron=
                CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("initialTrigger")
                .withSchedule(cron)
                .build();
    }

}
