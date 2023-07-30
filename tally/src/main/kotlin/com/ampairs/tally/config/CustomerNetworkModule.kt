package com.ampairs.tally.config

import com.ampairs.network.customer.CustomerApi
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

const val CUSTOMER_ENDPOINT = "http://localhost:8081/"

@Configuration
@ComponentScan(value = arrayOf("com.ampairs.tally"))
class CustomerNetworkModule @Autowired constructor() {

    @Bean
    fun customerRetrofit(
        okHttpClient: OkHttpClient,
        moshiConverterFactory: MoshiConverterFactory
    ): Retrofit {
        return Retrofit.Builder().addConverterFactory(moshiConverterFactory)
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(CUSTOMER_ENDPOINT).client(okHttpClient).build()
    }

    @Bean
    fun customerApi(customerRetrofit: Retrofit): CustomerApi =
        customerRetrofit.create(CustomerApi::class.java)


}