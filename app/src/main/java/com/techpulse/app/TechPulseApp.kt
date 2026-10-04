package com.techpulse.app

import android.app.Application
import com.techpulse.app.data.learn.LearningStore
import com.techpulse.app.data.translate.Translator

/**
 * Точка инициализации приложения: поднимаем до старта любых экранов
 * сетевой переводчик и хранилище прогресса обучения.
 */
class TechPulseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Translator.init(this)
        LearningStore.init(this)
    }
}
