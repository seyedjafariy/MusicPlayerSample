package com.worldsnas.mediasessionsample

import android.os.Parcelable
import com.worldsnas.mediasessionsample.AyaMediaItem.Companion.STARTING_AYA_ORDER_ID
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
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


    @Serializable
    sealed class StartingAya(val orderId: Long) : Parcelable {

        @Serializable
        @Parcelize
        data class Id(val id: Long) : StartingAya(id)

        @Serializable
        @Parcelize
        object Beginning : StartingAya(STARTING_AYA_ORDER_ID)
    }

    @Serializable
    sealed class Part : Parcelable {

        @Serializable
        @Parcelize
        data class Surah(val order: Long) : Part()

//        @Parcelize
//        data class Juz(val order: Long) : Part()

        @Serializable
        @Parcelize
        data class Aya(val surahOrder: Long) : Part()
    }

    @Serializable
    @Parcelize
    data class Reciter(
        val id: Long,
    ) : Parcelable


    fun isSameListAndReciter(item: AyaPlayList): Boolean =
        isSameList(item) && hasSameReciter(item)

    fun isSameList(item: AyaPlayList): Boolean =
        part == item.part

    fun hasSameReciter(item: AyaPlayList): Boolean =
        reciter == item.reciter
}
