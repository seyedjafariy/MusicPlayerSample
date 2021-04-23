package com.worldsnas.mediasessionsample

import android.content.SharedPreferences
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
    private val downloadDir: File,
    private val playerView: PlayerView,
    private val client: OkHttpClient,
    private val sharedPreferences: SharedPreferences,
) {

    fun getPlayListItems(playList: AyaPlayList) {
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

                //TODO play single aya
            }
            is AyaPlayList.Part.Surah -> {
                val surahDir = playList.getSurahDirectory(downloadDir)

                if (!surahDir.exists()) {
                    downloadAndPlay(playList)
                    return
                }


                //TODO check if surah has all of the ayas
                // yes -> play all at the playList.order index
                // no re-download the surah again

            }
        }
    }

    private fun downloadAndPlay(playList: AyaPlayList) = with(playList) {
        downloadAndUnZipSurah(reciter.id.toString(), surahOrder.toString())

        when (part) {
            is AyaPlayList.Part.Aya -> {
                playSingleAya(reciter.id, surahOrder, order.orderId)
            }
            is AyaPlayList.Part.Surah -> {
                playSurah(reciter.id, surahOrder, order.orderId)
            }
        }

        //TODO store playList in SharedPreferences for future BrowserService lookups
    }

    private fun downloadAndUnZipSurah(reciterId: String, surahOrder: String) {
        val reciteDirectory = File(downloadDir, reciterId)

        if (!reciteDirectory.exists()) {
            reciteDirectory.mkdirs()
        }

        val downloadedSurahZip = File(reciteDirectory, "$surahOrder.zip")
        if (!downloadedSurahZip.exists())
            downloadSurahZip(reciterId, surahOrder)

        // we should unzip it now
        unzip(downloadedSurahZip, File(reciteDirectory, surahOrder))

        //we delete to avoid the extra space
        downloadedSurahZip.delete()
    }

    private fun downloadSurahZip(reciterId: String, surahOrder: String) {
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
            val request = Request.Builder().url("").build()

            val response = client.newCall(request).execute()
            val body = response.body ?: TODO("handle errors")
            val contentLength = body.contentLength()

            source = body.source()
            sink = tempReciteFile.sink().buffer().buffer

            var totalRead = 0L
            var bufferSize: Long = 8 * 1024
            var bytesRead = 0L;

            bytesRead = source.read(sink, bufferSize)
            while (bytesRead != -1L) {
                sink.emit();
                totalRead += bytesRead;
                val progress = ((totalRead * 100) / contentLength).toInt()

                //TODO publish progress
//            subscriber.onNext(progress);


                bytesRead = source.read(sink, bufferSize)
            }
            sink.flush();
        } catch (e: IOException) {
            e.printStackTrace();
            //TODO handle download errors
        } finally {
            source?.closeQuietly()
            sink?.closeQuietly()
        }

        //rename the file so we know it's been downloaded completely
        tempReciteFile.renameTo(File(tempReciteFile.absolutePath.replace(PREFIX_ZIP_FILE_TEMP, "")))

    }

    private fun unzip(zipFile: File, targetDirectory: File) {
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

        //TODO start playing at specified location in PlayList
        playerView.loadAndPlay(ayas)
    }

    private fun getAyaFiles(reciterId: Long, surahOrder: Long): List<File> {
        return getSurahDirectory(downloadDir, reciterId, surahOrder).listFiles()!!.toList()
    }
}

private const val PREFIX_ZIP_FILE_TEMP = "-temp"