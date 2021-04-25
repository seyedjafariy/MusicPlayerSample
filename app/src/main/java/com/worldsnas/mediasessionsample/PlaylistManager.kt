package com.worldsnas.mediasessionsample

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.BufferedSource
import okio.Sink
import okio.buffer
import okio.sink
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PlaylistManager(
    scope: CoroutineScope,
    private val downloadDir: File,
    private val playerView: PlayerView,
    private val client: OkHttpClient,
    private val sharedPreferences: SharedPreferences,
) : CoroutineScope by scope + SupervisorJob() {

    fun loadAndPlay(playList: AyaPlayList) {
        when (playList.part) {
            is AyaPlayList.Part.Aya -> {
                val surahDir = playList.getSurahDirectory(downloadDir)

                if (!surahDir.exists()) {
                    downloadAndPlay(playList)
                    return
                }

                val audioFile = playList.getAyaAudioFile(downloadDir)

                if (!audioFile.exists()) {
                    downloadAndPlay(playList)
                    return
                }

                playlistReady(playList)
            }
            is AyaPlayList.Part.Surah -> {
                val surahDir = playList.getSurahDirectory(downloadDir)

                if (!surahDir.exists()) {
                    downloadAndPlay(playList)
                    return
                }

                playlistReady(playList)
            }
        }
    }

    private fun downloadAndPlay(playList: AyaPlayList) = launch {
        with(playList) {
            //TODO handle concurrency
            // ex:. if we receive a play command and immediately receive another one
            // we should decide what we should do with previous process
            downloadAndUnZipSurah(reciter.id.toString(), surahOrder.toString())

            playlistReady(playList)
        }
    }

    private fun playlistReady(playList: AyaPlayList) = with(playList) {
        when (part) {
            is AyaPlayList.Part.Aya -> {
                playSingleAya(reciter.id, surahOrder, order.orderId)
            }
            is AyaPlayList.Part.Surah -> {
                playSurah(reciter.id, surahOrder, order.orderId)
            }
        }

        //storing the last played
        sharedPreferences.edit(false) {
            putString(KEY_LAST_PLAYLIST, json.encodeToString(playList))
        }
    }

    private suspend fun downloadAndUnZipSurah(reciterId: String, surahOrder: String) =
        withContext(Dispatchers.IO) {
            val reciteDirectory = File(downloadDir, reciterId)

            if (!reciteDirectory.exists()) {
                reciteDirectory.mkdirs()
            }

            val downloadedSurahZip = File(reciteDirectory, "$surahOrder.zip")
            if (!downloadedSurahZip.exists())
                downloadSurahZip(reciterId, surahOrder)

            // we should unzip it now
            showDownloading("حمد", "عبدل باسط", 85)
            unzip(downloadedSurahZip, File(reciteDirectory, surahOrder))

            //we delete to avoid the extra space
            downloadedSurahZip.delete()
        }

    private suspend fun downloadSurahZip(reciterId: String, surahOrder: String) =
        withContext(Dispatchers.IO) {
            showDownloading("حمد", "عبدل باسط", null)

            val reciteDirectory = getReciterDirectory(downloadDir, reciterId)

            val tempReciteFile = File(reciteDirectory, "$surahOrder$PREFIX_ZIP_FILE_TEMP.zip")
            if (tempReciteFile.exists()) {
                //TODO support resuming
                tempReciteFile.delete()
                tempReciteFile.createNewFile()
            }

            var sink: Sink? = null
            var source: BufferedSource? = null
            try {
                val request =
                    Request.Builder()
                        .url("https://drive.google.com/uc?export=download&id=1mRlLUc1_ji1Ny-k7A-99xACgnArzk39K")
                        .build()

                val response = client.newCall(request).execute()
                val body = response.body ?: TODO("handle errors")
                val contentLength = body.contentLength()

                source = body.source()
                sink = tempReciteFile.sink().buffer().buffer

                var totalRead = 0L
                val bufferSize: Long = 8 * 1024
                var bytesRead: Long

                bytesRead = source.read(sink, bufferSize)
                while (isActive && bytesRead != -1L) {
                    sink.emit()
                    totalRead += bytesRead
                    val progress = ((totalRead * 80) / contentLength).toInt()

                    showDownloading("حمد", "عبدل باسط", progress)

                    bytesRead = source.read(sink, bufferSize)
                }
                sink.flush()
            } catch (e: IOException) {
                e.printStackTrace()
                //TODO handle download errors
            } finally {
                source?.closeQuietly()
                sink?.closeQuietly()
            }

            //rename the file so we know it's been downloaded completely
            tempReciteFile.renameTo(
                File(
                    tempReciteFile.absolutePath.replace(
                        PREFIX_ZIP_FILE_TEMP,
                        ""
                    )
                )
            )

        }

    private suspend fun unzip(zipFile: File, targetDirectory: File) = withContext(Dispatchers.IO) {
        val zis = ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile))
        )
        try {
            var ze: ZipEntry? = zis.nextEntry
            var count: Int
            val buffer = ByteArray(8192)
            while (ze != null) {
                val file = File(targetDirectory, ze.name)
                val dir = if (ze.isDirectory) file else file.parentFile
                if (!dir.isDirectory && !dir.mkdirs()) throw FileNotFoundException(
                    "Failed to ensure directory: " +
                            dir.absolutePath
                )
                if (ze.isDirectory) continue
                val fout = FileOutputStream(file)
                try {
                    while (zis.read(buffer).also { count = it } != -1) fout.write(buffer, 0, count)
                } finally {
                    fout.close()
                }
                /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
                ze = zis.nextEntry
            }
        } finally {
            zis.close()
        }
    }

    private fun playSingleAya(reciterId: Long, surahOrder: Long, ayaOrder: Long) {
        val ayaFile = getAyaFile(downloadDir, reciterId, surahOrder, ayaOrder)

        //TODO get reciter name and picture,
        // get surahName (in device locale not the app)

        playerView.loadAndPlay(
            AyaMediaItem(
                ayaFile,

                ayaOrder,
                surahOrder,
                "",
                ""
            )
        )
    }

    private fun playSurah(reciterId: Long, surahOrder: Long, startingAya: Long) {
        val ayas = getAyaFiles(reciterId, surahOrder).map {
            AyaMediaItem(
                it,
                it.nameWithoutExtension.toLong(),
                surahOrder,
                "",
                ""
            )
        }

        playerView.loadAndPlay(ayas, startingAya)
    }

    private fun getAyaFiles(reciterId: Long, surahOrder: Long): List<File> {
        return getSurahDirectory(downloadDir, reciterId, surahOrder).listFiles()!!.toList()
    }

    private suspend fun showDownloading(surahName: String, reciterName: String, progress: Int?) =
        withContext(Dispatchers.Main) {
            playerView.showDownloading(surahName, reciterName, progress)
        }
}

private val json = Json {
}

private const val PREFIX_ZIP_FILE_TEMP = "-temp"
private const val KEY_LAST_PLAYLIST = "KEY_LAST_PLAYLIST"