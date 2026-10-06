package com.animejapaneselab.nativeapp.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import com.animejapaneselab.nativeapp.MainActivity
import com.animejapaneselab.nativeapp.R
import com.animejapaneselab.nativeapp.ui.radio.RadioPlayer

/**
 * Keeps 知識电台 playing with the screen off (午睡 / 散歩) and puts it on the lock screen and the
 * headset buttons. It only mirrors [RadioPlayer]: [sync] after every change.
 */
class RadioService : Service() {
    private var session: MediaSession? = null
    private var portrait: Bitmap? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        RadioPlayer.init(this)
        session = MediaSession(this, "radio").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = RadioPlayer.resume()
                override fun onPause() = RadioPlayer.pause()
                override fun onSkipToNext() = RadioPlayer.next()
                override fun onSkipToPrevious() = RadioPlayer.previous()
                override fun onStop() = RadioPlayer.stop()
            })
            isActive = true
        }
        portrait = runCatching {
            BitmapFactory.decodeResource(resources, R.drawable.rezero_emilia_character, BitmapFactory.Options().apply { inSampleSize = 4 })
        }.getOrNull()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionToggle -> RadioPlayer.toggle()
            ActionNext -> RadioPlayer.next()
            ActionStop -> RadioPlayer.stop()
        }
        // startForegroundService must always be answered with startForeground, even when stopping.
        val n = build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Id, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(Id, n)
        }
        if (!RadioPlayer.state.value.active) refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        instance = null
        session?.release()
        session = null
        super.onDestroy()
    }

    private fun refresh() {
        if (!RadioPlayer.state.value.active) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        getSystemService(NotificationManager::class.java)?.notify(Id, build())
    }

    private fun build(): Notification {
        val s = RadioPlayer.state.value
        val track = RadioPlayer.track()
        val title = track?.let { "${it.no} ${it.title}" } ?: "知識电台"
        val text = when {
            s.recap -> "只念日语 · 午睡"
            track != null -> track.shelf
            else -> ""
        }
        session?.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "エミリア")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, text)
                .apply { portrait?.let { putBitmap(MediaMetadata.METADATA_KEY_ART, it) } }
                .build(),
        )
        session?.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP)
                .setState(if (s.playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
        ensureChannel()
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, Channel)
            .setSmallIcon(StudyReminder.sealIcon(this))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(s.playing)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .apply { portrait?.let { setLargeIcon(it) } }
            .addAction(action(if (s.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (s.playing) "暂停" else "播放", ActionToggle))
            .addAction(action(android.R.drawable.ic_media_next, "下一段", ActionNext))
            .addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "停止", ActionStop))
            .setStyle(Notification.MediaStyle().setMediaSession(session?.sessionToken).setShowActionsInCompactView(0, 1))
            .build()
    }

    private fun action(icon: Int, label: String, name: String): Notification.Action {
        val pi = PendingIntent.getService(this, name.hashCode(), Intent(this, RadioService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Action.Builder(Icon.createWithResource(this, icon), label, pi).build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(Channel) != null) return
        nm.createNotificationChannel(NotificationChannel(Channel, "知識电台", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
    }

    companion object {
        private const val Id = 4107
        private const val Channel = "radio"
        private const val ActionToggle = "radio.toggle"
        private const val ActionNext = "radio.next"
        private const val ActionStop = "radio.stop"
        @Volatile private var instance: RadioService? = null

        /** Starts the service when the radio becomes active, updates or stops it after. */
        fun sync(context: Context) {
            val running = instance
            when {
                running != null -> running.refresh()
                RadioPlayer.state.value.active -> runCatching {
                    context.startForegroundService(Intent(context, RadioService::class.java))
                }
            }
        }
    }
}
