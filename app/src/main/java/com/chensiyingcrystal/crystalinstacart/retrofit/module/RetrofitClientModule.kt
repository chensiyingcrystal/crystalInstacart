package com.chensiyingcrystal.crystalinstacart.retrofit.module

import com.chensiyingcrystal.crystalinstacart.retrofit.RetrofitClient
import com.chensiyingcrystal.crystalinstacart.retrofit.RetrofitClientImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class RetrofitClientModule {
  @Binds
  abstract fun bindRetrofitClient(impl: RetrofitClientImpl): RetrofitClient
}