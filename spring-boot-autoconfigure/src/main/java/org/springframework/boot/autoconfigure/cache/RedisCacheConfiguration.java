package org.springframework.boot.autoconfigure.cache;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
/** 
 * Redis cache configuration.
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration @AutoConfigureAfter(RedisAutoConfiguration.class) @ConditionalOnBean(RedisTemplate.class) @ConditionalOnMissingBean(CacheManager.class) @Conditional(CacheCondition.class) class RedisCacheConfiguration {
  @Autowired public CacheProperties cacheProperties;
  @Autowired public CacheManagerCustomizers customizerInvoker;
  @Bean public RedisCacheManager cacheManager(  RedisTemplate<Object,Object> redisTemplate){
    RedisCacheManager cacheManager=new RedisCacheManager(redisTemplate);
    cacheManager.setUsePrefix(true);
    List<String> cacheNames=this.cacheProperties.getCacheNames();
    if (!cacheNames.isEmpty()) {
      cacheManager.setCacheNames(cacheNames);
    }
    return this.customizerInvoker.customize(cacheManager);
  }
  public RedisCacheConfiguration(){
  }
}
