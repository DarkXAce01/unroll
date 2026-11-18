package io.github.darkxace01.unroll.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.Surface
import io.github.darkxace01.unroll.model.Look
import io.github.darkxace01.unroll.model.PhotoItem
import io.github.darkxace01.unroll.model.Project
import io.github.darkxace01.unroll.model.ScrollPlan
import io.github.darkxace01.unroll.model.Timeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * H.264 from frames we draw ourselves: the encoder's input surface gives a hardware canvas, the renderer
 * draws into it, and frames are stamped at a steady frame rate however long each took to draw.
 */
class VideoEncoder(private val file: File, val width: Int, val height: Int, private val fps: Int = 30, bitrate: Int = width * height * 6) {
    private val codec: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
    private val surface: Surface
    private val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    private val info = MediaCodec.BufferInfo()
    private var track = -1
    private var written = 0L

    init {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            // Baseline: no B-frames, so frames come out in the order they went in and our timestamps hold.
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = codec.createInputSurface()
        codec.start()
    }

    fun frame(draw: (Canvas) -> Unit) {
        val c = surface.lockHardwareCanvas()
        try { draw(c) } finally { surface.unlockCanvasAndPost(c) }
        drain(false)
    }

    fun finish() {
        codec.signalEndOfInputStream()
        drain(true)
        codec.stop(); codec.release(); surface.release()
        if (track >= 0) muxer.stop()
        muxer.release()
    }

    fun abandon() {
        runCatching { codec.stop() }; runCatching { codec.release() }; runCatching { surface.release() }
        runCatching { muxer.release() }
        file.delete()
    }

    private fun drain(end: Boolean) {
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, if (end) 10_000 else 0)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!end) return
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track = muxer.addTrack(codec.outputFormat); muxer.start() }
                idx >= 0 -> {
                    val buf = codec.getOutputBuffer(idx)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && track >= 0) {
                        info.presentationTimeUs = written * 1_000_000L / fps
                        written++
                        buf.position(info.offset); buf.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buf, info)
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }
}

/** Scroll videos for Reels/Stories, and animated slides for carousels. */
class VideoExporter(private val context: Context, private val photos: Photos, private val renderer: Renderer, private val exporter: Exporter) {
    val fps = 30

    /** Photos for video: sharp enough for 1080, capped so twenty of them fit in memory together. */
    private suspend fun loadAll(p: Project): Map<String, Bitmap?> {
        val out = HashMap<String, Bitmap?>()
        for (ph in p.items.filterIsInstance<PhotoItem>().filterNot { it.isEmpty }) {
            if (ph.uri !in out) out[ph.uri] = photos.load(ph.uri, exporter.neededSide(ph).coerceAtMost(1440), useCache = false)
        }
        for (t in p.items.filterIsInstance<io.github.darkxace01.unroll.model.TextItem>()) {
            val u = t.fill ?: continue
            if (u !in out) out[u] = photos.load(u, exporter.fillSide(t).coerceAtMost(1440), useCache = false)
        }
        return out
    }

    /**
     * The whole carousel as one video gliding across it. [reel] frames it 9:16 with the canvas centred
     * (what Reels and Stories want); otherwise it's the post's own shape.
     */
    suspend fun scroll(p: Project, reel: Boolean, plan: ScrollPlan = ScrollPlan(p.slides), onProgress: (Float) -> Unit = {}): Uri = withContext(Dispatchers.Default) {
        val w = p.slideW
        val h = if (reel) 1920 else p.slideH
        val bitmaps = loadAll(p)
        val tmp = File(context.cacheDir, "scroll-${System.currentTimeMillis()}.mp4")
        val enc = VideoEncoder(tmp, w, h, fps)
        try {
            val frames = (plan.duration * fps).toInt()
            val top = (h - p.slideH) / 2f
            for (i in 0 until frames) {
                coroutineContext.ensureActive()
                val t = i / fps.toFloat()
                val look = Timeline.scrollLook(p, plan, t)
                val x = plan.position(t) * p.slideW
                enc.frame { c ->
                    // Above and below the post in a reel: the project's own background.
                    renderer.drawBackground(c, p, w.toFloat(), h.toFloat())
                    c.save()
                    c.translate(-x, top)
                    c.clipRect(x, 0f, x + w, p.slideH.toFloat())
                    renderer.draw(c, p, { bitmaps[it] }, look = look)
                    c.restore()
                }
                if (i % 10 == 0) onProgress(i / frames.toFloat())
            }
            enc.finish()
        } catch (e: Throwable) {
            enc.abandon(); throw e
        }
        onProgress(1f)
        publish(tmp, "${name(p)} ${if (reel) "reel" else "scroll"}", p)
    }

    /**
     * The carousel with movement: slides where something animates become short clips, the rest stay
     * photos. Instagram carousels take both, in order.
     */
    suspend fun animatedSlides(p: Project, onProgress: (Float) -> Unit = {}): List<Uri> = withContext(Dispatchers.Default) {
        val moving = Timeline.animatedSlides(p)
        val length = Timeline.slideLength(p)
        val bitmaps = if (moving.isEmpty()) emptyMap() else loadAll(p)
        val out = mutableListOf<Uri>()
        for (s in 0 until p.slides) {
            coroutineContext.ensureActive()
            if (s !in moving) {
                out += exporter.exportSlides(p, listOf(s)).first()
            } else {
                val tmp = File(context.cacheDir, "slide-$s-${System.currentTimeMillis()}.mp4")
                val enc = VideoEncoder(tmp, p.slideW, p.slideH, fps)
                try {
                    val frames = (length * fps).toInt()
                    for (i in 0 until frames) {
                        val t = i / fps.toFloat()
                        val look: (io.github.darkxace01.unroll.model.Item) -> Look = Timeline.slideLook(p, s, t, length)
                        enc.frame { c ->
                            c.translate(-s * p.slideW.toFloat(), 0f)
                            renderer.draw(c, p, { bitmaps[it] }, look = look)
                        }
                    }
                    enc.finish()
                } catch (e: Throwable) {
                    enc.abandon(); throw e
                }
                out += publish(tmp, "${name(p)} ${(s + 1).toString().padStart(2, '0')}", p)
            }
            onProgress((s + 1) / p.slides.toFloat())
        }
        out
    }

    private fun name(p: Project) = p.name.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().ifEmpty { "Unroll" }

    private fun publish(tmp: File, name: String, p: Project): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Unroll/${name(p)}")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values) ?: error("Couldn't save the video")
        resolver.openOutputStream(uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
        tmp.delete()
        values.clear()
        values.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
