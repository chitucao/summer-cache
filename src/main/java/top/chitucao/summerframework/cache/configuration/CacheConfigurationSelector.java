package top.chitucao.summerframework.cache.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import top.chitucao.summerframework.cache.CacheType;
import top.chitucao.summerframework.cache.annotation.EnableSummerCache;
import top.chitucao.summerframework.cache.configuration.enhance.MyProxyCachingConfiguration;

/**
 * CacheConfigurationSelector
 *
 * @author chitucao
 */
public class CacheConfigurationSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        List<String> result = new ArrayList<>(3);
        AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableSummerCache.class.getName()));
        CacheType cacheType = annotationAttributes.getEnum("value");
        if (cacheType == CacheType.BOTH) {
            result.add(CompositeCacheConfiguration.class.getName());
        } else if (cacheType == CacheType.REMOTE) {
            result.add(MyRedisCacheConfiguration.class.getName());
        } else {
            result.add(CaffeineCacheConfiguration.class.getName());
        }

        result.add(AutoProxyRegistrar.class.getName());
        result.add(MyProxyCachingConfiguration.class.getName());

        return StringUtils.toStringArray(result);
    }
}
