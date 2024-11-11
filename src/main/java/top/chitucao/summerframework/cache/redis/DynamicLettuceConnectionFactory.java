package top.chitucao.summerframework.cache.redis;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * lettuce动态连接工厂
 * -1.主要是解决redis配置更新的场景
 */
@Slf4j
public class DynamicLettuceConnectionFactory implements RedisConnectionFactory, InitializingBean, DisposableBean {

    private final AtomicReference<LettuceConnectionFactory> ref;

    public DynamicLettuceConnectionFactory(LettuceConnectionFactory connectionFactory) {
        this.ref = new AtomicReference<>(connectionFactory);
    }

    public RedisConnectionFactory getConnectionFactory() {
        return ref.get();
    }

    public LettuceConnectionFactory setConnectionFactory(LettuceConnectionFactory newConnectionFactory) {
        newConnectionFactory.afterPropertiesSet();
        return ref.getAndSet(newConnectionFactory);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ref.get().afterPropertiesSet();
        log.info("[DynamicLettuceConnectionFactory] Initialized. {}", ref.get());
    }

    @Override
    public void destroy() throws Exception {
        ref.get().destroy();
        log.info("[DynamicLettuceConnectionFactory] Destroyed. {}", ref.get());
    }

    @Override
    public RedisConnection getConnection() {
        return ref.get().getConnection();
    }

    @Override
    public RedisClusterConnection getClusterConnection() {
        return ref.get().getClusterConnection();
    }

    @Override
    public boolean getConvertPipelineAndTxResults() {
        return ref.get().getConvertPipelineAndTxResults();
    }

    @Override
    public RedisSentinelConnection getSentinelConnection() {
        return ref.get().getSentinelConnection();
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return ref.get().translateExceptionIfPossible(ex);
    }
}
