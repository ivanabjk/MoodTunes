package com.example.moodtunes_v1.home

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner

//class HomeViewModelFactory(
//    private val app: Application,
//    private val context: Context,
//    private val savedStateHandle: SavedStateHandle
//) : ViewModelProvider.Factory {
//
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
//            return HomeViewModel(app, context, savedStateHandle) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}

class HomeViewModelFactory(
    private val app: Application,
    private val context: Context,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        state: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(app, context, state) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}