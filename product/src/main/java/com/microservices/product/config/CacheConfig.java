package com.microservices.product.config;

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
        config.setInstanceName("product-service-hazelcast");
        config.getNetworkConfig()
        .getJoin()
        .getTcpIpConfig()
        .addMember("localhost:5758")
        .setEnabled(true);
        config.getNetworkConfig()
        .getJoin()
        .getMulticastConfig()
        .setEnabled(false);
        
        // Configure cache for products
        MapConfig productCacheConfig = new MapConfig();
        productCacheConfig.setName("products");
        productCacheConfig.setTimeToLiveSeconds(600); // 10 minutes
        productCacheConfig.setMaxIdleSeconds(300); // 5 minutes
        
        config.addMapConfig(productCacheConfig);
        
        return Hazelcast.newHazelcastInstance(config);
    }
    
    @Bean
    @Primary
    public org.springframework.cache.CacheManager springCacheManager() {
        // Get JCache (JSR-107) CacheManager
        CacheManager jcacheManager = Caching.getCachingProvider().getCacheManager();
        
        // Ensure "userCache" exists
        if (jcacheManager.getCache("products") == null) {
            MutableConfiguration<String, Object> userCacheConfig =
                new MutableConfiguration<String, Object>()
                    .setTypes(String.class, Object.class)
                    .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 5)))
                    .setStatisticsEnabled(true);

            jcacheManager.createCache("products", userCacheConfig);
        }
        
        // Wrap JCacheManager inside Spring's JCacheCacheManager (Fixes the issue)
        return new JCacheCacheManager(jcacheManager);
    }
    
    @Bean
    public CacheManager jCacheManager() {
        return Caching.getCachingProvider().getCacheManager(); // JCache (javax.cache) Manager
    }
	
}
