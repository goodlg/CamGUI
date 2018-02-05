package org.ftd.gyn.camlibrary;

import java.lang.ref.WeakReference;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class MyCamera {
    private static final String TAG = "MyCam";

    private static final int RAWCAM_MSG_ERROR            = 0x001;
    private static final int RAWCAM_MSG_SHUTTER          = 0x002;
    private static final int RAWCAM_MSG_FOCUS            = 0x004;
    private static final int RAWCAM_MSG_ZOOM             = 0x008;
    private static final int RAWCAM_MSG_PREVIEW_FRAME    = 0x010;
    private static final int RAWCAM_MSG_VIDEO_FRAME      = 0x020;
    private static final int RAWCAM_MSG_POSTVIEW_FRAME   = 0x040;
    private static final int RAWCAM_MSG_RAW_IMAGE        = 0x080;
    private static final int RAWCAM_MSG_COMPRESSED_IMAGE = 0x100;

    private long mNativeContext;
    private EventHandler mEventHandler;
    private PreviewCallback mPreviewCallback;
    private AutoFocusCallback mAutoFocusCallback;
    private OnZoomChangeListener mZoomListener;
    private ErrorCallback mErrorCallback;
    private boolean mOneShot;
    private boolean mWithBuffer;

    public static MyCamera open(int cameraId) {
        return new MyCamera(cameraId);
    }

    public static void apiInit() {
        Log.d(TAG, "apiInit");
        ApiInit();
    }

    public static void apiDeinit() {
        ApiDeinit();
    }

    private MyCamera(int cameraId) {
    	Looper looper;

        mPreviewCallback = null;
        mAutoFocusCallback = null;
        mZoomListener = null;
        mErrorCallback = null;

        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }

        native_setup(new WeakReference<MyCamera>(this), cameraId);
    }

    public static native final int ApiInit();
    public static native final int ApiDeinit();
    public native final int native_setup(Object cam_this, int cameraId);
    public native final int native_release();

    @Override
    protected void finalize() {
        release();
    }

    public final void release() {
        native_release();
    }

    public interface PreviewCallback {
        void onPreviewFrame(byte[] data, MyCamera rawCam);
    };

    public interface AutoFocusCallback {
        void onAutoFocus(boolean success, MyCamera rawCam);
    }

    public interface OnZoomChangeListener {
        void onZoomChange(int zoomValue, boolean stopped, MyCamera rawCam);
    };

    public interface ErrorCallback {
        void onError(int error, MyCamera rawCam);
    };

	private class EventHandler extends Handler {
        private final MyCamera mCam;

        public EventHandler(MyCamera r, Looper looper) {
            super(looper);
            mCam = r;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case RAWCAM_MSG_SHUTTER:
            	// not support 
                return;

            case RAWCAM_MSG_RAW_IMAGE:

                return;

            case RAWCAM_MSG_COMPRESSED_IMAGE:

                return;

            case RAWCAM_MSG_PREVIEW_FRAME:
                if (mPreviewCallback != null) {
                	PreviewCallback cb = mPreviewCallback;
                    cb.onPreviewFrame((byte[])msg.obj, mCam);
                }
                return;

            case RAWCAM_MSG_FOCUS:
                if (mAutoFocusCallback != null) {
                    mAutoFocusCallback.onAutoFocus(msg.arg1 == 0 ? false : true, mCam);
                }
                return;

            case RAWCAM_MSG_ZOOM:
                if (mZoomListener != null) {
                    mZoomListener.onZoomChange(msg.arg1, msg.arg2 != 0, mCam);
                }
                return;

            case RAWCAM_MSG_VIDEO_FRAME:

                return;

            case RAWCAM_MSG_ERROR :
                Log.e(TAG, "Error " + msg.arg1);
                if (mErrorCallback != null) {
                    mErrorCallback.onError(msg.arg1, mCam);
                }
                return;

            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }

    private static void postEventFromNative(Object camera_ref,
                                            int what, int arg1, int arg2, Object obj)
    {
        MyCamera c = (MyCamera)((WeakReference)camera_ref).get();
        if (c == null)
            return;

        if (c.mEventHandler != null) {
            Message m = c.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            c.mEventHandler.sendMessage(m);
        }
    }
}
