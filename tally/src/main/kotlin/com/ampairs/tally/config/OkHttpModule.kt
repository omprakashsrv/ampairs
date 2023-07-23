package com.ampairs.tally.config

import com.ampairs.api.repository.UserPreferencesRepository
import com.ampairs.network.auth.ApiAuthenticator
import com.ampairs.network.auth.AuthInterceptor
import com.ampairs.network.model.DateTimeAdapter
import com.ampairs.tally.repository.TokenStoreRepository
import com.ampairs.tally.repository.UserRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.converter.moshi.MoshiConverterFactory

@Configuration
class OkHttpModule @Autowired constructor() {

    @Bean
    fun okHttpClient(
        userPreferencesRepository: UserPreferencesRepository
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        builder.addInterceptor(loggingInterceptor)
        builder.addInterceptor(AuthInterceptor(userPreferencesRepository))
        return builder.authenticator(ApiAuthenticator(AUTH_ENDPOINT, userPreferencesRepository)).build()
    }

    @Bean
    fun moshiConverterFactory(): MoshiConverterFactory {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(DateTimeAdapter()).build()
        return MoshiConverterFactory.create(moshi)
    }

    @Bean
    fun userPreferencesRepository(userRepository: UserRepository): UserPreferencesRepository {
        return TokenStoreRepository()
    }
}