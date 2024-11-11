package top.chitucao.summerframework.cache.annotation;

import java.lang.annotation.*;

import top.chitucao.summerframework.cache.CacheType;

/**
 * 缓存配置
 *
 * @author chitucao
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CacheConfigOptionsContainer.class)
public @interface CacheConfigOptions {

    /**
     * 指定该注解设置作用于哪个缓存,如果多个缓存使用相同的设置,可以设置多个名称<br/>
     * 如果不设置cacheNames,则需要配合{@link org.springframework.cache.annotation.CacheConfig}指定默认缓存名,否则启动时抛出异常
     */
    String[] cacheNames();

    /**
     * 启用的缓存类型,默认全部启用
     */
    CacheType cacheType() default CacheType.BOTH;

    //------------------------------------------caffeine配置 开始------------------------------------------
    /**
     * caffeine spec
     * e.g. initialCapacity=50,maximumSize=500,expireAfterWrite=10s,refreshAfterWrite=5s
     */
    String spec() default "";

    /**
     * 初始化缓存容量
     */
    int initialCapacity() default 0;

    /**
     * 缓存最大容量
     */
    long maximumSize() default 1024;

    /**
     * 访问后多少时间没有访问缓存失效
     * <br/> 时间单位 ： d、h、m、s e.g: 1d,1h,1m,1s
     * <br/> 每次访问后会重置缓存失效时间
     */
    String expireAfterAccess() default "";

    /**
     * 写完后多少时间缓存失效 单位同 expireAfterAccess
     * <br/> 第一次写入后不会重置失效时间
     */
    String expireAfterWrite() default "";
    //------------------------------------------caffeine配置 结束------------------------------------------

    //------------------------------------------redis配置 开始------------------------------------------
    /**
     * 远程缓存失效时间 单位为秒
     * 相当于 caffeine 的 expireAfterWrite
     */
    long expired() default 300;
    //------------------------------------------redis配置 结束------------------------------------------

}
