package com.agrojurado.sfmappv2.di

import com.agrojurado.sfmappv2.domain.security.DefaultRoleAccessControl
import com.agrojurado.sfmappv2.domain.security.RoleAccessControl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoleAccessControlModule {
    @Provides
    @Singleton
    fun provideRoleAccessControl(): RoleAccessControl {
        return DefaultRoleAccessControl()
    }
}