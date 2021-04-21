package com.worldsnas.mediasessionsample

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AyaPlayList(
    val order: StartingAya,
    val surahOrder: Long,

    val reciter: Reciter,
    val part: Part,
) : Parcelable {

    companion object {
        const val EXTRA_KEY_AYA_MEDIA_ITEM = "extra_key_aya_media_item"
    }

    sealed class StartingAya(val orderId : Long) : Parcelable{

        @Parcelize
        data class Id(val id : Long) : StartingAya(id)

        @Parcelize
        object Beginning : StartingAya(0)
    }

    sealed class Part : Parcelable {

        @Parcelize
        data class Surah(val order: Long) : Part()

//        @Parcelize
//        data class Juz(val order: Long) : Part()

        @Parcelize
        data class Aya(val surahOrder : Long) : Part()
    }

    @Parcelize
    data class Reciter(
        val id: Long,
    ) : Parcelable


    fun isSameListAndReciter(item : AyaPlayList) : Boolean =
        isSameList(item) && hasSameReciter(item)

    fun isSameList(item : AyaPlayList) : Boolean =
        part == item.part

    fun hasSameReciter(item : AyaPlayList) : Boolean =
        reciter == item.reciter
}
