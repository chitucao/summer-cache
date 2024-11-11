package top.chitucao.summerframework.cache.redis;

import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

/**
 * RedisProperties
 *
 * @author chitucao
 */
@Getter
public class RedisProperties {

    /** 天眼标志 */
    @Value("${summer.cache.redis.appCode}")
    private String appCode;

    /** 环境 */
    @Value("${summer.cache.redis.env}")
    private String env;

    /** redis的组 */
    @Value("${summer.cache.redis.groupName}")
    private String groupName;
}