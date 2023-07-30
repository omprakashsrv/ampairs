package com.ampairs.tally.config

import com.ampairs.network.product.ProductApi
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

const val PRODUCT_ENDPOINT = "http://localhost:8082/"

@Configuration
@ComponentScan(value = arrayOf("com.ampairs.tally"))
class ProductNetworkModule @Autowired constructor() {

    @Bean
    fun productRetrofit(
        okHttpClient: OkHttpClient,
        moshiConverterFactory: MoshiConverterFactory
    ): Retrofit {
        return Retrofit.Builder().addConverterFactory(moshiConverterFactory)
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(PRODUCT_ENDPOINT).client(okHttpClient).build()
    }

    @Bean
    fun productApi(productRetrofit: Retrofit): ProductApi =
        productRetrofit.create(ProductApi::class.java)


}