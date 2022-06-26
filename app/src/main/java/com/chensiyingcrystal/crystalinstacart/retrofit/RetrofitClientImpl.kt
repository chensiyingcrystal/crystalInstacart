package com.chensiyingcrystal.crystalinstacart.retrofit

import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

@Singleton
class RetrofitClientImpl  @Inject constructor() : RetrofitClient {
  private val retrofit: Retrofit
  private val googleApi: IGoogleAPI

  init {
    retrofit= Retrofit.Builder().baseUrl("https://maps.googleapis.com")
      .addConverterFactory(ScalarsConverterFactory.create()).build()
    googleApi = retrofit.create(IGoogleAPI::class.java)
  }

  override fun getGoogleApi(): IGoogleAPI {
    return googleApi
  }


}
