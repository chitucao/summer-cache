package top.chitucao.summerframework.cache.configuration;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ReflectionUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
import top.chitucao.summerframework.cache.CacheType;
import top.chitucao.summerframework.cache.annotation.CacheConfigOptions;
import top.chitucao.summerframework.cache.caffeine.EnhancedCaffeineCacheManager;
import top.chitucao.summerframework.cache.composite.CompositeCacheManager;
import top.chitucao.summerframework.cache.redis.LettuceConnectConfiguration;
import top.chitucao.summerframework.cache.utils.DurationStyle;

/**
 * 组合缓存配置
 *
 * @author chitucao
 */
@Slf4j
@Import({ LettuceConnectConfiguration.class })
@Configuration
public class CompositeCacheConfiguration extends AbstractCacheConfiguration {

    static final int UNSET_INT = -1;

    @Bean
    public CacheManager cacheManager() {
        CacheManager local = localCacheManager();
        CacheManager remote = remoteCacheManager();
        return new CompositeCacheManager(local, remote);
    }

    public CacheManager localCacheManager() {
        Map<String, CacheConfigOptions> cacheConfigOptions = super.getCacheConfigOptions(applicationContext, CacheType.LOCAL, CacheType.BOTH);
        @SuppressWarnings("rawtypes")
        Map<String, Caffeine> cacheConfigMap = Maps.newHashMap();
        for (Map.Entry<String, CacheConfigOptions> entry : cacheConfigOptions.entrySet()) {
            CacheConfigOptions value = entry.getValue();
            if (StringUtils.isNotBlank(value.spec())) {
                // 优先从spec加载
                cacheConfigMap.put(entry.getKey(), Caffeine.from(entry.getValue().spec()));
            } else {
                Caffeine<Object, Object> builder = Caffeine.newBuilder();
                if (value.initialCapacity() != UNSET_INT) {
                    builder.initialCapacity(value.initialCapacity());
                }
                if (value.maximumSize() != UNSET_INT) {
                    builder.maximumSize(value.maximumSize());
                }
                if (StringUtils.isNotBlank(value.expireAfterWrite())) {
                    builder.expireAfterWrite(DurationStyle.detectAndParse(value.expireAfterWrite()));
                }
                if (StringUtils.isNotBlank(value.expireAfterAccess())) {
                    builder.expireAfterAccess(DurationStyle.detectAndParse(value.expireAfterAccess()));
                }
                cacheConfigMap.put(entry.getKey(), builder);
            }
        }
        EnhancedCaffeineCacheManager cacheManager = new EnhancedCaffeineCacheManager(cacheConfigMap);
        cacheManager.setAllowNullValues(false);
        cacheManager.afterPropertiesSet();
        return cacheManager;
    }

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

    public CacheManager remoteCacheManager() {
        Map<String, CacheConfigOptions> cacheConfigOptions = super.getCacheConfigOptions(applicationContext, CacheType.REMOTE, CacheType.BOTH);
        Set<String> cacheNames = Sets.newHashSet();
        Map<String, Long> expires = Maps.newHashMap();
        for (Map.Entry<String, CacheConfigOptions> entry : cacheConfigOptions.entrySet()) {
            cacheNames.add(entry.getKey());
            expires.put(entry.getKey(), entry.getValue().expired());
        }
        RedisCacheManager redisCacheManager = new RedisCacheManager(applicationContext.getBean(RedisTemplate.class), cacheNames, false);
        Field dynamicField = ReflectionUtils.findField(redisCacheManager.getClass(), "dynamic");
        ReflectionUtils.makeAccessible(dynamicField);
        ReflectionUtils.setField(dynamicField, redisCacheManager, false);
        redisCacheManager.setExpires(expires);
        redisCacheManager.afterPropertiesSet();
        return redisCacheManager;
    }

}
