package com.yotadevices.sdk;

import com.yotadevices.sdk.Constants.Gestures;
import com.yotadevices.sdk.Constants.VolumeButtonsEvent;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Handler of incoming messages from service.
 */
class BSAcivityIncomingMessagesHandler extends Handler {

	private BSActivity mBSActivity;

	public BSAcivityIncomingMessagesHandler(BSActivity bsActivity) {
		mBSActivity = bsActivity;
	}

	@Override
	public void handleMessage(Message msg) {
		Log.d(BSActivity.TAG, "Received from service: " + msg.what);
		switch (msg.what) {
		case Constants.MESSAGE_ACTIVATED:
			mBSActivity.performBSResume();
			break;
		case Constants.MESSAGE_DISACTIVATED:
			mBSActivity.performBSPause(); // TODO: lifecircle
			mBSActivity.performBSStop();
			break;
		case Constants.MESSAGE_MOTION_EVENT:
			BSMotionEvent event = new BSMotionEvent();
			event.setBSAction(Gestures.valueOf(msg.arg1));
			mBSActivity.performBSTouchEvent(event);
			break;
		case Constants.MESSAGE_BS_PAUSE:
			mBSActivity.performBSPause();
			break;
		case Constants.MESSAGE_BS_GESTURE_DISABLE:
			mBSActivity.performBSGestureDisable();
			break;
		case Constants.MESSAGE_BS_GESTURE_ENABLE:
			mBSActivity.performBSGestureEnable();
			break;
		case Constants.MESSAGE_VOLUME_BUTTONS_EVENT:
			mBSActivity.performVolumeButtonsEvent(VolumeButtonsEvent.valueOf(msg.arg1));
		default:
			super.handleMessage(msg);
		}
	}
}
