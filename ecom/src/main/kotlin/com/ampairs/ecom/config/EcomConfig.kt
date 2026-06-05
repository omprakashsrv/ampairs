package com.ampairs.ecom.config

import com.ampairs.ecom.interceptor.StorefrontTenantInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableScheduling
class EcomConfig(
    private val storefrontTenantInterceptor: StorefrontTenantInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(storefrontTenantInterceptor)
            .addPathPatterns("/v1/store/**")
            .order(10)
    }
}
