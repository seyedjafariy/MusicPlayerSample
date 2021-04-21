package com.worldsnas.mediasessionsample

import android.content.SharedPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class PlaylistManager(
    private val downloadDir : File,
    private val playerView : PlayerView,
    private val client : OkHttpClient,
    private val sharedPreferences: SharedPreferences,
) {

    fun getPlayListItems(playList : AyaPlayList){
        val reciterDir = File(downloadDir, playList.reciter.id.toString())

        if(!reciterDir.exists()){

            //TODO start downloading the recites
            return
        }

        when (playList.part) {
            is AyaPlayList.Part.Aya -> {
                val surahDir = File(reciterDir, playList.part.surahOrder.toString())

                if (!surahDir.exists()){
                    // TODO download
                    return
                }

                val audioFile = File(surahDir, playList.order.orderId.toString() + ".mp3")

                if(!audioFile.exists()){
                    // TODO download

                    return
                }

                //TODO play single aya
            }
            is AyaPlayList.Part.Surah -> {
                val surahDir = File(reciterDir, playList.part.order.toString())

                if (!surahDir.exists()){
                    // TODO download
                    return
                }


                //TODO check if surah has all of the ayas
                // yes -> play all at the playList.order index
                // no re-download the surah again

            }
        }
    }

    private fun downloadRecite(playList : AyaPlayList){

//        Request.Builder()
//            .
//        client.newCall()


    }
}