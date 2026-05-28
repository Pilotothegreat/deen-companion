package com.leekleak.trafficlight.model

import coil3.ImageLoader
import coil3.request.crossfade
import org.koin.dsl.module

val managerModule = module {
    single {
        ImageLoader.Builder(get())
            .crossfade(true)
            .build()
    }
}