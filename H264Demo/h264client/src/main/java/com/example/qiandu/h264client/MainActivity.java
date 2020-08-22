package com.example.qiandu.h264client;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private final static String TAG = "H264Client";

    private final static String MIME_TYPE = "video/avc";

    private final static int VIDEO_WIDTH = 720;
    private final static int VIDEO_HEIGHT = 1280;
    private SurfaceView mSurfaceView;
    private MediaCodec mCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mSurfaceView.getHolder().setFixedSize(dm.widthPixels, dm.heightPixels);
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    public void start(View view) {
        startServer1();
        initDecoder();
    }

    InputStream is;

    private void startServer1() {
                try {
//                    URI url = new URI("ws://192.168.0.103:16886");
//                    URI url = new URI("ws://192.168.9.120:16886");
                    URI url = new URI("ws://192.168.1.100:16886");
                    MyWebSocketClient myWebSocketClient = new MyWebSocketClient(url);
                    myWebSocketClient.connectBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                }
    }
    private class MyWebSocketClient extends WebSocketClient {

        public MyWebSocketClient(URI serverURI) {
            super(serverURI);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            Log.v(TAG, "onOpen    ");
        }

        @Override
        public void onMessage(String s) {
            Log.v(TAG, "onMessage    " + s);
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            byte[] buf = new byte[bytes.remaining()];
            bytes.get(buf);
            onFrame(buf, 0, buf.length);

        }

        @Override
        public void onClose(int i, String s, boolean b) {
            Log.v(TAG, "onClose    ");
        }

        @Override
        public void onError(Exception e) {
            Log.v(TAG, "onError    " + e);
        }
    }

    public void initDecoder() {
        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);

            final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
            format.setInteger(MediaFormat.KEY_BIT_RATE,  VIDEO_WIDTH * VIDEO_HEIGHT);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            // 横屏
//            byte[] header_sps = {0, 0, 0, 1, 103, 66, -128, 31, -38, 1, 64, 22, -24, 6, -48, -95, 53};
//            byte[] header_pps = {0, 0 ,0, 1, 104, -50, 6, -30};

            // 竖屏
            byte[] header_sps = {0, 0, 0, 1, 103, 66, -128, 31, -38, 2, -48, 40, 104, 6, -48, -95, 53};
            byte[] header_pps = {0, 0 ,0, 1, 104, -50, 6, -30};
//
            format.setByteBuffer("csd-0", ByteBuffer.wrap(header_sps));
            format.setByteBuffer("csd-1", ByteBuffer.wrap(header_pps));
            mCodec.configure(format, mSurfaceView.getHolder().getSurface(),
                    null, 0);
            mCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    int mCount = 0;
    public boolean onFrame(byte[] buf, int offset, int length) {
        // Get input buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        int inputBufferIndex = mCodec.dequeueInputBuffer(100);
//        Log.v(TAG, " inputBufferIndex  " + inputBufferIndex);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0);
//            mCount++;
        } else {
            return false;
        }
        // Get output buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);


        while (outputBufferIndex >= 0) {
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        return true;
    }
}
