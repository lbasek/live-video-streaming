package me.lbasek.video.streaming.ui

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import me.lbasek.video.streaming.R
import me.lbasek.video.streaming.decoder.H264DecoderAsync
import me.lbasek.video.streaming.decoder.H264DecoderSync
import java.net.*

private const val TAG = "MainActivity"
internal const val TCP_BUFFER_SIZE = 16 * 1024

class MainActivity : AppCompatActivity(){

    private var tcpSocket: ServerSocket? = null

    private var communicationSocket: Socket? = null

    lateinit var h264DecoderSync: H264DecoderSync

    lateinit var h264DecoderAsync: H264DecoderAsync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSyncDecoder()
        initAsyncDecoder()

        Log.i(TAG, localIP().toString())
        ipAddressTextView.text = localIP().toString()

        try {
            tcpSocket = ServerSocket(9090).apply {
                reuseAddress = true
                soTimeout = 0
            }
        } catch (exception: Exception) {
            Log.e(TAG, exception.localizedMessage!!)
        }

        Thread(Runnable {
            while (true) {
                communicationSocket = tcpSocket?.accept()?.apply {
                    reuseAddress = true
                    soTimeout = 0
                }

                val data = ByteArray(TCP_BUFFER_SIZE)

                while (communicationSocket != null && communicationSocket!!.isConnected) {
                    val dataLength = communicationSocket!!.getInputStream().read(data)
                    if (dataLength == -1) break

                    val packageData = data.sliceArray(IntRange(0, dataLength - 1))

                    if (::h264DecoderSync.isInitialized) {
                        h264DecoderSync.pushData(packageData)
                    }

                    if (::h264DecoderAsync.isInitialized) {
                        h264DecoderAsync.pushData(packageData)
                    }
                }
            }
        }).start()
    }

    private fun initAsyncDecoder() {
        asyncTextureView.surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture?,
                            width: Int,
                            height: Int
                    ) {
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                        return false
                    }

                    override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture?,
                            width: Int,
                            height: Int
                    ) {
                        h264DecoderAsync =
                                H264DecoderAsync(surfaceTexture = asyncTextureView.surfaceTexture)
                    }
                }
    }

    private fun initSyncDecoder() {
        syncTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture?,
                    width: Int,
                    height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture?,
                    width: Int,
                    height: Int
            ) {
                if (::h264DecoderSync.isInitialized.not()) {
                    h264DecoderSync = H264DecoderSync(syncTextureView)
                    h264DecoderSync.start()
                }
            }
        }
    }

    private fun localIP(): InetAddress {
        val networkInterfaces = NetworkInterface.getNetworkInterfaces()
        while (networkInterfaces.hasMoreElements()) {
            val networkInterface = networkInterfaces.nextElement()
            val address = networkInterface.inetAddresses
            while (address.hasMoreElements()) {
                val tmpAddress = address.nextElement()
                if (!tmpAddress.isLoopbackAddress && tmpAddress is Inet4Address) {
                    return tmpAddress
                }
            }
        }
        throw IllegalStateException("IP Address not found!")
    }
}