package com.gulab.sigkillserver.config.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class GameTaskSchedulerConfig {

    private static final int GAME_TASK_POOL_SIZE = 4;
    private static final String GAME_TASK_THREAD_PREFIX = "game-task-";

    @Bean(name = "gameTaskScheduler")
    public TaskScheduler gameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(GAME_TASK_POOL_SIZE);
        scheduler.setThreadNamePrefix(GAME_TASK_THREAD_PREFIX);
        scheduler.initialize();
        return scheduler;
    }
}
