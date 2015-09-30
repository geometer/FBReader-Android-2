
package com.yotadevices.sdk.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/*
 * Hide Helper class
 */
public class ServiceBSHelper {

    public interface OnSuccesBinding {
        void onBind(IFrameworkService service);

        void onError();
    }

    private final static String TAG = "BSLockManager";

    private Context mContext;

    FrameforkServiceConnection mConnection;
    IFrameworkService mFrameforkService;

    private final class FrameforkServiceConnection implements ServiceConnection {

        OnSuccesBinding listener;

        void setListener(OnSuccesBinding l) {
            listener = l;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mFrameforkService = IFrameworkService.Stub.asInterface(service);
            if (listener != null) {
                listener.onBind(mFrameforkService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mFrameforkService = null;
        }

    }

    public ServiceBSHelper(Context ctx) {
        mContext = ctx;
        mConnection = new FrameforkServiceConnection();
    }

    public void getAsyncService(OnSuccesBinding listener) {
        if (mFrameforkService != null) {
            listener.onBind(mFrameforkService);
        } else {
            bind(listener);
        }
    }

    public void unbind() {
        try {
            mContext.unbindService(mConnection);
        } catch (Exception unused) {
        }
    }

    private void bind(OnSuccesBinding listener) {
        Intent platinumIntent = new Intent();
        platinumIntent.setAction(HelperConstant.FRAMEWORK_SERVICE_AIDL_SIGNATURE);
        platinumIntent.setClassName(HelperConstant.PLATINUM_FRAMEWORK_PACKAGE, HelperConstant.PLATINUM_FRAMEWORK_SERVICE_CLASS);

        mConnection.setListener(listener);
        boolean b = mContext.bindService(platinumIntent, mConnection, Context.BIND_AUTO_CREATE);
        if (!b) {
            Log.d(TAG, "can't to bind service " + HelperConstant.PLATINUM_FRAMEWORK_PACKAGE);
            if (listener != null) {
                listener.onError();
            }
        }
    }

}
