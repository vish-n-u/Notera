package com.example.devaudioreccordings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.user: DataStore<Preferences> by preferencesDataStore(name = "userInfo")


object DataStoreKeys {
    val USER_UID = stringPreferencesKey("user_uid")
    val Used_Transcription_Duration = longPreferencesKey("transcription_duration")
    val Total_Transcription_Duration = longPreferencesKey("total_transcription_duration")
    val Used_Linkedin_Text_Conversion_Count = intPreferencesKey("used_linkedin_conversion_times")
    val Total_Linkedin_Text_Conversion_Count = intPreferencesKey("total_linkedin_conversion_times")
    val Used_Enhance_Text_Count = intPreferencesKey("used_textTransformation_times")
    val Total_Enhance_Text_Count = intPreferencesKey("total_textTransformation_times")
    val first_launch = booleanPreferencesKey("first_launch")
    val Use_App_Theme = booleanPreferencesKey("use_app_theme")
    val App_Theme = stringPreferencesKey("app_theme")
    val Color_Scheme = stringPreferencesKey("color_scheme")
    val Has_Shown_App_Intro = booleanPreferencesKey("has_shown_app_intro")

}