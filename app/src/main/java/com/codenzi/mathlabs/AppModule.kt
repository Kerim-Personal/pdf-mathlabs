package com.codenzi.mathlabs.di

import android.content.Context
import com.codenzi.mathlabs.CourseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Bu bağımlılıkların uygulama ömrü boyunca yaşayacağını belirtir
object AppModule {

    @Provides
    @Singleton // Context'in tek bir örneğinin olmasını sağlar
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    // Hilt'e bir CourseRepository'ye ihtiyaç duyulduğunda nasıl sağlanacağını öğretir.
    // Hilt, bu fonksiyonu çağırırken gereken 'context' parametresini yukarıdaki
    // provideContext fonksiyonundan alacaktır.
    @Provides
    @Singleton
    fun provideCourseRepository(context: Context): CourseRepository {
        return CourseRepository(context)
    }
}