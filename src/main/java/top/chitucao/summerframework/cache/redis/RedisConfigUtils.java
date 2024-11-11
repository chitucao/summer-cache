package top.chitucao.summerframework.cache.redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import lombok.Getter;
import lombok.Setter;

/**
 * 从公共的配置中心获取redis的配置.
 */
public class RedisConfigUtils {
    private static final String URL                       = "http://tccomponent.17usoft.com/tcconfigcenter6/v6/getspecifickeyvalue/%s/%s/TCBase.Cache";
    private static final String ELEMENT_NAME_TCBASE_CACHE = "tcbase.cache";
    private static final String ELEMENT_NAME_CACHE        = "cache";
    private static final String ELEMENT_NAME_REDIS        = "redis";

    /**
     * Method getConfigString ...
     *
     * @param projectName of type String
     * @param env of type String
     * @return String
     */
    private static String getConfigString(String projectName, String env) {
        String urlStr = String.format(URL, env, projectName);
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String auth = projectName + ":" + projectName;
            byte[] authEncBytes = Base64.getEncoder().encode(auth.getBytes());
            String authStringEnc = new String(authEncBytes);
            conn.setRequestProperty("Authorization", "Basic " + authStringEnc);
            conn.setRequestMethod("GET");
            conn.connect();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

    /**
     * Method getRedisConfig ...
     *
     * @param projectName of type String
     * @param env of type String
     * @param groupName of type String
     * @return CacheConfig
     */
    public static CacheConfig getRedisConfig(String projectName, String env, String groupName) {
        Map<String, CacheConfig> configMap = getRedisConfig(projectName, env);
        if (configMap == null || configMap.isEmpty()) {
            return null;
        }
        return configMap.get(groupName);
    }

    /**
     * Method getRedisConfig ...
     *
     * @param projectName of type String
     * @param env of type String
     * @return Map<String ,   CacheConfig>
     */
    public static Map<String, CacheConfig> getRedisConfig(String projectName, String env) {
        String config = getConfigString(projectName, env);
        Map<String, CacheConfig> map = new HashMap<>();
        try {
            SAXReader reader = new SAXReader();
            reader.setEncoding("UTF-8");
            Document document = reader.read(new StringReader(config));
            Element element = document.getRootElement();
            if (!ELEMENT_NAME_TCBASE_CACHE.equals(element.getName())) {
                return map;
            }
            Iterator<Element> iterator = element.elementIterator(ELEMENT_NAME_CACHE);
            while (iterator.hasNext()) {
                Element cacheElement = iterator.next();
                CacheConfig cacheConfig = new CacheConfig();
                String nameAttr = cacheElement.attributeValue("name");
                if (nameAttr == null || nameAttr.isEmpty()) {
                    continue;
                }
                cacheConfig.name = nameAttr.trim();
                cacheConfig.setType(cacheElement.attributeValue("type") == null ? "S" : cacheElement.attributeValue("type").trim());
                Iterator<Element> redisIterator = cacheElement.elementIterator(ELEMENT_NAME_REDIS);
                while (redisIterator.hasNext()) {
                    Element redisElement = redisIterator.next();
                    RedisConfig redisConfig = new RedisConfig();
                    String ip = redisElement.attributeValue("ip") == null ? "" : redisElement.attributeValue("ip").trim();
                    redisConfig.setIp(ip);
                    if (StringUtils.isNotBlank(ip)) {
                        String[] url = StringUtils.split(ip, ":");
                        if (url.length == 2) {
                            redisConfig.setHost(url[0]);
                            redisConfig.setPort(Integer.parseInt(url[1]));
                        }
                    }
                    redisConfig.setSentinel(redisElement.attributeValue("sentinel") != null && Boolean.parseBoolean(redisElement.attributeValue("sentinel").trim()));
                    redisConfig.setTimeOut(redisElement.attributeValue("timeOut") == null ? 1000 : Integer.parseInt(redisElement.attributeValue("timeOut").trim()));
                    redisConfig.setPassword(redisElement.attributeValue("password") == null ? null : redisElement.attributeValue("password").trim());
                    cacheConfig.redisConfig.add(redisConfig);
                }
                map.put(cacheConfig.getName(), cacheConfig);
            }
        } catch (Exception ignored) {
        }
        return map;
    }

    @Getter
    @Setter
    public static class CacheConfig {
        List<RedisConfig> redisConfig = new ArrayList<>();
        private String    name;

        /** S 代表redis为单机模式， M代表为主从模式，C代表集群模式 */
        private String    type;

        private String    masterName;
    }

    @Getter
    @Setter
    public static class RedisConfig {
        private String  ip;
        private String  password;
        private int     timeOut  = 5000;
        private int     maxPool  = 20;
        private int     minPool  = 3;
        private boolean sentinel = false;
        private String  host;
        private int     port;
    }
}
