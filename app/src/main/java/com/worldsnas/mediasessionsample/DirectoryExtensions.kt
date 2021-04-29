package com.worldsnas.mediasessionsample

import java.io.File


fun AyaPlayList.needsDownloading(downloadDir: File) =
    !getSurahDirectory(downloadDir).exists()

internal fun AyaPlayList.getAyaAudioFile(downloadDir: File): File {
    return getAyaFile(downloadDir, reciter.id, surahOrder, order.orderId)
}

internal fun AyaPlayList.getSurahDirectory(downloadDir : File): File =
    getSurahDirectory(downloadDir, reciter.id, surahOrder)

internal fun AyaPlayList.getReciterDirectory(downloadDir: File): File =
    getReciterDirectory(downloadDir, reciter.id)

internal fun getAyaFile(downloadDir: File, reciterId: Long, surahId : Long, ayaId : Long) : File =
    getAyaFile(downloadDir, reciterId.toString(), surahId.toString(), ayaId.toString())

internal fun getAyaFile(downloadDir: File, reciterId: String, surahId : String, ayaId : String) : File =
    File(getSurahDirectory(downloadDir, reciterId, surahId), "$ayaId$FILE_EXTENSION_MP3")

internal fun getSurahDirectory(downloadDir: File, reciterId: Long, surahId : Long) : File =
    getSurahDirectory(downloadDir, reciterId.toString(), surahId.toString())

internal fun getSurahDirectory(downloadDir: File, reciterId: String, surahId : String) : File =
    File(getReciterDirectory(downloadDir, reciterId), surahId)

internal fun getReciterDirectory(downloadDir: File, reciterId: Long) : File =
    getReciterDirectory(downloadDir, reciterId.toString())

internal fun getReciterDirectory(downloadDir : File, reciterId : String) : File =
    File(downloadDir, reciterId)

internal const val FILE_EXTENSION_MP3 = ".mp3"