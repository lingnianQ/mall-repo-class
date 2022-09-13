package cn.tedu.mall.seckill.timer.config;

import cn.tedu.mall.seckill.timer.job.SeckillBloomInitialJob;
import cn.tedu.mall.seckill.timer.job.SeckillInitialJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    // 向Spring容器中保存缓存预热的JobDetail
    @Bean
    public JobDetail initJobDetail(){
        return JobBuilder.newJob(SeckillInitialJob.class)
                .withIdentity("initialSeckill")
                .storeDurably()
                .build();
    }
    // 向Spring容器中保存触发器,在设置的时机触发运行缓存预热
    @Bean
    public Trigger initSeckillTrigger(){
        // 实际开发设置好秒杀计划后,要指定秒杀开始前5分钟的时间
        // 但是学习过程中,不可能等待那个时机,所以仍然使用每分钟运行
        CronScheduleBuilder cron=
                CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("initialTrigger")
                .withSchedule(cron)
                .build();
    }

    // 布隆过滤器的加载
    @Bean
    public JobDetail seckillBloomJobDetail(){
        return JobBuilder.newJob(SeckillBloomInitialJob.class)
                .withIdentity("SeckillBloom")
                .storeDurably()
                .build();
    }
    @Bean
    public Trigger seckillBloomTrigger(){
        return TriggerBuilder.newTrigger()
                .forJob(seckillBloomJobDetail())
                .withIdentity("SeckillBloomTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/1 * * * ?"))
                .build();
    }




}
