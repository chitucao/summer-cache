package top.chitucao.summerframework.cache.redis;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ReflectionUtils;

import com.lambdaworks.redis.SocketOptions;
import com.lambdaworks.redis.cluster.ClusterClientOptions;
import com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions;
import com.lambdaworks.redis.cluster.RedisClusterClient;

/**
 * lettuce连接工厂增强，主要是为了增强设置，设置拓扑刷新、tcpNoDelay
 *
 * @author chitucao
 */
public class EnhancedLettuceConnectionFactory extends LettuceConnectionFactory {

    public EnhancedLettuceConnectionFactory(RedisClusterConfiguration clusterConfig) {
        super(clusterConfig);
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        Field clientField = ReflectionUtils.findField(this.getClass(), "client");
        ReflectionUtils.makeAccessible(clientField);
        RedisClusterClient client = (RedisClusterClient) ReflectionUtils.getField(clientField, this);

        client.setOptions(ClusterClientOptions.builder() //
            .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder() //
                .enablePeriodicRefresh(15, TimeUnit.SECONDS) //
                .enableAdaptiveRefreshTrigger(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT, ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
                .adaptiveRefreshTriggersTimeout(10, TimeUnit.SECONDS) //
                .build()) //
            .socketOptions(SocketOptions.builder() //
                .tcpNoDelay(true) //
                .keepAlive(true) //
                .build())
            .autoReconnect(true) //
            .build());
    }
}