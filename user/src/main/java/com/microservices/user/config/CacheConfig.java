package com.microservices.user.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

@Configuration
public class CacheConfig {
    
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("user-service-hazelcast");
        config.getNetworkConfig()
        .getJoin()
        .getTcpIpConfig()
        .addMember("localhost:5759")
        .setEnabled(true);
        config.getNetworkConfig()
        .getJoin()
        .getMulticastConfig()
        .setEnabled(false);
        // Configure cache for users
        MapConfig userCacheConfig = new MapConfig();
        userCacheConfig.setName("users");
        userCacheConfig.setTimeToLiveSeconds(300); // 5 minutes
        userCacheConfig.setMaxIdleSeconds(120); // 2 minutes
        
        config.addMapConfig(userCacheConfig);
        
        return Hazelcast.newHazelcastInstance(config);
    }
    
    @Bean
    @Primary
    public org.springframework.cache.CacheManager springCacheManager() {
        // Get JCache (JSR-107) CacheManager
        CacheManager jcacheManager = Caching.getCachingProvider().getCacheManager();
        
        // Ensure "userCache" exists
        if (jcacheManager.getCache("users") == null) {
            MutableConfiguration<String, Object> userCacheConfig =
                new MutableConfiguration<String, Object>()
                    .setTypes(String.class, Object.class)
                    .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 5)))
                    .setStatisticsEnabled(true);

            jcacheManager.createCache("users", userCacheConfig);
        }
        
        // Wrap JCacheManager inside Spring's JCacheCacheManager (Fixes the issue)
        return new JCacheCacheManager(jcacheManager);
    }
    
    @Bean
    public CacheManager jCacheManager() {
        return Caching.getCachingProvider().getCacheManager(); // JCache (javax.cache) Manager
    }
	
}