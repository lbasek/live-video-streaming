# Live Video Streaming

This is example how to stream [H264](https://en.wikipedia.org/wiki/Advanced_Video_Coding) encoded video with [FFmpeg](https://ffmpeg.org/) framework and how decode H264 video on Android phone using [MediaCodec API](https://developer.android.com/reference/android/media/MediaCodec.).



Requirements (only macOS):
1. Install [Brew](https://brew.sh/)
2. Install FFmpeg framework with Brew `brew install ffmpeg`
3. Run Android application
4. Connect your development machine and android device into the same network
5. Run video stream over TCP protocol:
```
ffmpeg -re \
-i videoplayback.mp4 \
-vcodec h264_videotoolbox \
-b:v 4M -maxrate 6M -bufsize 2M \
-pix_fmt yuv420p \
-tune zerolatency \
-f h264 tcp://192.168.0.32:9090
```

or stream from your front camera (be careful, you need grant camera permission on macOS)

```
ffmpeg -f avfoundation -video_size 1280x720 -framerate 30 -i "0" \
-vcodec h264_videotoolbox \
-b:v 4M -maxrate 6M -bufsize 2M \
-pix_fmt yuv420p \
-tune zerolatency \
-f h264 tcp://192.168.0.32:9090
```

![alt text](https://github.com/lbasek/live-video-streaming/blob/master/media-codec-schema.png)



