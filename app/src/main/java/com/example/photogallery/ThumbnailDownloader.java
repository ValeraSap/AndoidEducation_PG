package com.example.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {
	private static final String TAG = "ThumbnailDownloader";
	private static final int MESSAGE_DOWNLOAD = 0;

	private boolean mHasQuit = false;
	private Handler mRequestHandler;
	private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
	private Handler mResponseHandler;
	private ThumbnailDownloadListener<T> mTThumbnailDownloadListener;

	public interface ThumbnailDownloadListener<T> {
		void onThumbnailDownloaded(T target, Bitmap thumbnail);
	}
	public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
		mTThumbnailDownloadListener=listener;
	}

	public ThumbnailDownloader(Handler responseHandler) {
		super(TAG);
		mResponseHandler=responseHandler;

	}

	@Override
	public boolean quit() {
		mHasQuit=true;
		return super.quit();
	}

	@Override
	protected void onLooperPrepared() {
		///callback ли??
		mRequestHandler= new Handler() {
			@Override
			public void handleMessage(@NonNull Message message) {
				if(message.what==MESSAGE_DOWNLOAD)
				{
					T target = (T) message.obj;
					//Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
					handleRequest(target);
				}
			}
		};
	}
	public void clearQueue() {
		mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
		mRequestMap.clear();
	}

	private void handleRequest(T target) {
		try {
			final String url = mRequestMap.get(target);
			if(url==null) {
				return;
			}
			byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
			final Bitmap bitmap = BitmapFactory
					.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
			//Log.i(TAG, "Bitmap created");

			mResponseHandler.post(new Runnable() {
				@Override
				public void run() {
					if(mRequestMap.get(target) != url || // гарантирует, что каждый объект PhotoHolder получит правильное изображение, даже если за прошедшее время был сделан другой запрос
					mHasQuit) {
						return;
					}
					mRequestMap.remove(target);
					mTThumbnailDownloadListener.onThumbnailDownloaded(target,bitmap);
				}
			});
		} catch (IOException ioe) {
			Log.e(TAG, "Error downloading image", ioe);
		}
	}

	public void queueThumbnail (T target, String url) {
		//Log.i(TAG, "Got a URL: " + url);
		if(url==null) {
			mRequestMap.remove(target);
		}
		else {
			mRequestMap.put(target, url);
			mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target)
					.sendToTarget();
		}
	}
}
