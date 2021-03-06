package com.worldsnas.mediasessionsample

import java.io.File

data class AyaMediaItem(
    val ayaFile: File,

    val ayaOrder: Long,
    val surahOrder: Long,

    val surahName: String,
    val reciterName: String,
){
    companion object {
        internal const val STARTING_AYA_ORDER_ID = 0L
    }
}