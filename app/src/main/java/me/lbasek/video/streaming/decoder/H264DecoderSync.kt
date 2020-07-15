package me.lbasek.video.streaming.decoder

import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.TextureView
import me.lbasek.video.streaming.model.VideoFrameType
import kotlin.experimental.and

private const val TAG = "H264Decoder"
private const val NALU_MAXLEN = 1024 * 1024
private const val WIDTH = 1280
private const val HEIGHT = 720
private const val H264 = MediaFormat.MIMETYPE_VIDEO_AVC
private val NALU_HEADER = byteArrayOf(0, 0, 0, 1)

class H264DecoderSync(textureView: TextureView, width: Int = WIDTH, height: Int = HEIGHT) : Thread() {

    private var decoder: MediaCodec = MediaCodec.createDecoderByType(H264)
    private var format: MediaFormat = MediaFormat.createVideoFormat(H264, width, height)

    private var naluData: ByteArray = ByteArray(NALU_MAXLEN)
    private var naluDataIndex: Int = 0

    init {
        decoder.configure(format, Surface(textureView.surfaceTexture), null, 0)
        decoder.start()
    }

    private fun feedDecoder(frame: ByteArray, type: VideoFrameType) {
        while (true) {
            val bufferInfo = MediaCodec.BufferInfo()
            when (val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Log.d(TAG, "Info output buffers changed.")
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.d(TAG, "Info output format changed.")
                MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(TAG, "Info try again later")
                else -> decoder.releaseOutputBuffer(outputBufferIndex, 0)
            }

            val inputBufferIndex = decoder.dequeueInputBuffer(0)
            if (inputBufferIndex >= 0) {
                feedInputQueue(inputBufferIndex, frame, type)
                break
            }
        }
    }

    private fun feedInputQueue(
        inputBufferIndex: Int,
        frame: ByteArray,
        type: VideoFrameType
    ) {
        val dataLength = frame.size
        decoder.getInputBuffer(inputBufferIndex)?.also {
            it.clear()
            it.put(frame, 0, dataLength)
        }
        decoder.queueInputBuffer(
            inputBufferIndex,
            0,
            dataLength,
            0,
            if (type.isConfigurationFrame()) BUFFER_FLAG_CODEC_CONFIG else 0
        )
    }

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
                        feedDecoder(buffer, type)
                    }
                    naluDataIndex = 0
                }
            }

            naluDataIndex++
            position++
        }
    }
}