package com.chensiyingcrystal.crystalinstacart.location.module

import com.chensiyingcrystal.crystalinstacart.location.LocationController
import com.chensiyingcrystal.crystalinstacart.location.LocationControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class LocationModule {
  @Binds
  abstract fun bindLocationController(impl: LocationControllerImpl): LocationController

}