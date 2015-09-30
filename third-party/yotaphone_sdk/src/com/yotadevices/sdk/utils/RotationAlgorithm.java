package com.yotadevices.sdk.utils;

import com.yotadevices.platinum.R;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

public class RotationAlgorithm implements SensorEventListener {
	private static final String TAG = "RotationAlgorithm";
	public interface OnPhoneRotatedListener {
		public void onPhoneRotatedToFS();
		public void onPhoneRotatedToBS();
	}

	public static final int OPTION_START_WITH_BS = 2;
	public static final int OPTION_POWER_ON = 4;
	public static final int OPTION_NO_UNLOCK = 8;

	private Context mContext;
	private static RotationAlgorithm mInstance;
	private OnPhoneRotatedListener mListener=null;

	public static RotationAlgorithm getInstance(Context context) {
		if (mInstance==null) {
			mInstance = new RotationAlgorithm(context);
			return mInstance;
		} else {
			mInstance.setContext(context);
			return mInstance;
		}
	}

	private class SensorAttributes {
		public float x;
		public float y;
		public float z;
	}

	private final static class MyPowerUtils implements IPowerCallback {

		private static MyPowerUtils sInstance;
		private Context ctx;

		public static MyPowerUtils getInstance(Context ctx) {
			if (sInstance == null) {
				sInstance = new MyPowerUtils(ctx);
			}
			return sInstance;
		}

		public void setContext(Context ctx) {
			this.ctx = ctx;
		}

		private MyPowerUtils(Context ctx) {
			this.ctx = ctx;
		}

		@Override
		public void goToSleep() {
			PowerUtils.goToSleep(ctx);
		}

		@Override
		public void wakeUp() {
			PowerUtils.wakeUp(ctx);
		}

		@Override
		public void lockOn() {
			PowerUtils.lockOn(ctx);
		}

		@Override
		public void lockOff() {
			PowerUtils.lockOff(ctx);
		}

		@Override
		public void lockBackScreen() {
			PowerUtils.lockBackScreen(ctx);
		}

		@Override
		public void unlockBackScreen() {
			PowerUtils.unlockBackScreen(ctx);
		}
	}

	private boolean firstStep=false;
	private boolean rotationPassedShortSide=false;
	private boolean rotationPassedLongSide=false;
	private long p2bClickedTime;
	private long rotationTime;

	private SensorAttributes accelerometer = new SensorAttributes();
	private SensorAttributes gyroscope = new SensorAttributes();

	private LinkedList<SensorAttributes> accelerometerArray = new LinkedList<SensorAttributes>();
	private LinkedList<SensorAttributes> gyroscopeArray = new LinkedList<SensorAttributes>();
	private final int mTimeDelay = 50; //time is in milliseconds

	private final int mArraySize = 1000/mTimeDelay;
	private boolean mUserIsLookingAtFS=true;
	private boolean mUserIsLookingAtFSPrevious=true;

	private boolean mFSIsUp=true;
	private boolean mUserIsLying=false;
	private boolean mStartWithFS=true;
	private boolean mPowerOn=false;
	private boolean mDeviceLockSettingIsNone=false;
	private boolean mNoUnlock=false;
	private boolean mScreenJustLocked=false;

	private SensorManager mSensorManager;
	private KeyguardManager mKeyguardManager;

	private IPowerCallback mUtils;

	private RotationAlgorithm(Context context) {
		mContext = context;
		mUtils = MyPowerUtils.getInstance(context);
	}

	public void setPowerCallback(IPowerCallback utils) {
		mUtils = utils;
	}

	public void setContext(Context context) {
		if (mContext!=context) {
			mContext = context;
			((MyPowerUtils)mUtils).setContext(context);
		}
	}

	public void turnScreenOffIfRotated(int options, OnPhoneRotatedListener listener) {
		mListener=listener;
		turnScreenOffIfRotated(options);
	}

	public void turnScreenOffIfRotated(int options) {
		if ((options & OPTION_START_WITH_BS) == OPTION_START_WITH_BS) {
			mStartWithFS=false;
		} else {
			mStartWithFS=true;
		}

		if ((options & OPTION_POWER_ON) == OPTION_POWER_ON) {
			mPowerOn=true;
		} else {
			mPowerOn=false;
		}

		if ((options & OPTION_NO_UNLOCK) == OPTION_NO_UNLOCK) {
			mNoUnlock=true;
		} else {
			mNoUnlock=false;
		}

		turnScreenOffIfRotated();
	}

	public void turnScreenOffIfRotated() {
		rotationTime=0;
		firstStep=true;
		mUserIsLookingAtFS=true;
		mUserIsLookingAtFSPrevious=true;
		rotationPassedShortSide=false;
		rotationPassedLongSide=false;
		mFSIsUp=true;
		mUserIsLying=false;
		accelerometerArray = new LinkedList<SensorAttributes>();
		gyroscopeArray = new LinkedList<SensorAttributes>();
		p2bClickedTime=System.currentTimeMillis();
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), mTimeDelay*1000);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), mTimeDelay*1000);
		mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
	}

	public void issueStandardToastAndVibration() {
		Toast toast = Toast.makeText(mContext, mContext.getResources().getString(R.string.application_is_updated_on_bs), Toast.LENGTH_SHORT);
		((TextView) toast.getView().findViewById(android.R.id.message)).setGravity(Gravity.CENTER);
		toast.show();
		((Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(mContext.getResources().getInteger(R.integer.vibration_time));
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accelerometer.x=event.values[0];
			accelerometer.y=event.values[1];
			accelerometer.z=event.values[2];
		}
		else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			gyroscope.x=event.values[0];
			gyroscope.y=event.values[1];
			gyroscope.z=event.values[2];
		}

		int display_mode = mContext.getResources().getConfiguration().orientation;

		if (display_mode == Configuration.ORIENTATION_LANDSCAPE) {
			float buf;
			buf=accelerometer.x;
			accelerometer.x=accelerometer.y;
			accelerometer.y=buf;
			buf=gyroscope.x;
			gyroscope.x=gyroscope.y;
			gyroscope.y=buf;
		}

		accelerometerArray.add(accelerometer);
		if (accelerometerArray.size()>mArraySize) {
			accelerometerArray.poll();
		}
		gyroscopeArray.add(gyroscope);
		if (gyroscopeArray.size()>mArraySize) {
			gyroscopeArray.poll();
		}


		float acceleromterYAvg=0;
		for (SensorAttributes sa:accelerometerArray) {
			acceleromterYAvg+=sa.y;
		}
		acceleromterYAvg=acceleromterYAvg/accelerometerArray.size();
		float acceleromterXAvg=0;
		for (SensorAttributes sa:accelerometerArray) {
			acceleromterXAvg+=sa.x;
		}
		acceleromterXAvg=acceleromterXAvg/accelerometerArray.size();

		float gyroscopeXAvg=0;
		for (SensorAttributes sa:gyroscopeArray) {
			gyroscopeXAvg+=sa.x;
		}
		gyroscopeXAvg=gyroscopeXAvg/gyroscopeArray.size();

		float gyroscopeYAvg=0;
		for (SensorAttributes sa:gyroscopeArray) {
			gyroscopeYAvg+=sa.y;
		}
		gyroscopeYAvg=gyroscopeYAvg/gyroscopeArray.size();

		float gyroscopeZAvg=0;
		for (SensorAttributes sa:gyroscopeArray) {
			gyroscopeZAvg+=sa.z;
		}
		gyroscopeZAvg=gyroscopeZAvg/gyroscopeArray.size();

		if (firstStep) { //first step is needed to determine if user is already holding he device upside-down
			if (accelerometer.z<-3 && mStartWithFS) {
				mUserIsLying=true;
				mFSIsUp=true;
				mUserIsLookingAtFS=true;
				rotationPassedShortSide=true;
				gyroscopeXAvg=2;
				gyroscopeYAvg=1;
			}
			if (accelerometer.z>3 && !mStartWithFS) {
				mUserIsLying=true;
				mFSIsUp=true;
				mUserIsLookingAtFS=false;
				rotationPassedShortSide=true;
				gyroscopeXAvg=2;
				gyroscopeYAvg=1;
			}
			firstStep=false;
		}

		if ((accelerometer.z<-3 && mUserIsLookingAtFS || accelerometer.z>3 && !mUserIsLookingAtFS) && Math.abs(gyroscopeYAvg)>3) { //if rotation happened and it happened via certain side
			rotationPassedLongSide=true;
			rotationPassedShortSide=false;
		} else if ((accelerometer.z<-3 && mUserIsLookingAtFS || accelerometer.z>3 && !mUserIsLookingAtFS) && Math.abs(gyroscopeXAvg)>1) {
			rotationPassedLongSide=false;
			rotationPassedShortSide=true;
		}

		if (accelerometer.z>3 && rotationPassedShortSide) {
			mFSIsUp=true;
			if (mFSIsUp!=mUserIsLookingAtFS){
				mUserIsLying=true;
			} else {
				mUserIsLying=false;
			}
		}
		if (accelerometer.z<-3 && rotationPassedShortSide) {
			mFSIsUp=false;
			if (mFSIsUp!=mUserIsLookingAtFS){
				mUserIsLying=true;
			} else {
				mUserIsLying=false;
			}
		}

		if (accelerometer.z>3 && (rotationPassedLongSide || (!rotationPassedShortSide && !rotationPassedLongSide))) {
			if (mUserIsLying) {
				mUserIsLookingAtFS=false;
				mFSIsUp=false;
			} else {
				mUserIsLookingAtFS=true;
				mFSIsUp=true;
			}
			rotationPassedShortSide=false;
			rotationPassedLongSide=false;
		}
		if (accelerometer.z<-3 && (rotationPassedLongSide || (!rotationPassedShortSide && !rotationPassedLongSide))) {
			if (mUserIsLying) {
				mUserIsLookingAtFS=true;
				mFSIsUp=true;
			} else {
				mUserIsLookingAtFS=false;
				mFSIsUp=false;
			}
			rotationPassedShortSide=false;
			rotationPassedLongSide=false;
		}

		if (Math.abs(gyroscopeZAvg)>3 && mUserIsLying) {
			mUserIsLookingAtFS=mFSIsUp;
			rotationPassedShortSide=false;
			rotationPassedLongSide=false;
		}

		if (mUserIsLookingAtFS) {
			if (System.currentTimeMillis()>p2bClickedTime+4*1000) {
				mSensorManager.unregisterListener(this);
			}
			if (mUserIsLookingAtFSPrevious!=mUserIsLookingAtFS) {
				mSensorManager.unregisterListener(this);
				if (mPowerOn || mDeviceLockSettingIsNone) mUtils.wakeUp();
				mUtils.lockBackScreen();

				new UnlockScreen().execute(1);

				if (mListener!=null) mListener.onPhoneRotatedToFS();
			}
		} else {
			if (mUserIsLookingAtFSPrevious!=mUserIsLookingAtFS) {
				rotationTime=System.currentTimeMillis();
				if (mStartWithFS) {
					mUtils.lockOn();
					new Handler().postDelayed(new Runnable() {

						@Override
						public void run() {
							if (!mKeyguardManager.inKeyguardRestrictedInputMode()) { //check for the case when user unlocked the screen while we waited
								mUtils.goToSleep();
								mScreenJustLocked=true;
								mDeviceLockSettingIsNone=true;
							}
						}
					}, 500);
				}
				mScreenJustLocked=true;
				if (!mNoUnlock) mUtils.unlockBackScreen();
			}

			if (System.currentTimeMillis()>rotationTime+4*1000) {
				mSensorManager.unregisterListener(this);
				if (!mDeviceLockSettingIsNone) {
					if (mKeyguardManager.inKeyguardRestrictedInputMode()) { //check for the case when user unlocked the screen while we waited
						mUtils.goToSleep();
						mScreenJustLocked=false;
					} else {
						mUtils.lockBackScreen();
					}
				}
			}
			if (mListener!=null) mListener.onPhoneRotatedToBS();
		}
		mUserIsLookingAtFSPrevious=mUserIsLookingAtFS;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private class UnlockScreen extends AsyncTask<Integer, Integer, Integer> {
		@Override
		protected Integer doInBackground(Integer... ints) {
			int limit = 2000;
			while (!mKeyguardManager.inKeyguardRestrictedInputMode() && (mScreenJustLocked || mPowerOn)) {//If rotation happens fast then unlock can happen faster than lock is completed. This is why we need to perform another unlock operation after some time.
				try {
					mUtils.lockOff();
					Thread.sleep(50);
					limit-=50;
					if (limit<0) break;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mUtils.lockOff();
			mScreenJustLocked=false;
			return 1;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
		}

		@Override
		protected void onPostExecute(Integer result) {
		}
	}

}
