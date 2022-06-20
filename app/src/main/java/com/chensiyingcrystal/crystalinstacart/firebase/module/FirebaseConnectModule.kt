package com.chensiyingcrystal.crystalinstacart.firebase.module

import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnect
import com.chensiyingcrystal.crystalinstacart.firebase.FirebaseConnectImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class FirebaseConnectModule {
  @Binds
  abstract fun bindFirebaseConnect(impl: FirebaseConnectImpl): FirebaseConnect
}