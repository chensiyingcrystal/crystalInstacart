package com.chensiyingcrystal.crystalinstacart.retrofit

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface IGoogleAPI {
  @GET
  fun getPath(@Url url: String): Call<String>
}