package com.yotadevices.sdk;

import com.yotadevices.sdk.Constants.VolumeButtonsEvent;
import com.yotadevices.sdk.exception.SuperNotCalledException;
import com.yotadevices.sdk.utils.InfiniteIntentService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The service which handles events to work with e-ink screen.
 * 
 * @author asazonov
 */
public abstract class BSActivity extends InfiniteIntentService {

	public static final String TAG = "BSActivity";
	private static final boolean DEBUG_BS_LIFECIRCLE = true;

	private final Object mLockActive = new Object();
	private BSDrawer mDrawer;

	private Intent mStartIntent = null;

	/** Messenger for communicating with service. */
	Messenger mService = null;
	private IDrawer mDrawerService;

	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	boolean mCalled;
	boolean isResumed;
	boolean canHandleIntent;
	boolean isBSEventEnable;

	// For initial events, if service stopped.
	private Queue<Message> mMessagesQueue = new LinkedList<Message>();

	/** Record inner state BSActivity */
	private BSRecord mRecord;

	final Handler mIncomingHandler = new BSAcivityIncomingMessagesHandler(this);

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(mIncomingHandler);

	private boolean mFullScreenMode = false;
	private int mMask = -1;

	public BSActivity() {
		super(TAG);
	}

	@Override
	final public void onCreate() {
		super.onCreate();

		mDrawer = new BSDrawer(this);
		mRecord = new BSRecord(this);
		isResumed = false;
		canHandleIntent = false;

		performBSCreate();
		doBindService();
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		doUnbindService();
		performBSDestroy();
	};

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			Log.d(TAG, "Attached.");

			sendRequestToBeActive();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			Log.d(TAG, "Disconnected.");
		}
	};

	private ServiceConnection mDrawerConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName cn) {
			mDrawerService = null;
		}

		@Override
		public void onServiceConnected(ComponentName cn, IBinder binder) {
			mDrawerService = IDrawer.Stub.asInterface(binder);
		}
	};

	private Intent getFrameworkIntent() {
		Intent i = new Intent();
		i.setClassName("com.yotadevices.framework", "com.yotadevices.framework.service.PlatinumManagerService");
		return i;
	}

	void doBindService() {
		Log.d(TAG, "Start Binding.");
		doBindDrawerService();
		mIsBound = bindService(getFrameworkIntent(), mConnection, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "Binding.");
	}

	private void doBindDrawerService() {
		Intent i = getFrameworkIntent();
		i.setAction(BSActivity.class.getName());
		bindService(i, mDrawerConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			unbindService(mDrawerConnection);
			mIsBound = false;
			Log.d(TAG, "Unbinding.");
		}
	}

	@Override
	final public void onStart(Intent intent, int startId) {
		mStartIntent = intent;
		if (intent.hasExtra(Constants.EXTRA_MESSAGE)) {
			Message msg = intent.getParcelableExtra(Constants.EXTRA_MESSAGE);
			mMessagesQueue.offer(msg);
		}

		super.onStart(intent, startId);
	}

	@Override
	final protected boolean canHandleIntent() {
		waitForActive();
		return canHandleIntent;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent");
	}

	private void waitForActive() {
		synchronized (mLockActive) {
			while (mService == null || !isResumed) {
				try {
					mLockActive.wait();
				} catch (InterruptedException unused) {
				}
			}
		}
	}

	private void unlockWaitForActive() {
		synchronized (mLockActive) {
			mLockActive.notifyAll();
		}
	}

	private void sendMessagesQueue() {
		Message m = null;
		while ((m = mMessagesQueue.poll()) != null) {
			try {
				mMessenger.send(m);
			} catch (RemoteException unused) {
			}
		}
	}

	private final void performBSCreate() {
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSCreate.");
		}
		onBSCreate();
	}

	void performBSStop() {
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSStop.");
		}

		saveInstanceState();

		mCalled = false;
		onBSStop();
		if (!mCalled) {
			throw new SuperNotCalledException("BSActivity " + getClass().getSimpleName() + " did not call through to super.onBSStop()");
		}
	}

	/**
	 * save inner state.
	 */
	private void saveInstanceState() {
		performBSSaveInstanceState(mRecord);
		mRecord.saveState();
	}

	private void restoreInstanceState() {
		mRecord.restoreState();
		performBSRestoreInstanceState(mRecord);
	}

	void performBSResume() {
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSResume.");
		}

		restoreInstanceState();
		onBSResume();
		// send all not treated messages
		sendMessagesQueue();
		// ready for onHandleIntent()

		canHandleIntent = true;
		isResumed = true;

		// send gestures mask if in onCreate() call
		if (mMask != -1) {
			enableGestures(mMask);
		}
		unlockWaitForActive();
	}

	private final void performBSDestroy() {
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSDestroy.");
		}
		onBSDestroy();
	}

	final void performBSTouchEvent(BSMotionEvent motionEvent) {
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSTouchEvent:" + motionEvent);
		}
		onBSTouchEvent(motionEvent);
	}

	final void performBSSaveInstanceState(BSRecord record) {
		onBSSaveInstanceState(record.getData());
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSSaveInstanceState " + this + " : " + record.getData());
		}
	}

	final void performBSRestoreInstanceState(BSRecord record) {
		onBSRestoreInstanceState(record.getData());
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSRestoreInstanceState " + this + " : " + record.getData());
		}
	}

	void performBSPause() {
		if (DEBUG_BS_LIFECIRCLE) {
			Log.v(TAG, "onBSPause.");
		}

		isResumed = false;
		canHandleIntent = false;

		mCalled = false;
		onBSPause();
		if (!mCalled) {
			throw new SuperNotCalledException("BSActivity " + getClass().getSimpleName() + " did not call through to super.onBSPause()");
		}
	}

	void performBSGestureDisable() {
		isBSEventEnable = false;
		onBSTouchDisable();
	}

	void performBSGestureEnable() {
		isBSEventEnable = true;
		onBSTouchEnadle();
	}

	final void performVolumeButtonsEvent(VolumeButtonsEvent event) {
		onVolumeButtonsEvent(event);
	}

	/**
	 * Called when BsDrawer is registered in the PM. In this state BsDrawer gains privileges to draw on BS.
	 */
	protected void onBSCreate() {

	}

	/**
	 * Called when BsDrawer is ready to draw on BS.
	 */
	protected void onBSResume() {

	}

	/**
	 * Called when BsDrawer loses privileges to draw on BS.
	 */
	protected void onBSStop() {
		stopSelf();
		mCalled = true;
	}

	protected void onBSPause() {
		mCalled = true;
	}

	@Override
	public final IBinder onBind(Intent intent) {
		return super.onBind(intent);
	}

	/**
	 * Called when BsDrawer is unregistered from PM.
	 */
	protected void onBSDestroy() {

	}

	/**
	 * Called when BS touch event occurs.
	 * 
	 * @param motionEvent
	 *            BS motion event.
	 */
	protected void onBSTouchEvent(BSMotionEvent motionEvent) {

	}

	protected void onBSTouchDisable() {

	}

	protected void onBSTouchEnadle() {

	}

	protected void onVolumeButtonsEvent(VolumeButtonsEvent event) {

	}

	public boolean isBSTouchEnable() {
		return isBSEventEnable;
	}

	/**
	 * Save state before the instance is killed
	 * 
	 * @param outState
	 *            instance state.
	 */
	protected void onBSSaveInstanceState(Bundle outState) {

	}

	/**
	 * Restores the state.
	 * 
	 * @param savedInstanceState
	 *            saved instance state.
	 */
	protected void onBSRestoreInstanceState(Bundle savedInstanceState) {

	}

	public Intent getIntent() {
		return mStartIntent;
	}

	public BSDrawer getBSDrawer() {
		return mDrawer;
	}

	/**
	 * Sending request to PM to be active on BS.
	 */
	void sendRequestToBeActive() {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.EXTRA_SERVICE_NAME, BSActivity.this.getClass().getName());
		bundle.putBoolean(Constants.EXTRA_FULL_SCREEN_MODE, BSActivity.this.getFullScreenMode());
		sendToPlatinum(Constants.MESSAGE_SET_ACTIVE, bundle);
	}

	void sendRequestToEnableGesture(int mask) {
		Bundle bundle = new Bundle();
		bundle.putInt(Constants.EXTRA_MASK_GESTURES, mask);
		sendToPlatinum(Constants.MESSAGE_GESTURE_BACK_ENABLE, bundle);
	}

	private void sendToPlatinum(int what, Bundle bundle) {
		try {
			Message msg = Message.obtain(null, what);

			msg.arg1 = android.os.Process.myPid();
			msg.arg2 = android.os.Process.myUid();

			msg.setData(bundle);
			msg.replyTo = mMessenger;
			mService.send(msg);
		} catch (Exception e) {
			Log.e(TAG, "Error while send msg", e);
			if (what != Constants.MESSAGE_SET_ACTIVE) {
				performBSPause();
			}
			performBSStop();
		}
	}

	void sendRequestToDrawBitmap(int left, int top, Bitmap bitmap, int waveform, int ditheringAlgorithm) {
		try {
			mDrawerService.drawBitmap(left, top, waveform, bitmap, ditheringAlgorithm);
		} catch (Exception e) {
			Log.e(TAG, "Error while send msg", e);
			performBSPause();
			performBSStop();
		}
	}

	/**
	 * Application can request to work in Full Screen mode. In this mode, no bar/counter notifications appear on the
	 * screen.
	 * 
	 * @param isFullScreen
	 *            is full screen mode enabled.
	 */
	protected void setFullScreenMode(boolean isFullScreen) {
		mFullScreenMode = isFullScreen;
	}

	/**
	 * @return is full screen mode enabled.
	 */
	protected boolean getFullScreenMode() {
		return mFullScreenMode;
	}

	/**
	 * see constants in EinkUtils ( GESTURE_BACK_SWIPE_RIGHT,GESTURE_BACK_SWIPE_LEFT,GESTURE_BACK_SINGLE_TAP,GESTURE_BACK_RLR,GESTURE_BACK_LRL)
	 */
	protected void enableGestures(int mask) {
		if (isResumed) {
			sendRequestToEnableGesture(mask);
		} else {
			mMask = mask;
		}
	}
}
