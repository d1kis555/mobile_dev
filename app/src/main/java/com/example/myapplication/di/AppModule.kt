package com.example.myapplication.di

import com.example.myapplication.GameViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { GameViewModel() }
}
