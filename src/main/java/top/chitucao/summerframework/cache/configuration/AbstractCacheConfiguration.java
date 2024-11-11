package top.chitucao.summerframework.cache.configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
import top.chitucao.summerframework.cache.CacheType;
import top.chitucao.summerframework.cache.annotation.CacheConfigOptions;
import top.chitucao.summerframework.cache.annotation.CacheConfigOptionsContainer;

/**
 * AbstractCacheConfiguration
 *
 * @author chitucao
 */
@Slf4j
public abstract class AbstractCacheConfiguration implements ApplicationContextAware {

    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
    * 根据缓存类型，获取缓存相关的注解
    * @param applicationContext    应用上下文
    * @param cacheTypes            缓存类型
    * @return                      缓存类型相关的缓存注解
    */
    protected Map<String, CacheConfigOptions> getCacheConfigOptions(ApplicationContext applicationContext, CacheType... cacheTypes) {
        Map<String, CacheConfigOptions> cacheNameCacheConfigOptionMap = getCacheConfigOptions(applicationContext);
        HashSet<CacheType> cacheTypeSet = Sets.newHashSet(cacheTypes);
        Map<String, CacheConfigOptions> result = Maps.newHashMap();
        for (Map.Entry<String, CacheConfigOptions> entry : cacheNameCacheConfigOptionMap.entrySet()) {
            if (cacheTypeSet.contains(entry.getValue().cacheType())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 获取所有缓存相关的注解
     * @param applicationContext    应用上下文
     * @return                      所有缓存相关的注解
     */
    protected Map<String, CacheConfigOptions> getCacheConfigOptions(ApplicationContext applicationContext) {
        Map<String, CacheConfigOptions> result = Maps.newHashMap();

        Map<String, Object> cacheAnnotationBeans = Maps.newHashMap();
        cacheAnnotationBeans.putAll(applicationContext.getBeansWithAnnotation(CacheConfigOptions.class));
        cacheAnnotationBeans.putAll(applicationContext.getBeansWithAnnotation(CacheConfigOptionsContainer.class));
        if (cacheAnnotationBeans.isEmpty()) {
            return result;
        }

        for (Object beanInstance : cacheAnnotationBeans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(beanInstance);
            List<CacheConfigOptions> cacheConfigOptionsAll = Lists.newLinkedList();
            CacheConfigOptions cacheConfigOptions = AnnotationUtils.findAnnotation(targetClass, CacheConfigOptions.class);
            if (cacheConfigOptions != null) {
                cacheConfigOptionsAll.add(cacheConfigOptions);
            }
            CacheConfigOptionsContainer cacheConfigOptionsContainer = AnnotationUtils.findAnnotation(targetClass, CacheConfigOptionsContainer.class);
            if (cacheConfigOptionsContainer != null) {
                Collections.addAll(cacheConfigOptionsAll, cacheConfigOptionsContainer.value());
            }
            for (CacheConfigOptions cacheConfigOptions1 : cacheConfigOptionsAll) {
                for (String cacheName : cacheConfigOptions1.cacheNames()) {
                    if (result.containsKey(cacheName)) {
                        // 重复的缓存名称，这里只做警告处理（后续重复的缓存名称不再生效）
                        log.error("[CacheAnnotationUtil] Duplicate cache name: {} find.", cacheName);
                    } else {
                        result.put(cacheName, cacheConfigOptions1);
                    }
                }
            }
        }
        return result;
    }

}