package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.ScheduleRepository
import com.jetbrains.kmpapp.data.api.MireaScheduleApi
import com.jetbrains.kmpapp.data.storage.ScheduleStorage
import com.jetbrains.kmpapp.screens.other.OtherViewModel
import com.jetbrains.kmpapp.screens.schedule.ScheduleViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    single {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        HttpClient {
            install(ContentNegotiation) {
                json(json, contentType = ContentType.Any)
            }
        }
    }

    singleOf(::MireaScheduleApi)
    singleOf(::ScheduleStorage)
    singleOf(::ScheduleRepository)
}

val viewModelModule = module {
    factoryOf(::ScheduleViewModel)
    factoryOf(::OtherViewModel)
}

fun initKoin() {
    startKoin {
        modules(
            dataModule,
            viewModelModule,
        )
    }
}
