package com.worldsnas.mediasessionsample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerControlView

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    lateinit var controller: PlayerControlView

    private val playerListener = object : PlayerManager.PlayerInstanceEventListener {
        override fun playerCreated(player: SimpleExoPlayer) {
            PlayerManager.removeListener(this)

            controller.player = player
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller = view.findViewById(R.id.playerController)
        controller.setShowMultiWindowTimeBar(true)
        controller.showTimeoutMs = 0

        if (PlayerManager.isInitialized()) {
            controller.player = PlayerManager.getInstance()
        } else {
            PlayerManager.addListener(playerListener)
        }
    }

    override fun onStop() {
        super.onStop()
        PlayerManager.removeListener(playerListener)
    }

}