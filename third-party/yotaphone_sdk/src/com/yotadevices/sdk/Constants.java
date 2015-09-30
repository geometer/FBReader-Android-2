package com.yotadevices.sdk;

public final class Constants {

	public final static int MESSAGE_GESTURES = 0;
	public final static int MESSAGE_CAMERA = 1;
	public final static int MESSAGE_BUTTONS = 2;
	public final static int MESSAGE_STATUS_BAR = 3;

	/**
	 * app request to being active.
	 */
	public static final int MESSAGE_SET_ACTIVE = 4;

	/**
	 * PM answer that app is active.
	 */
	public static final int MESSAGE_ACTIVATED = 5;

	/**
	 * PM answer that app is disactive.
	 */
	public static final int MESSAGE_DISACTIVATED = 6;

	/**
	 * motion event to the app.
	 */
	public static final int MESSAGE_MOTION_EVENT = 7;

	/**
	 * if BS busy
	 */
	public static final int MESSAGE_BS_PAUSE = 8;

	public static final int MESSAGE_BS_GESTURE_DISABLE = 9;

	public static final int MESSAGE_BS_GESTURE_ENABLE = 10;

	public static final int MESSAGE_VOLUME_BUTTONS_EVENT = 11;

	public static final int MESSAGE_GESTURE_BACK_ENABLE = 12;

	/**
	 * extra service name.
	 */
	public static final String EXTRA_SERVICE_NAME = "service_class_name";

	/**
	 * extra is full screen mode enabled.
	 */
	public static final String EXTRA_FULL_SCREEN_MODE = "full_screen_mode";

	/**
	 * See PlatinumManagerService. To send message(event), if service not active.
	 */
	public static final String EXTRA_MESSAGE = "service_message";

	/**
	 * extra is gestures back enabled.
	 */
	public static final String EXTRA_MASK_GESTURES = "mask_gestures";

	public static final String META_DATA_BS_ICON = "com.yotadevices.BS_ICON";
	public static final String META_DATA_BS_TITLE = "com.yotadevices.BS_TITLE";

	public enum CameraEvent {
		CAMERA_PREVIEW_START, CAMERA_PREVIEW_STOP, CAMERA_PHOTOSHUTTER, CAMERA_VIDEORECORDING_START, CAMERA_VIDEORECORDING_STOP, CAMERA_ERROR, CAMERA_CLOSED, UNKNOW;

		public static CameraEvent valueOf(int event) {
			switch (event) {
			case 0:
				return CAMERA_PREVIEW_START;
			case 1:
				return CAMERA_PREVIEW_STOP;
			case 2:
				return CAMERA_PHOTOSHUTTER;
			case 3:
				return CAMERA_VIDEORECORDING_START;
			case 4:
				return CAMERA_VIDEORECORDING_STOP;
			case 5:
				return CAMERA_ERROR;
			case 6:
				return CAMERA_CLOSED;
			default:
				return UNKNOW;
			}
		}
	}

	public enum VolumeButtonsEvent {
		VOLUME_PLUS_DOWN, VOLUME_MINUS_DOWN, VOLUME_PLUS_UP, VOLUME_MINUS_UP, VOLUME_PLUS_LONG_PRESS, VOLUME_MINUS_LONG_PRESS, UNKNOW;

		public static VolumeButtonsEvent valueOf(int event) {
			switch (event) {
			case 0:
				return VOLUME_PLUS_DOWN;
			case 1:
				return VOLUME_MINUS_DOWN;
			case 2:
				return VOLUME_PLUS_UP;
			case 3:
				return VOLUME_MINUS_UP;
			case 4:
				return VOLUME_PLUS_LONG_PRESS;
			case 5:
				return VOLUME_MINUS_LONG_PRESS;
			default:
				return UNKNOW;
			}
		}
	}

	public enum Gestures {
		GESTURES_P2B, GESTURES_TOP_LONG_PRESS, GESTURES_TOP_UP, GESTURES_BS_LR, GESTURES_BS_RL, GESTURES_BS_LRL, GESTURES_BS_RLR, GESTURES_BS_SINGLE_TAP, GESTURES_BS_LONG_PRESS, GESTURES_BS_LONG_PRESS_UP, GESTURES_BS_DOUBLE_TAP, GESTURES_BS_SCROLL_LEFT, GESTURES_BS_SCROLL_RIGHT, GESTURES_UNKNOW;

		// not use ordinal();
		public static Gestures valueOf(int gesture) {
			switch (gesture) {
			case 0:
				return GESTURES_P2B;
			case 1:
				return GESTURES_TOP_LONG_PRESS;
			case 2:
				return GESTURES_TOP_UP;
			case 3:
				return GESTURES_BS_LR;
			case 4:
				return GESTURES_BS_RL;
			case 5:
				return GESTURES_BS_LRL;
			case 6:
				return GESTURES_BS_RLR;
			case 7:
				return GESTURES_BS_SINGLE_TAP;
			case 8:
				return GESTURES_BS_LONG_PRESS;
			case 9:
				return GESTURES_BS_LONG_PRESS_UP;
			case 10:
				return GESTURES_BS_DOUBLE_TAP;
			case 11:
				return GESTURES_BS_SCROLL_LEFT;
			case 12:
				return GESTURES_BS_SCROLL_RIGHT;
			default:
				return GESTURES_UNKNOW;
			}
		}

	}

	public static final class Notifications {
		private static final int FULL_SCREEN_NOTIFICATION = 65536;
		// Half screen notification
		private static final int HALF_SCREEN_NOTIFICATION = 131072;
		// Bar notification
		private static final int BAR_NOTIFICATION = 262144;
		// Counter notification
		private static final int COUNTER_NOTIFICATION = 524288;

		private static final int UNKNOW = -1;

		public static int valueOf(int event) {
			switch (event) {
			case 65536:
				return FULL_SCREEN_NOTIFICATION;
			case 131072:
				return HALF_SCREEN_NOTIFICATION;
			case 262144:
				return BAR_NOTIFICATION;
			case 524288:
				return COUNTER_NOTIFICATION;
			default:
				return UNKNOW;
			}
		}
	}

	public static final class Settings {

		/**
		 * Whether Privacy Mode is on.
		 */
		public static final String PRIVACY_MODE = "yotadevices_privacy_mode";

		/**
		 * Whether Favorites Mode is on.
		 */
		public static final String FAVORITES_MODE = "yotadevices_favorites_mode";

		/**
		 * Whether SMSFUN Mode is on.
		 */
		public static final String SMS_EMOTIONAL_MODE = "yotadevices_sms_emotional_mode";

		/**
		 * Whether notification can show on BS.
		 */
		public static final String BS_NOTIFICATION_ON = "yotadevices_bs_notification_on";

		/**
		 * Whether Smile for the Camera/Video is on.
		 */
		public static final String SMILE_FOR_CAMERA = "yotadevices_smile_camera_on";

		/**
		 * Whether preview photo after shutting is on.
		 */
		public static final String PHOTO_PREVIEW = "yotadevices_photo_preview_on";

		/**
		 * Whether discharged state on BS is on.
		 */
		public static final String DISCHARGED_STATE = "yotadevices_discharged_state_on";

		/**
		 * Whether discharged state on BS is on.
		 */
		public static final String DISMISS_FS_NOTIFICATION = "yotadevices_dismiss_fs_notification";

		/**
		 * Whether Task Manager on BS is on.
		 */
		public static final String TASK_MANAGER_MODE = "yotadevices_task_manager_mode";

	}

	private Constants() {

	}
}
