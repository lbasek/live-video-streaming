# Live Video Streaming

This is example how to stream [H264](https://en.wikipedia.org/wiki/Advanced_Video_Coding) encoded video with [FFmpeg](https://ffmpeg.org/) framework and how decode H264 video on Android phone using [MediaCodec API](https://developer.android.com/reference/android/media/MediaCodec.).

![alt text](https://github.com/lbasek/live-video-streaming/blob/master/result.png)

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

## Media Codec Schema

![alt text](https://github.com/lbasek/live-video-streaming/blob/master/media-codec-schema.png)

# License
```
Copyright (c) 2020 Luka Basek

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
