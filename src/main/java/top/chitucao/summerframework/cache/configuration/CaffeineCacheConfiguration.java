package top.chitucao.summerframework.cache.configuration;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
import top.chitucao.summerframework.cache.CacheType;
import top.chitucao.summerframework.cache.annotation.CacheConfigOptions;
import top.chitucao.summerframework.cache.caffeine.EnhancedCaffeineCacheManager;
import top.chitucao.summerframework.cache.utils.DurationStyle;

/**
 * caffeine缓存配置
 */
@Slf4j
public class CaffeineCacheConfiguration extends AbstractCacheConfiguration {

    static final int UNSET_INT = -1;

    @Bean
    public CacheManager cacheManager() {
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
        return cacheManager;
    }

}
