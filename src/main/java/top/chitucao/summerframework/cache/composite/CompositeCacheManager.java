package top.chitucao.summerframework.cache.composite;

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;

/**
 * 多级缓存管理器实现
 *
 * @author chitucao
 */
public class CompositeCacheManager extends AbstractCacheManager {

    /** 本地缓存 */
    private final CacheManager local;
    /** 远程缓存 */
    private final CacheManager remote;

    public CompositeCacheManager(CacheManager local, CacheManager remote) {
        this.local = local;
        this.remote = remote;
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        return Collections.emptyList();
    }

    @Override
    protected Cache getMissingCache(String name) {
        Cache localCache = local.getCache(name);
        Cache remoteCache = remote.getCache(name);
        if (localCache == null && remoteCache == null) {
            return null;
        }
        return new CompositeCache(name, localCache, remoteCache);
    }
}
