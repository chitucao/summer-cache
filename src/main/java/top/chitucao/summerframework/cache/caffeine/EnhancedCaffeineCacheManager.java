package top.chitucao.summerframework.cache.caffeine;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.AbstractCacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.Setter;

/**
 * caffeine缓存管理器
 * 
 * @author chitucao
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnhancedCaffeineCacheManager extends AbstractCacheManager {

    @Setter
    private boolean                     allowNullValues = true;

    private final Map<String, Caffeine> cacheConfigMap;

    public EnhancedCaffeineCacheManager(Map<String, Caffeine> cacheConfigMap) {
        this.cacheConfigMap = cacheConfigMap;
    }

    /**
     * Load the initial caches for this cache manager.
     * <p>Called by {@link #afterPropertiesSet()} on startup.
     * The returned collection may be empty but must not be {@code null}.
     */
    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<Cache> caches = new LinkedList<>();
        cacheConfigMap.forEach((key, value) -> caches.add(new CaffeineCache(key, value.build(), allowNullValues)));
        return caches;
    }
}
