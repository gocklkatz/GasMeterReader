package com.example.greetingcard

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GasMeterApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun newImageLoader(): ImageLoader = imageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
