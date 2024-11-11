package top.chitucao.summerframework.cache.redis;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import com.ly.tcbase.config.ChangeConfigData;
import com.ly.tcbase.config.ConfigCenterClient;
import com.ly.tcbase.config.ConfigChanged;

import lombok.extern.slf4j.Slf4j;

/**
 * redis配置更新监听
 *
 * @author chitucao
 */
@Slf4j
public class RedisConfigurationRefreshListener implements InitializingBean, ApplicationContextAware {

    private static final String            REDIS_CONFIG_KEY = "TCBase.Cache";

    private final ScheduledExecutorService executorService  = Executors.newSingleThreadScheduledExecutor();

    private ApplicationContext             applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        ConfigCenterClient.onConfigChanged(new ConfigChanged() {
            @Override
            public void configChanged(ChangeConfigData changeInfo) {
                if (isConfigChanged(changeInfo)) {
                    log.info("[RedisConfigurationRefreshListener] Redis config changed. {}", changeInfo);

                    // 创建新的连接
                    LettuceConnectConfiguration configuration = applicationContext.getBean(LettuceConnectConfiguration.class);
                    LettuceConnectionFactory lettuceConnectionFactory = configuration.createLettuceConnectionFactory();

                    // 设置新的连接
                    DynamicLettuceConnectionFactory factory = applicationContext.getBean(DynamicLettuceConnectionFactory.class);
                    LettuceConnectionFactory oldLettuceConnectionFactory = factory.setConnectionFactory(lettuceConnectionFactory);

                    // 释放旧的连接
                    releaseLettuceConnectionFactory(oldLettuceConnectionFactory);
                }
            }
        });
    }

    private void releaseLettuceConnectionFactory(LettuceConnectionFactory factory) {
        LettuceConnectionFactoryReleaseTask task = new LettuceConnectionFactoryReleaseTask(factory, executorService);
        executorService.schedule(task, 3, TimeUnit.SECONDS);
    }

    /**
     * 配置是否更新
     *
     * @param data  更新数据
     * @return      配置是否更新
     */
    private boolean isConfigChanged(ChangeConfigData data) {
        return data.getAddList().stream().anyMatch(p -> REDIS_CONFIG_KEY.equals(p.getKey())) //
               || data.getUpdateList().stream().anyMatch(p -> REDIS_CONFIG_KEY.equals(p.getKey())) //
               || data.getRemoveList().stream().anyMatch(p -> REDIS_CONFIG_KEY.equals(p.getKey())); //
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
