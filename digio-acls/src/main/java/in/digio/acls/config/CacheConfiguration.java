
package in.digio.acls.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@EnableCaching
@AutoConfiguration
public class CacheConfiguration {

    public static final String ACL_CACHE = "aclCache";

    @Bean
    public CacheManager aclCacheManager() {
        return new ConcurrentMapCacheManager(ACL_CACHE);
    }


}