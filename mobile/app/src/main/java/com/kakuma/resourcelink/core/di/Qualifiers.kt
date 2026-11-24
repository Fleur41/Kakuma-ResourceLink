package com.kakuma.resourcelink.core.di

import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

/** OkHttp client without the token authenticator — used for the refresh call itself. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class RefreshClient
