package com.andresDev.puriapp.di

import com.andresDev.puriapp.data.api.ClienteApi
import com.andresDev.puriapp.data.api.PedidoApi
import com.andresDev.puriapp.data.api.VisitaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://192.168.31.217:8080/api/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideClienteApi(retrofit: Retrofit): ClienteApi =
        retrofit.create(ClienteApi::class.java)

    @Provides
    @Singleton
    fun providePedidoApi(retrofit: Retrofit): PedidoApi =
        retrofit.create(PedidoApi::class.java)

    @Provides
    @Singleton
    fun provideVisitaApi(retrofit: Retrofit): VisitaApi =
        retrofit.create(VisitaApi::class.java)
}