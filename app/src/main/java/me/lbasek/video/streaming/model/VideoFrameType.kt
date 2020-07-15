package me.lbasek.video.streaming.model

sealed class VideoFrameType(val id: Int, val name: String) {

    object Idr : VideoFrameType(5, "IDR")
    object Sei : VideoFrameType(6, "SEI")
    object Sps : VideoFrameType(7, "SPS")
    object Pps : VideoFrameType(8, "PPS")
    object NonIdr : VideoFrameType(1, "NON_IDR")
    object Unknown : VideoFrameType(-1, "UNKNOWN")

    companion object {
        fun videoTypeById(id: Int): VideoFrameType {
            return when (id) {
                Sps.id -> Sps
                Pps.id -> Pps
                Idr.id -> Idr
                NonIdr.id -> NonIdr
                Sei.id -> Sei
                else -> Unknown
            }
        }
    }

    fun isConfigurationFrame(): Boolean = this == Sps || this == Pps || this == Sei
}