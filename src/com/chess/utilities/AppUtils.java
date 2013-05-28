package com.chess.utilities;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import com.chess.R;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.StaticData;
import com.chess.model.GameListCurrentItem;
import com.chess.ui.views.drawables.BackgroundChessDrawable;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AppUtils class
 *
 * @author alien_roger
 * @created at: 01.02.12 7:50
 */
public class AppUtils {

	public static final float MDPI = 1.0f;
	public static final float HDPI = 1.5f;
	public static final float XHDPI = 2.0f;
	private static boolean ENABLE_LOG = true;
	private static final String DAYS = "d";
	private static final String H = "h";
	private static final String M = "m";

	public static final boolean HONEYCOMB_PLUS_API = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	public static final boolean JELLYBEAN_PLUS_API = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public static int sizeOfBitmap(Bitmap data) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
			return data.getRowBytes() * data.getHeight();
		} else {
			return data.getByteCount();
		}
	}

	public static String getApplicationCacheDir(String packageName) {
		// path should match the specified string
		// /Android/data/<package_name>/files/
		return "Android/data/" + packageName + "/cache/";
	}

	public static void copyStream(InputStream is, OutputStream os) {
		final int buffer_size = 1024;
		try {
			byte[] bytes = new byte[buffer_size];
			for (;;) {
				int count = is.read(bytes, 0, buffer_size);
				if (count == -1)
					break;
				os.write(bytes, 0, count);
			}
		} catch (Exception ignored) {
		}
	}

	public static class ListSelector implements Runnable{
		private int pos;
		private ListView listView;

		public ListSelector(int pos, ListView listView){
			this.pos = pos;
			this.listView = listView;
		}
		@Override
		public void run() {
			listView.setSelection(pos);
		}
	}

	public static void setBackground(View mainView, Context context) {
		if (JELLYBEAN_PLUS_API){
			mainView.setBackground(new BackgroundChessDrawable(context));
		} else {
			mainView.setBackgroundDrawable(new BackgroundChessDrawable(context));
		}

		int paddingTop = (int) context.getResources().getDimension(R.dimen.dashboard_padding_top);
		int paddingLeft = (int) context.getResources().getDimension(R.dimen.dashboard_padding_side);
		int paddingRight = (int) context.getResources().getDimension(R.dimen.dashboard_padding_side);
		int paddingBot = (int) context.getResources().getDimension(R.dimen.dashboard_padding_bot);
		mainView.setPadding(paddingLeft, paddingTop, paddingRight, paddingBot);
	}

	/**
	 * For QVGA screens we don't need a title bar and Action bar
	 * @param context
	 * @return
	 */
	public static boolean needFullScreen(Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		Configuration config = context.getResources().getConfiguration();
		return (displayMetrics.density < MDPI || displayMetrics.densityDpi == DisplayMetrics.DENSITY_LOW)
				&& config.orientation == Configuration.ORIENTATION_PORTRAIT;
	}

	/**
	 * For mdpi normal screens we don't need a action bar only
	 * @param context
	 * @return
	 */
	public static boolean noNeedTitleBar(Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		Configuration config = context.getResources().getConfiguration();
		return (displayMetrics.density == MDPI || displayMetrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM)
				&& (displayMetrics.heightPixels <= 480 && config.orientation == Configuration.ORIENTATION_PORTRAIT
				|| displayMetrics.heightPixels <= 300 && config.orientation == Configuration.ORIENTATION_LANDSCAPE);
	}

	/**
	 * For mdpi normal screens we don't need a action bar only
	 * @param context
	 * @return
	 */
	public static boolean isHdpi800(Context context) {
		DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		Configuration config = context.getResources().getConfiguration();
		return (displayMetrics.density == HDPI || displayMetrics.densityDpi == DisplayMetrics.DENSITY_HIGH)
				&& (displayMetrics.heightPixels <= 800 && config.orientation == Configuration.ORIENTATION_PORTRAIT);
	}

	/**
	 * Fire simplified notification with defined arguments
	 *
	 * @param context - Application Context for resources
	 * @param title - title that will be visible at status bar
	 * @param id - request code id
	 * @param body - short description for notification message content
	 * @param clazz - which class to open when User press notification
	 */
	public static void showMoveStatusNotification(Context context, String title,  String body, int id, Class<?> clazz) {
		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.ic_stat_chess, title, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		Intent intent = new Intent(context, clazz);
//		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent contentIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_ONE_SHOT);

		notification.setLatestEventInfo(context, title, body, contentIntent);
		notifyManager.cancelAll();
		notifyManager.notify(R.id.notification_message, notification);

		SharedPreferences preferences = AppData.getPreferences(context);
		boolean playSounds = preferences.getBoolean(AppData.getUserName(context) + AppConstants.PREF_SOUNDS, false);
		if(playSounds){
			final MediaPlayer player = MediaPlayer.create(context, R.raw.move_opponent);

			if(player == null) // someone hasn't player?
				return;

			player.setOnCompletionListener(completionListener);
			player.start();
		}
	}

	private static MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.stop();
			mediaPlayer.release();
		}
	};

	public static void showNewMoveStatusNotification(Context context, String title,  String body, int id,
													 GameListCurrentItem currentGameItem) {
//		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//		notifyManager.cancelAll(); // clear all previous notifications
//
//		Notification notification = new Notification(R.drawable.ic_stat_chess, title, System.currentTimeMillis());
//		notification.flags |= Notification.FLAG_AUTO_CANCEL;
//
//		Intent intent = new Intent(context, GameOnlineScreenActivity.class);
//		intent.putExtra(BaseGameItem.GAME_ID, currentGameItem.getGameId());
//		intent.putExtra(AppConstants.NOTIFICATION, true);
////		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);  // as we are using singleTask mode for GameOnlineActivity we call enter there via onNewIntent callback
//
//		PendingIntent contentIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_ONE_SHOT);
//
//		notification.setLatestEventInfo(context, title, body, contentIntent);
//
//		notifyManager.notify((int) currentGameItem.getGameId(), notification);
	}

	/**
	 * Use default android.util.Log with Flag trigger
	 * Use this method to track changes, but avoid to use in uncertain cases,
	 * where release version can tell where some bugs were born
	 * @param tag
	 * @param message
	 */
	public static void logD(String tag, String message){
		if(ENABLE_LOG) // can be set false for release version.
			Log.d(tag, message);
	}

	public static void startNotificationsUpdate(Context context) {
//		Intent statusUpdate = new Intent(context, AlarmReceiver.class);
//
//		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, StaticData.YOUR_MOVE_UPDATE_ID,
//				statusUpdate, PendingIntent.FLAG_UPDATE_CURRENT);
//
//		// schedule the service for updating
//		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//		alarms.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), StaticData.REMIND_ALARM_INTERVAL, pendingIntent);
	}

	public static void stopNotificationsUpdate(Context context) {
//		Intent statusUpdate = new Intent(context, AlarmReceiver.class);
//
//		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, StaticData.YOUR_MOVE_UPDATE_ID, statusUpdate,
//				PendingIntent.FLAG_UPDATE_CURRENT);
//		AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//		alarms.cancel(pendingIntent);
//
//		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//
//		notifyManager.cancel(R.id.notification_message);
	}

	public static void cancelNotifications(Context context) {
		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notifyManager.cancelAll();
	}

	public static void cancelNotification(Context context, int id){
		NotificationManager notifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notifyManager.cancel(id);
	}

	public static boolean isNeedToUpgrade(Context context){
		return AppData.getUserPremiumStatus(context) < StaticData.GOLD_USER;
	}

	public static boolean isNeedToUpgradePremium(Context context){
		return AppData.getUserPremiumStatus(context) < StaticData.DIAMOND_USER;
	}

	public static String getTimeLeftFromSeconds(long duration, Context context) {
		long minutes = duration /60%60;
		long hours = duration /3600%24;
		long days = duration /86400;
		StringBuilder sb = new StringBuilder();

		if (days > 0) {
			sb.append(days).append(StaticData.SYMBOL_SPACE).append(context.getString(R.string.days)).append(StaticData.SYMBOL_SPACE);
		} else if (hours > 0) {
			if (!sb.toString().trim().equals(StaticData.SYMBOL_EMPTY))
				sb.append(StaticData.SYMBOL_SPACE);
			sb.append(hours).append(StaticData.SYMBOL_SPACE).append(context.getString(R.string.hours)).append(StaticData.SYMBOL_SPACE);
		} else if (minutes > 0) {
			if (!sb.toString().trim().equals(StaticData.SYMBOL_EMPTY))
				sb.append(StaticData.SYMBOL_SPACE);
			sb.append(context.getString(R.string.min_arg, minutes));
		}

		return sb.toString();
	}

	public static String getSecondsTimeFromSecondsStr(long duration) {
		long seconds = duration %60;
		long minutes = duration /60%60;
		long hours = duration /3600%24;
		long days = duration /86400;
		StringBuilder sb = new StringBuilder();

		if (days > 0) {
            sb.append(days).append(StaticData.SYMBOL_COLON);
        }

		if (hours > 0) {
			sb.append(hours).append(StaticData.SYMBOL_COLON);
		}

        if (minutes < 10) {
            sb.append(0);
        }
        sb.append(minutes).append(StaticData.SYMBOL_COLON);

        if (seconds < 10) {
            sb.append(0);
        }
        sb.append(seconds);

		return sb.toString();
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager cm
				= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnected();
	}

	public static String getI18nString(Context context, String codeMessage, String... parameters) {
		int stringIdentifier = context.getResources().getIdentifier(codeMessage, "string", context.getPackageName());
		return stringIdentifier != 0 ? context.getString(stringIdentifier, parameters) : null;
	}

	public static String getI18nStringForAPIError(Context context, String message) {
		final Resources resources = context.getResources();

		if (isLocaleEn(resources)) {
			return message;
		}

		final int positionOfMessage = Arrays.asList(resources.getStringArray(R.array.site_api_error_messages)).indexOf(message);
		if (positionOfMessage != -1) {
			final String messageKey = resources.getStringArray(R.array.site_api_error_keys)[positionOfMessage];
			return context.getString(resources.getIdentifier(messageKey, "string", context.getPackageName()));
		}

		return message;
	}

	private static boolean isLocaleEn(Resources resources) {
		return resources.getConfiguration().locale.getLanguage().equals(StaticData.LOCALE_EN);
	}

	public static class DeviceInfo {
		public String MODEL;
		public int SDK_API;
		public String APP_VERSION_NAME = StaticData.SYMBOL_EMPTY;
		public int APP_VERSION_CODE = 0;
		public String android_id;
		/*
		 * Get information about device model, App version and API version
		 */
		public DeviceInfo getDeviceInfo(Context context) {
			DeviceInfo deviceInfo = new DeviceInfo();

			deviceInfo.android_id =  Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);


			deviceInfo.MODEL = Build.MODEL;
			Log.i("requested MODEL = ", deviceInfo.MODEL);

			deviceInfo.SDK_API = Build.VERSION.SDK_INT;
			Log.i("requested SDK_INT = ", deviceInfo.SDK_API + StaticData.SYMBOL_EMPTY);
			// get version number and name
			try {
				PackageManager manager = context.getPackageManager();
				PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
				deviceInfo.APP_VERSION_CODE = info.versionCode;
				Log.i("requested versionCode = ", deviceInfo.APP_VERSION_CODE + StaticData.SYMBOL_EMPTY);

				deviceInfo.APP_VERSION_NAME = info.versionName;
				Log.i("requested versionName = ", deviceInfo.APP_VERSION_NAME);

			} catch (PackageManager.NameNotFoundException nnf) {
				nnf.printStackTrace();
			}
			return deviceInfo;
		}
	}

	public static String httpEntityToString(HttpEntity entity) throws IOException {

		InputStream inputStream = entity.getContent();
		int numberBytesRead = 0;
		StringBuilder out = new StringBuilder();
		byte[] bytes = new byte[4096];

		while (numberBytesRead != -1) {
			out.append(new String(bytes, 0, numberBytesRead));
			numberBytesRead = inputStream.read(bytes);
		}

		inputStream.close();

		return out.toString();
	}

	public static void showKeyBoard(Context context, EditText view){
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, 0 );
	}

//	public static void showKeyBoard(Context context, EditText editText){
//		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
//	}
//
	public static void hideKeyBoard(Context context, View editText){
		InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}

	public static List<String> convertArrayToList(String[] array){
		List<String> items = new ArrayList<String>();
		items.addAll(Arrays.asList(array));
		return items;
	}

	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens, and also
	 * remove tokens.
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##",
	 * new ForegroundColorSpan(0xFFFF0000));} will return a CharSequence
	 * {@code "Hello world!"} with {@code world} in red.
	 *
	 * @param text The text, with the tokens, to adjust.
	 * @param token The token string; there should be at least two instances
	 *             of token in text.
	 * @param cs The style to apply to the CharSequence. WARNING: You cannot
	 *            send the same two instances of this parameter, otherwise
	 *            the second call will remove the original span.
	 * @return A Spannable CharSequence with the new style applied.
	 *
	 * @see {@link <a>http://developer.android.com/reference/android/text/style/CharacterStyle.html</a> }
	 */
	public static CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs) {
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1)
		{
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs)
				ssb.setSpan(c, start, end, 0);

			// Delete the tokens before and after the span
			ssb.delete(end, end + tokenLen);
			ssb.delete(start - tokenLen, start);

			text = ssb;
		}

		return text;
	}

	public static Drawable getCountryFlag(Context context, String userCountry) {
		if (userCountry == null) {
			try {
				Log.e("getCountryFlag", " userCountry == NULL");
				return Drawable.createFromStream(context.getResources().getAssets().open("flags/" + "United States" + ".png"), null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			return Drawable.createFromStream(context.getResources().getAssets().open("flags/" + userCountry + ".png"), null);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("No such Country flag: " + userCountry);
		}
	}

	public static Drawable getCountryFlagScaled(Context context, String userCountry) {
		int flagSize = (int) context.getResources().getDimension(R.dimen.country_flag_size);
		Drawable drawable = getCountryFlag(context, userCountry);
		// get bitmap to scale properly
		Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

		return new BitmapDrawable(context.getResources(), Bitmap.createScaledBitmap(bitmap, flagSize, flagSize, true));
	}

	public static Drawable getUserFlag(Context context) {
		String userCountry = AppData.getUserCountry(context);
		return getCountryFlag(context, userCountry);
	}

	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static int[] getValidThemeBackIds() {
		return new int[] {
				R.drawable.img_theme_green_felt
//				,
//				R.drawable.img_theme_dueling_tigers
//				,
//				R.drawable.img_theme_blackwood,
//				R.drawable.img_theme_blackstone,
//				R.drawable.img_theme_charcoal,
//				R.drawable.img_theme_agua,
//				R.drawable.img_theme_grey_felt,
//				R.drawable.img_theme_grass
		};
	}

	public static boolean isThemeBackIdValid(int backId){
		int[] validThemeBackIds = getValidThemeBackIds();
		for (int validThemeBackId : validThemeBackIds) {
			if (validThemeBackId == backId){
				return true;
			}
		}
		return false;
	}

	public static void iconRestore() { // TODO restore icons

	}

}
