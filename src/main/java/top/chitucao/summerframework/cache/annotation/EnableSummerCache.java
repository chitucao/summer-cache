package top.chitucao.summerframework.cache.annotation;

import java.lang.annotation.*;

import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import top.chitucao.summerframework.cache.CacheType;
import top.chitucao.summerframework.cache.configuration.CacheConfigurationSelector;

/**
 * 启用缓存开关
 * @author chitucao
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import({ CacheConfigurationSelector.class })
public @interface EnableSummerCache {

    /**
     * 全局启用的缓存类型
     */
    CacheType value() default CacheType.BOTH;

    /**
    * Indicate the ordering of the execution of the caching advisor
    * when multiple advices are applied at a specific joinpoint.
    * <p>The default is {@link Ordered#LOWEST_PRECEDENCE}.
    */
    int order() default Ordered.LOWEST_PRECEDENCE;

}