package com.worldsnas.mediasessionsample

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import okio.BufferedSource
import okio.Sink
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PlaylistManager(
    scope: CoroutineScope,
    private val downloadDir: File,
    private val playerView: PlayerView,
    private val client: OkHttpClient,
    private val sharedPreferences: SharedPreferences,
    private val dismissCallback: () -> Unit
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

    fun getLastPlayList(): AyaPlayList {
        val lastPlayList = sharedPreferences.getString(KEY_LAST_PLAYLIST, null) ?: error(
            """
            no last playlist has been stored.
        """.trimIndent()
        )

        return json.decodeFromString(lastPlayList)
    }

    private fun downloadAndPlay(playList: AyaPlayList) = launch {
        with(playList) {
            //TODO handle concurrency
            // ex:. if we receive a play command and immediately receive another one
            // we should decide what we should do with previous process
            val downloadSuccessful =
                downloadAndUnZipSurah(reciter.id.toString(), surahOrder.toString())

            mainThreadView {
                hideDownloading()
            }

            if (downloadSuccessful) {
                playlistReady(playList)
            }
        }
    }

    private fun playlistReady(playList: AyaPlayList, play: Boolean = true) = with(playList) {
        val mediaItems = when (part) {
            is AyaPlayList.Part.Aya -> {
                listOf(getAyaMediaItem(reciter.id, surahOrder, order.orderId))
            }
            is AyaPlayList.Part.Surah -> {
                getSurahMediaItems(reciter.id, surahOrder)
            }
        }

        //storing the last played
        sharedPreferences.edit(false) {
            putString(KEY_LAST_PLAYLIST, json.encodeToString(playList))
        }

        if(play){
            playerView.loadAndPlay(mediaItems, playList.order.orderId)
        }else{
            playerView.loadAndShow(mediaItems)
        }
    }

    private suspend fun downloadAndUnZipSurah(reciterId: String, surahOrder: String): Boolean =
        withContext(Dispatchers.IO) {
            val reciteDirectory = File(downloadDir, reciterId)

            if (!reciteDirectory.exists()) {
                reciteDirectory.mkdirs()
            }

            val downloadedSurahZip = File(reciteDirectory, "$surahOrder.zip")
            if (!downloadedSurahZip.exists())
                downloadSurahZip(reciterId, surahOrder)

            //check if download was successful
            if (!downloadedSurahZip.exists()) {
                return@withContext false
            }

            // we should unzip it now
            showDownloading("حمد", "عبدل باسط", 85)
            unzip(downloadedSurahZip, File(reciteDirectory, surahOrder))

            //we delete to avoid the extra space
            downloadedSurahZip.delete()

            return@withContext true
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
                        .url("https://everyayah.com/data/Abdul_Basit_Murattal_64kbps/zips/001.zip")
                        .build()

                val response = client.newCall(request).execute()

                val body = response.body
                if (body == null) {
                    mainThreadView { cancelDownloadingAndShowFailed("حمد", "عبدلباسط") }
                    return@withContext
                }

                var inputStream: InputStream? = null;
                val output: OutputStream = BufferedOutputStream(FileOutputStream(tempReciteFile))
                try {

                    inputStream = body.byteStream()
                    val contentLength = body.contentLength()

                    val buff = ByteArray(1024 * 8)
                    var downloaded = 0L

                    while (true) {
                        val readed = inputStream.read(buff)

                        if (!isActive || readed == -1) {
                            break
                        }
                        output.write(buff, 0, readed);
                        //write buff
                        downloaded += readed;

                        val progress = ((downloaded * 80) / contentLength).toInt()

                        showDownloading("حمد", "عبدل باسط", progress)
                    }
                } finally {
                    output.flush();
                    output.close();
                    inputStream?.close()
                }

//                source = body.source()
//                sink = tempReciteFile.sink().buffer().buffer
//
//                var totalRead = 0L
//                val bufferSize: Long = 8 * 1024
//                var bytesRead: Long
//
//                bytesRead = source.read(sink, bufferSize)
//                while (isActive && bytesRead != -1L) {
//                    sink.emit()
//                    totalRead += bytesRead
//                    val progress = ((totalRead * 80) / contentLength).toInt()
//
//                    showDownloading("حمد", "عبدل باسط", progress)
//
//                    bytesRead = source.read(sink, bufferSize)
//                }
//                sink.flush()
            } catch (e: IOException) {
                e.printStackTrace()
                mainThreadView {
                    downloadingFailed()
                }
                return@withContext
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

    private fun getAyaMediaItem(reciterId: Long, surahOrder: Long, ayaOrder: Long): AyaMediaItem {
        val ayaFile = getAyaFile(downloadDir, reciterId, surahOrder, ayaOrder)

        //TODO get reciter name and picture,
        // get surahName (in device locale not the app)

        return AyaMediaItem(
            ayaFile,

            ayaOrder,
            surahOrder,
            "",
            ""
        )
    }

    private fun getSurahMediaItems(
        reciterId: Long,
        surahOrder: Long,
    ): List<AyaMediaItem> {
        return getAyaFiles(reciterId, surahOrder)
            .filter {
                it.extension == EXT_AUDIO_MP3
            }
            .map {
                AyaMediaItem(
                    it,
                    it.nameWithoutExtension.toLong(),
                    surahOrder,
                    "",
                    ""
                )
            }
    }

    private fun getAyaFiles(reciterId: Long, surahOrder: Long): List<File> {
        return getSurahDirectory(downloadDir, reciterId, surahOrder).listFiles()!!.toList()
    }

    private suspend fun showDownloading(surahName: String, reciterName: String, progress: Int?) =
        mainThreadView {
            showDownloading(surahName, reciterName, progress)
        }

    private suspend inline fun mainThreadView(crossinline block: PlayerView.() -> Unit) =
        withContext(Dispatchers.Main) {
            block(playerView)
        }

    private fun downloadingFailed(){
        playerView.cancelDownloadingAndShowFailed("حمد", "عبدلباسط")

        if (hasLastPlayed()) {
            //showLast
            loadLastPlayed()
        } else {
            //nothing to show or play. stopService
            dismissCallback()
        }
    }

    private fun loadLastPlayed() {
        playlistReady(getLastPlayList(), play= false)
    }

    fun hasLastPlayed(): Boolean =
        sharedPreferences.contains(KEY_LAST_PLAYLIST)
}

private val json = Json {
}

private const val EXT_AUDIO_MP3 = "mp3"
private const val PREFIX_ZIP_FILE_TEMP = "-temp"
private const val KEY_LAST_PLAYLIST = "KEY_LAST_PLAYLIST"