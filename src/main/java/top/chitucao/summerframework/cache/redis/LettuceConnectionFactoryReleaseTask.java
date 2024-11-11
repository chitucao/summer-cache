package top.chitucao.summerframework.cache.redis;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.ClusterCommandExecutor;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ReflectionUtils;

import io.lettuce.core.AbstractRedisClient;
import lombok.extern.slf4j.Slf4j;

/**
 * lettuce连接工厂释放任务
 * -1.默认的destroy方法，释放连接的过程异常会被catch，所以这里将每个步骤拆开并在异常时执行重试
 *
 * @author chitucao
 */
@Slf4j
public class LettuceConnectionFactoryReleaseTask implements Runnable {

    private final LettuceConnectionFactory lettuceConnectionFactory;

    private final ScheduledExecutorService scheduledExecutorService;

    /** 最大重试释放次数 */
    private static final int               MAX_RETRY_TIMES          = 10;

    /** 重试释放间隔 */
    private static final int               GAP_IN_MILLISECONDS      = 5000;

    /** 延迟执行时间 */
    private int                            retryDelayInMilliSeconds = 5000;

    /** 释放次数计数 */
    private int                            retry                    = 0;

    public LettuceConnectionFactoryReleaseTask(LettuceConnectionFactory lettuceConnectionFactory, ScheduledExecutorService scheduledExecutorService) {
        this.lettuceConnectionFactory = lettuceConnectionFactory;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public void run() {
        if (!release(lettuceConnectionFactory)) {
            if (retry < MAX_RETRY_TIMES) {
                retryDelayInMilliSeconds += GAP_IN_MILLISECONDS;
                scheduledExecutorService.schedule(this, retryDelayInMilliSeconds, TimeUnit.MILLISECONDS);
            } else {
                log.warn("[LettuceConnectionFactoryReleaseTask] Releasing retry time has reached max. {} release failed. force to release lettuce connection.",
                    lettuceConnectionFactory);
                // 最后再尝试一次
                lettuceConnectionFactory.destroy();
            }
        }
    }

    private boolean release(LettuceConnectionFactory lettuceConnectionFactory) {
        try {
            lettuceConnectionFactory.resetConnection();
            releaseClient(lettuceConnectionFactory);
            releaseClusterCommandExecutor(lettuceConnectionFactory);
            log.info("[LettuceConnectionFactoryReleaseTask] Release lettuce connection factory error successfully! {}", lettuceConnectionFactory);
        } catch (Exception e) {
            log.error("[LettuceConnectionFactoryReleaseTask] Release lettuce connection factory error.", e);
            return false;
        } finally {
            retry++;
        }
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean releaseClient(LettuceConnectionFactory lettuceConnectionFactory) {
        Field clientField = ReflectionUtils.findField(lettuceConnectionFactory.getClass(), "client");
        ReflectionUtils.makeAccessible(clientField);
        AbstractRedisClient client = (AbstractRedisClient) ReflectionUtils.getField(clientField, lettuceConnectionFactory);
        client.shutdown(lettuceConnectionFactory.getShutdownTimeout(), lettuceConnectionFactory.getShutdownTimeout(), TimeUnit.MILLISECONDS);
        return true;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean releaseClusterCommandExecutor(LettuceConnectionFactory lettuceConnectionFactory) throws Exception {
        Field clusterCommandExecutorField = ReflectionUtils.findField(lettuceConnectionFactory.getClass(), "clusterCommandExecutor");
        ReflectionUtils.makeAccessible(clusterCommandExecutorField);
        ClusterCommandExecutor clusterCommandExecutor = (ClusterCommandExecutor) ReflectionUtils.getField(clusterCommandExecutorField, lettuceConnectionFactory);
        if (clusterCommandExecutor != null) {
            clusterCommandExecutor.destroy();
        }
        return true;
    }
}
