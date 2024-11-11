package top.chitucao.summerframework.cache.composite;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.cache.Cache;

/**
 * 多级缓存实现
 *
 * @author chitucao
 */
public class CompositeCache implements Cache {

    /** 缓存名称 */
    private final String name;
    /** 本地缓存 */
    private final Cache  local;
    /** 远程缓存 */
    private final Cache  remote;

    public CompositeCache(String name, Cache local, Cache remote) {
        this.name = name;
        this.local = local;
        this.remote = remote;
    }

    /**
     * Return the cache name.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Return the underlying native cache provider.
     */
    @Override
    public Object getNativeCache() {
        throw new UnsupportedOperationException("don't support native cache");
    }

    /**
     * 优先本地，后远程，远程有本地没有的时候从远程写入本地
     */
    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper valueWrapper = getReturnIfNotNull(local, cache -> cache.get(key));
        if (valueWrapper != null) {
            return valueWrapper;
        }
        valueWrapper = getReturnIfNotNull(remote, cache -> cache.get(key));
        if (valueWrapper != null) {
            Object val = valueWrapper.get();
            Optional.ofNullable(local).ifPresent(cache -> local.put(key, val));
        }
        return valueWrapper;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper valueWrapper = get(key);
        if (valueWrapper == null) {
            return null;
        }
        return (T) valueWrapper.get();
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper valueWrapper = get(key);
        if (valueWrapper == null) {
            T value;
            try {
                value = valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
            put(key, value);
            return value;
        }
        //noinspection unchecked
        return (T) valueWrapper.get();
    }

    @Override
    public void put(Object key, Object value) {
        Optional.ofNullable(local).ifPresent(cache -> cache.put(key, value));
        Optional.ofNullable(remote).ifPresent(cache -> cache.put(key, value));
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existingValueWrapper = get(key);
        if (existingValueWrapper == null || existingValueWrapper.get() == null) {
            put(key, value);
            return null;
        }
        return existingValueWrapper;
    }

    @Override
    public void evict(Object key) {
        Optional.ofNullable(local).ifPresent(cache -> cache.evict(key));
        Optional.ofNullable(remote).ifPresent(cache -> cache.evict(key));
    }

    @Override
    public void clear() {
        Optional.ofNullable(local).ifPresent(Cache::clear);
        Optional.ofNullable(remote).ifPresent(Cache::clear);
    }

    private <R> R getReturnIfNotNull(Cache cache, Function<Cache, R> func) {
        if (cache != null) {
            return func.apply(cache);
        }
        return null;
    }
}
