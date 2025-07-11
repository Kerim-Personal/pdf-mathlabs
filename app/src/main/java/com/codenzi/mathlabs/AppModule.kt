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
    // Bu artık state'e sahip olmadığı için context'e ihtiyaç duymaz.
    @Provides
    @Singleton
    fun provideCourseRepository(): CourseRepository {
        return CourseRepository()
    }
}