package me.lbasek.video.streaming.decoder

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG
import android.media.MediaCodec.CodecException
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import me.lbasek.video.streaming.model.VideoFrameType
import java.util.concurrent.LinkedBlockingDeque
import kotlin.experimental.and

private const val TAG = "DecodeFramesTask"
private const val NALU_MAXLEN = 1024 * 1024
private const val WIDTH = 1280
private const val HEIGHT = 720
private const val H264 = MediaFormat.MIMETYPE_VIDEO_AVC
private val NALU_HEADER = byteArrayOf(0, 0, 0, 1)

class H264DecoderAsync(
    videoHeight: Int = HEIGHT,
    videoWidth: Int = WIDTH,
    surfaceTexture: SurfaceTexture
) {

    private var mediaCodec: MediaCodec

    private val freeInputBuffers = LinkedBlockingDeque<Int>()

    private var naluData: ByteArray = ByteArray(NALU_MAXLEN)
    private var naluDataIndex: Int = 0

    init {
        val format = MediaFormat.createVideoFormat(H264, videoWidth, videoHeight)
        mediaCodec = MediaCodec.createDecoderByType(H264)

        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                freeInputBuffers.add(index)
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                try {
                    codec.releaseOutputBuffer(index, true)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, e.localizedMessage ?: "Something went wrong.")
                }
            }

            override fun onError(codec: MediaCodec, e: CodecException) {
                Log.e(TAG, e.localizedMessage ?: "Something went wrong.")
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.i(TAG, "onOutputFormatChanged")
            }
        })

        mediaCodec.configure(format, Surface(surfaceTexture), null, 0)
        mediaCodec.start()
    }

    /**
     * Enqueue video frame for processing
     *
     * @param frame video frame
     */
    fun pushData(data: ByteArray) {
        var position = 0

        while (position < data.size) {

            naluData[naluDataIndex] = data[position]

            if (naluDataIndex == NALU_MAXLEN - 1) {
                Log.e(TAG, "NALU Overflow")
                naluDataIndex = 0
            }

            if (position + 4 < data.size) {
                if (data.sliceArray(IntRange(position, position + 3)).contentEquals(NALU_HEADER)) {
                    val buffer = naluData.sliceArray(IntRange(0, naluDataIndex - 1))

                    if (buffer.isNotEmpty()) {

                        val type = VideoFrameType.videoTypeById((buffer[4] and 0x1F).toInt())
                        Log.i(TAG, type.name)
                        if (type == VideoFrameType.Unknown) return

                        freeInputBuffers.poll()?.let {
                            val inputBuffer = mediaCodec.getInputBuffer(it)
                            inputBuffer?.clear()
                            inputBuffer?.put(buffer)

                            mediaCodec.queueInputBuffer(
                                it,
                                0,
                                buffer.size,
                                0,
                                if (type.isConfigurationFrame()) BUFFER_FLAG_CODEC_CONFIG else 0
                            )
                        }
                    }
                    naluDataIndex = 0
                }
            }

            naluDataIndex++
            position++
        }
    }


    /**
     * Free up resources used by the codec instance.
     * Make sure you call this when you're done to free up any opened component instance
     * instead of relying on the garbage collector to do this for you at some point in the future.
     */
    fun releaseDecodeFrameTask() {
        mediaCodec.release()
    }

}