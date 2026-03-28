package com.omar.musica.store

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanStateRepository @Inject constructor() {
    val scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanHistory = MutableStateFlow<List<String>>(emptyList())
}
