package cn.tedu.mall.seckill.timer.config;

import cn.tedu.mall.seckill.timer.job.SeckillInitialJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author sytsnb@gmail.com
 * @date 2022 2022/11/14 12:26
 */
@Configuration
public class QuartzConfig {

    /**
     * 向spring容器中保存JobDetail对象
     *
     * @return
     */
    @Bean
    public JobDetail initJobDetail() {
        return JobBuilder.newJob(SeckillInitialJob.class)
                .withIdentity("initSeckill")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger initSeckillTrigger() {
        // 实际开发要正确的写出所有批次秒杀前5分钟的cron表达式
        // 例如 11:55    13:55     15:55  ......
        // 0 55 11,13,15,17 * * ?
        // 但是学习过程中我们不可能去等这个时间的,所以为了方便测试,我们设置它每分钟都触发一次
        CronScheduleBuilder cron =
                CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("initTrigger")
                .withSchedule(cron)
                .build();
    }

}
