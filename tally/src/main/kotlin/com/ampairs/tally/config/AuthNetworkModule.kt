package com.ampairs.tally.config

import com.ampairs.network.auth.AuthApi
import com.ampairs.network.user.UserApi
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

const val AUTH_ENDPOINT = "http://localhost:8080/"

@Configuration
@ComponentScan(value = arrayOf("com.ampairs.tally"))
class AuthNetworkModule @Autowired constructor() {

    @Bean
    fun authRetrofit(
        okHttpClient: OkHttpClient,
        moshiConverterFactory: MoshiConverterFactory
    ): Retrofit {
        return Retrofit.Builder().addConverterFactory(moshiConverterFactory)
            .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
            .baseUrl(AUTH_ENDPOINT).client(okHttpClient).build()
    }

    @Bean
    fun userApi(authRetrofit: Retrofit): UserApi =
        authRetrofit.create(UserApi::class.java)

    @Bean
    fun authApi(authRetrofit: Retrofit): AuthApi =
        authRetrofit.create(AuthApi::class.java)


}