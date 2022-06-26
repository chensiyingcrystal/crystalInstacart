package com.chensiyingcrystal.crystalinstacart.retrofit

import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnect
import com.firebase.geofire.GeoFire
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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
