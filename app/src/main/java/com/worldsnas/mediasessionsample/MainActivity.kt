package com.worldsnas.mediasessionsample

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            unzip()
//            startService(Intent(this, PlayerService::class.java).apply {
//                action = PlayerAction.Play.action
//                putExtra(EXTRA_KEY_AYA_MEDIA_ITEM, AyaPlayList(
//                    AyaPlayList.StartingAya.Beginning,
//                    1,
//                    AyaPlayList.Reciter(1),
//                    AyaPlayList.Part.Surah(1)
//                ))
//            })

//            startService(Intent(this, PlayerService::class.java))
        }
        Build.VERSION_CODES.Q
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun unzip() {
//        val gzipSource = assets.open("surah.zip").source().gzip()
//        gzipSource.buffer().buffer.use { buffer ->
//            File(getExternalFilesDir(null)!!, "someFile").apply {
//                if (exists()) {
//                    delete()
//                }
//
//                createNewFile()
//            }.sink().use {
//                it.write(buffer, buffer.size)
//            }
//        }


//        unzip(assets.open("surah.zip"), getExternalFilesDir(null)!!)

        Log.d("test", "finished")

    }


}


