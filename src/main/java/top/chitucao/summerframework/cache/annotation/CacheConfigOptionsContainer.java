package top.chitucao.summerframework.cache.annotation;

import java.lang.annotation.*;

/**
 * 适用于重复注解
 *
 * @author chitucao
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfigOptionsContainer {
    CacheConfigOptions[] value();
}
