package top.chitucao.summerframework.cache.redis;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.lambdaworks.redis.resource.DefaultClientResources;

import lombok.extern.slf4j.Slf4j;

/**
 * lettuce连接配置
 *
 * @author chitucao
 */
@Slf4j
@Import(RedisProperties.class)
public class LettuceConnectConfiguration {

    @Resource
    private RedisProperties redisProperties;

    @Bean
    public RedisConfigurationRefreshListener redisConfigurationRefresherListener() {
        return new RedisConfigurationRefreshListener();
    }

    @Bean
    public DynamicLettuceConnectionFactory redisConnectionFactory() {
        return new DynamicLettuceConnectionFactory(createLettuceConnectionFactory());
    }

    public LettuceConnectionFactory createLettuceConnectionFactory() {
        Assert.notNull(redisProperties.getAppCode(), "appCode不能为空!");
        Assert.notNull(redisProperties.getEnv(), "env不能为空!");
        Assert.notNull(redisProperties.getGroupName(), "groupName不能为空!");

        RedisConfigUtils.CacheConfig config = RedisConfigUtils.getRedisConfig(redisProperties.getAppCode(), redisProperties.getEnv(), redisProperties.getGroupName());
        if (config == null || CollectionUtils.isEmpty(config.getRedisConfig())) {
            throw new RuntimeException("未拉取到redis配置");
        }

        return buildClusterLettuceConnectionFactory(config);
    }

    /**
    * 集群模式
    * 
    * @param cacheConfig   redis配置
    * @return              lettuce连接工厂
    */
    private LettuceConnectionFactory buildClusterLettuceConnectionFactory(RedisConfigUtils.CacheConfig cacheConfig) {
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        for (RedisConfigUtils.RedisConfig redisConfig : cacheConfig.getRedisConfig()) {
            redisClusterConfiguration.addClusterNode(new RedisNode(redisConfig.getHost(), redisConfig.getPort()));
        }
        EnhancedLettuceConnectionFactory lettuceConnectionFactory = new EnhancedLettuceConnectionFactory(redisClusterConfiguration);

        RedisConfigUtils.RedisConfig redisConfig = cacheConfig.getRedisConfig().iterator().next();

        // 设置超时时间
        lettuceConnectionFactory.setTimeout(redisConfig.getTimeOut() != 0 ? redisConfig.getTimeOut() : 5000);

        // 设置密码
        if (!StringUtils.isEmpty(redisConfig.getPassword())) {
            lettuceConnectionFactory.setPassword(redisConfig.getPassword());
        }

        // 工作线程相关配置
        lettuceConnectionFactory.setClientResources(DefaultClientResources.builder().ioThreadPoolSize(8).computationThreadPoolSize(8).build());

        return lettuceConnectionFactory;
    }
}