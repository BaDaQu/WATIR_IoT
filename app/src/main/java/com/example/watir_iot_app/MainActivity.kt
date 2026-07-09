package com.example.watir_iot_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.watir_iot_app.core.navigation.AppNavigation
import com.example.watir_iot_app.ui.theme.WATIR_IoT_APPTheme
import com.example.watir_iot_app.viewmodel.WatirViewModel
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WATIR_IoT_APP)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val watirViewModel: WatirViewModel = viewModel()
            val isDarkMode by watirViewModel.isDarkMode
            val language by watirViewModel.language

            LaunchedEffect(language) {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
                if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language) {
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }

            WATIR_IoT_APPTheme(darkTheme = isDarkMode) {
                AppNavigation(watirViewModel)
            }
        }
    }
}