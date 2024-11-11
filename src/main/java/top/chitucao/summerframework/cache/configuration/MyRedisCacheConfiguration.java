package top.chitucao.summerframework.cache.configuration;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
import top.chitucao.summerframework.cache.CacheType;
import top.chitucao.summerframework.cache.annotation.CacheConfigOptions;
import top.chitucao.summerframework.cache.redis.LettuceConnectConfiguration;

/**
 * redis缓存配置
 *
 * @author chitucao
 */
@Slf4j
@Import({ LettuceConnectConfiguration.class })
public class MyRedisCacheConfiguration extends AbstractCacheConfiguration {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory, ResourceLoader resourceLoader) {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer(resourceLoader.getClassLoader()));
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new JdkSerializationRedisSerializer(resourceLoader.getClassLoader()));
        return redisTemplate;
    }

    @SuppressWarnings("rawtypes")
    @Bean
    public CacheManager cacheManager(RedisTemplate redisTemplate) {
        Map<String, CacheConfigOptions> cacheConfigOptions = super.getCacheConfigOptions(applicationContext, CacheType.REMOTE, CacheType.BOTH);
        Set<String> cacheNames = Sets.newHashSet();
        Map<String, Long> expires = Maps.newHashMap();
        for (Map.Entry<String, CacheConfigOptions> entry : cacheConfigOptions.entrySet()) {
            cacheNames.add(entry.getKey());
            expires.put(entry.getKey(), entry.getValue().expired());
        }
        RedisCacheManager redisCacheManager = new RedisCacheManager(redisTemplate, cacheNames, false);
        Field dynamicField = ReflectionUtils.findField(redisCacheManager.getClass(), "dynamic");
        ReflectionUtils.makeAccessible(dynamicField);
        ReflectionUtils.setField(dynamicField, redisCacheManager, false);
        redisCacheManager.setExpires(expires);
        return redisCacheManager;
    }

}
