package com.chess.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.ServerErrorCode;
import com.chess.backend.entity.DataHolder;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.TacticsDataHolder;
import com.chess.backend.entity.new_api.LoginItem;
import com.chess.backend.entity.new_api.RegisterItem;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.backend.statics.*;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.ui.activities.CoreActivityActionBar;
import com.chess.ui.engine.ChessBoardComp;
import com.chess.ui.fragments.daily.DailyGamesNotificationFragment;
import com.chess.ui.fragments.home.HomePlayFragment;
import com.chess.ui.fragments.home.HomeTabsFragment;
import com.chess.ui.fragments.welcome.SignInFragment;
import com.chess.ui.fragments.welcome.WelcomeTabsFragment;
import com.chess.ui.interfaces.ActiveFragmentInterface;
import com.chess.utilities.AppUtils;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.flurry.android.FlurryAgent;
import com.slidingmenu.lib.SlidingMenu;

import java.util.Arrays;

import static com.chess.backend.statics.AppConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 02.01.13
 * Time: 10:18
 */
public abstract class CommonLogicFragment extends BasePopupsFragment implements View.OnClickListener {

	private static final int SIGNIN_FACEBOOK_CALLBACK_CODE = 128;
	private static final int SIGNIN_CALLBACK_CODE = 16;
	protected static final long FACEBOOK_DELAY = 200;
	private static final int MIN_USERNAME_LENGTH = 3;
	private static final int MAX_USERNAME_LENGTH = 20;

	protected static final String RE_LOGIN_TAG = "re-login popup";
	protected static final String NETWORK_CHECK_TAG = "network check popup";
	protected static final int NETWORK_REQUEST = 3456;
	protected static final String CHESS_NO_ACCOUNT_TAG = "chess no account popup";
	protected static final String CHECK_UPDATE_TAG = "check update";

	protected static final String MODE = "mode";
	protected static final String CONFIG = "config";

	public static final int CENTER_MODE = 1;
	public static final int RIGHT_MENU_MODE = 2;

	protected static final int DEFAULT_ICON = 0;
	protected static final int ONE_ICON = 1;
	protected static final int TWO_ICON = 2;
	protected static final long SIDE_MENU_DELAY = 150;
	private static final long SWITCH_DELAY = 50;

	private LoginUpdateListener loginUpdateListener;

	private int loginReturnCode;
	private ActiveFragmentInterface activityFace;
	protected static Handler handler;
	private EditText loginUsernameEdt;
	private EditText passwordEdt;

	protected SharedPreferences preferences;
	protected SharedPreferences.Editor preferencesEditor;
	private CharSequence title;
	protected UiLifecycleHelper facebookUiHelper;
	private boolean facebookActive;
	protected View loadingView;
	private int padding;
	private int paddingCode;
	private boolean slideMenusEnabled;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityFace = (ActiveFragmentInterface) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = getAppData().getPreferences();
		preferencesEditor = getAppData().getEditor();

		handler = new Handler();
		setHasOptionsMenu(true);

		padding = (int) (48 * getResources().getDisplayMetrics().density);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		enableSlideMenus(true);

		loadingView = view.findViewById(R.id.loadingView);

		getActivityFace().showActionMenu(R.id.menu_add, false);
		getActivityFace().showActionMenu(R.id.menu_search, false);
		getActivityFace().showActionMenu(R.id.menu_share, false);
		getActivityFace().showActionMenu(R.id.menu_cancel, false);
		getActivityFace().showActionMenu(R.id.menu_accept, false);
		getActivityFace().showActionMenu(R.id.menu_edit, false);
		getActivityFace().showActionMenu(R.id.menu_message, false);
		getActivityFace().showActionMenu(R.id.menu_challenge, false);
		getActivityFace().showActionMenu(R.id.menu_notifications, true);
		getActivityFace().showActionMenu(R.id.menu_games, true);

		setTitlePadding(DEFAULT_ICON);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getActivityFace().setTouchModeToSlidingMenu(slideMenusEnabled? SlidingMenu.TOUCHMODE_FULLSCREEN
				: SlidingMenu.TOUCHMODE_NONE);

		LoginButton loginButton = (LoginButton) getView().findViewById(R.id.fb_connect);
		if (loginButton != null) {
			facebookUiHelper = new UiLifecycleHelper(getActivity(), callback);
			facebookUiHelper.onCreate(savedInstanceState);
			facebookActive = true;
			facebookInit(loginButton);
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// update title and padding here when activity will finish it's setups for actionbar
		updateTitle();
		setTitlePadding();
		getActivityFace().updateActionBarIcons();

		if (facebookActive) {
			facebookUiHelper.onResume();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (facebookActive) {
			facebookUiHelper.onPause();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (facebookActive) {
			facebookUiHelper.onSaveInstanceState(outState);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (facebookActive) {
			facebookUiHelper.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (facebookActive) {
			facebookUiHelper.onDestroy();
		}
	}

	protected void showActionBar(boolean show) {
		getActivityFace().showActionBar(show);
	}

	protected void setTitle(int titleId) {
		this.title = getString(titleId);
	}

	protected void setTitle(String title) {
		this.title = title;
	}

	/**
	 * ONE_ICON means minus 1 from default state, because be default there are 2 icons on right side
	 * @param code can be 1 or 2 - corresponds for one or two icons offset
	 */
	protected void setTitlePadding(int code) {
		paddingCode = code;
	}

	private void setTitlePadding() {
		if (paddingCode == ONE_ICON) {
			getActivityFace().setTitlePadding(0);
		} else if (paddingCode == TWO_ICON) { // TODO
			getActivityFace().setTitlePadding(padding);
		} else if (paddingCode == DEFAULT_ICON) {
			getActivityFace().setTitlePadding(padding);
		}
	}

	private void updateTitle() {
		if (!TextUtils.isEmpty(title)) {
			getActivityFace().updateTitle(title);
		}
	}

	protected void enableSlideMenus(boolean enable) {
		slideMenusEnabled = enable;
	}

	protected void unRegisterGcmService() {
		getActivityFace().unRegisterGcm();
	}

	protected void facebookInit(LoginButton loginBtn) {
		loginBtn.setFragment(this);
		loginBtn.setReadPermissions(Arrays.asList("user_status", "email"));
		loginBtn.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
			@Override
			public void onUserInfoFetched(GraphUser user) {
			}
		});

		loginUpdateListener = new LoginUpdateListener();
	}

	protected Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception); // TODO create protected method to inform who need
		}
	};

	protected void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (state != null && state.isOpened()) {
			loginWithFacebook(session);
		}
	}

	private void loginWithFacebook(Session session) {
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_LOGIN);
		loadItem.setRequestMethod(RestHelper.POST);
		loadItem.addRequestParams(RestHelper.P_FACEBOOK_ACCESS_TOKEN, session.getAccessToken());
		loadItem.addRequestParams(RestHelper.P_DEVICE_ID, getDeviceId());
		loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.V_USERNAME);
		loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.V_TACTICS_RATING);

		new RequestJsonTask<LoginItem>(loginUpdateListener).executeTask(loadItem);
		loginReturnCode = SIGNIN_FACEBOOK_CALLBACK_CODE;
	}

	protected void setLoginFields(EditText passedUsernameEdt, EditText passedPasswordEdt) {
		this.loginUsernameEdt = passedUsernameEdt;
		this.passwordEdt = passedPasswordEdt;
	}

	protected CoreActivityActionBar getInstance() {
		return activityFace.getActionBarActivity();
	}

	protected ActiveFragmentInterface getActivityFace() {
		return activityFace;
	}

	protected void registerReceiver(BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
		getActivity().registerReceiver(broadcastReceiver, intentFilter);
	}

	protected void unRegisterMyReceiver(BroadcastReceiver broadcastReceiver) {
		if (broadcastReceiver != null) {
			getActivity().unregisterReceiver(broadcastReceiver);
		}
	}

	protected void backToHomeFragment() {
		getActivityFace().clearFragmentStack();
		getActivityFace().switchFragment(new HomeTabsFragment());
	}

	protected void backToLoginFragment() {
		getActivityFace().switchFragment(new SignInFragment());
	}

	public SoundPlayer getSoundPlayer() {
		return SoundPlayer.getInstance(getActivity());
	}

	protected void signInUser() {
		String userName = getTextFromField(loginUsernameEdt);
		if (userName.length() < MIN_USERNAME_LENGTH || userName.length() > MAX_USERNAME_LENGTH) {
			loginUsernameEdt.setError(getString(R.string.validateUsername));
			loginUsernameEdt.requestFocus();
			return;
		}

		String pass = getTextFromField(passwordEdt);
		if (pass.length() == 0) {
			passwordEdt.setError(getString(R.string.password_cant_be_empty));
			passwordEdt.requestFocus();
			return;
		}

		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_LOGIN);
		loadItem.setRequestMethod(RestHelper.POST);
		loadItem.addRequestParams(RestHelper.P_DEVICE_ID, getDeviceId());
		loadItem.addRequestParams(RestHelper.P_USER_NAME_OR_MAIL, userName);
		loadItem.addRequestParams(RestHelper.P_PASSWORD, getTextFromField(passwordEdt));
		loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.P_USERNAME);
		loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.P_TACTICS_RATING);

		new RequestJsonTask<LoginItem>(loginUpdateListener).executeTask(loadItem);

		loginReturnCode = SIGNIN_CALLBACK_CODE;
	}

	@Override
	public void onClick(View view) {

	}

	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {

		String tag = fragment.getTag();
		if (tag == null) {
			super.onPositiveBtnClick(fragment);
			return;
		}

		if (tag.equals(RE_LOGIN_TAG)) {
			performLogout();
		}
		super.onPositiveBtnClick(fragment);
	}

	protected void setBadgeValueForId(int menuId, int value) {
		getActivityFace().setBadgeValueForId(menuId, value);
	}

	protected String getDeviceId() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			String string = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
			while ((string != null ? string.length() : 0) < 32) { // 32 length is requirement for deviceId parameter
				string += "a";
			}
			return string;
		} else {
			return StaticData.SYMBOL_EMPTY;
		}
	}

	protected class ChessUpdateListener<ItemType> extends ActionBarUpdateListener<ItemType> {

		public ChessUpdateListener(Class<ItemType> clazz) {
			super(getInstance(), CommonLogicFragment.this, clazz);
		}

		public ChessUpdateListener() {
			super(getInstance(), CommonLogicFragment.this);
		}

		@Override
		public void showProgress(boolean show) {
			super.showProgress(show);
			if (loadingView != null) {
				loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
			}
		}

		@Override
		public void errorHandle(Integer resultCode) {
			super.errorHandle(resultCode);

			// show message only for re-login
			if (RestHelper.containsServerCode(resultCode)) {
				int serverCode = RestHelper.decodeServerCode(resultCode);
				if (serverCode == ServerErrorCode.INVALID_LOGIN_TOKEN_SUPPLIED) {

					safeShowSinglePopupDialog(R.string.session_expired, RE_LOGIN_TAG);
				}
			}
		}
	}

	protected class ChessLoadUpdateListener<ItemType> extends ChessUpdateListener<ItemType> {

		public ChessLoadUpdateListener() {
			super();
		}

		public ChessLoadUpdateListener(Class<ItemType> clazz) {
			super(clazz);
		}

		@Override
		public void showProgress(boolean show) {
			showLoadingProgress(show);
		}
	}

	private class LoginUpdateListener extends ChessLoadUpdateListener<LoginItem> {
		public LoginUpdateListener() {
			super(LoginItem.class);
		}

		@Override
		public void updateData(LoginItem returnedObj) {
			if (loginReturnCode == SIGNIN_FACEBOOK_CALLBACK_CODE) {
				FlurryAgent.logEvent(FlurryData.FB_LOGIN);
			}
			String username = returnedObj.getData().getUsername();
			if (!TextUtils.isEmpty(username)) {
				preferencesEditor.putString(USERNAME, username);
			}

			preferencesEditor.putString(username + PREF_USER_AVATAR_URL, returnedObj.getData().getAvatarUrl());
			preferencesEditor.putInt(username + PREF_USER_COUNTRY_ID, returnedObj.getData().getCountryId());

			if (returnedObj.getData().getTacticsRating() != 0) {
				preferencesEditor.putInt(username + PREF_USER_TACTICS_RATING, returnedObj.getData().getTacticsRating());
			}
			preferencesEditor.putInt(username + USER_PREMIUM_STATUS, returnedObj.getData().getPremiumStatus());
			preferencesEditor.putString(LIVE_SESSION_ID, returnedObj.getData().getSessionId());
			processLogin(returnedObj.getData());
		}

		@Override
		public void errorHandle(Integer resultCode) {
			dismissProgressDialog();

			if (RestHelper.containsServerCode(resultCode)) {
				// get server code
				int serverCode = RestHelper.decodeServerCode(resultCode);
				switch (serverCode) {
					case ServerErrorCode.INVALID_USERNAME_PASSWORD:
						showSinglePopupDialog(R.string.login, R.string.invalid_username_or_password);
						passwordEdt.requestFocus();
						break;
					case ServerErrorCode.FACEBOOK_USER_NO_ACCOUNT:
						popupItem.setPositiveBtnId(R.string.sign_up);
						showPopupDialog(R.string.no_chess_account_signup_please, CHESS_NO_ACCOUNT_TAG);
						break;
					default:
						String serverMessage = ServerErrorCode.getUserFriendlyMessage(getActivity(), serverCode); // TODO restore
						showToast(serverMessage);

						break;
				}
			}
		}

		@Override
		public void errorHandle(String resultMessage) {
			if (resultMessage.contains(RestHelper.R_FB_USER_HAS_NO_ACCOUNT)) {
				popupItem.setPositiveBtnId(R.string.sign_up);
				showPopupDialog(R.string.no_chess_account_signup_please, CHESS_NO_ACCOUNT_TAG);
			}
		}
	}

	protected void processLogin(RegisterItem.Data returnedObj) {
		if (passwordEdt == null || getActivity() == null) { // if accidentally return in wrong callback, when widgets are not initialized
			return;
		}

		preferencesEditor.putString(PASSWORD, getTextFromField(passwordEdt));
		preferencesEditor.putString(USER_TOKEN, returnedObj.getLoginToken());
		preferencesEditor.commit();

		getAppData().setLiveChessMode(false);
		DataHolder.reset();
		TacticsDataHolder.reset();

		getActivityFace().registerGcm();

		afterLogin();

		String themeBackPath = getAppData().getThemeBackPath();
		if (!TextUtils.isEmpty(themeBackPath)) {
			getActivityFace().setMainBackground(themeBackPath);
		} else {
			getActivityFace().setMainBackground(getAppData().getThemeBackId());
		}
	}

	protected void afterLogin() {
		FlurryAgent.logEvent(FlurryData.LOGGED_IN);
		backToHomeFragment();
	}

	protected void showLoadingProgress(boolean show) {
		if (show) {
			showPopupProgressDialog(R.string.loading_);
		} else {
			if (isPaused)
				return;

			dismissProgressDialog();
		}
	}

	protected Fragment findFragmentByTag(String tag) {
		return getFragmentManager().findFragmentByTag(tag);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_games:
				getActivityFace().changeRightFragment(HomePlayFragment.createInstance(RIGHT_MENU_MODE));
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						getActivityFace().toggleRightMenu();
					}
				}, SIDE_MENU_DELAY);
				return true;
			case R.id.menu_notifications:
				CommonLogicFragment fragment = (CommonLogicFragment) findFragmentByTag(DailyGamesNotificationFragment.class.getSimpleName());
				if (fragment == null) {
					fragment = new DailyGamesNotificationFragment();
				}
				getActivityFace().changeRightFragment(fragment);
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						getActivityFace().toggleRightMenu();
					}
				}, SIDE_MENU_DELAY);
				return true;
		}
		return false;
	}

	protected void logTest(String messageToLog) {
		Log.d("TEST", messageToLog);
	}

	protected AppData getAppData() {
		return getActivityFace().getMeAppData();
	}

	protected String getUsername() {
		return getActivityFace().getMeUsername();
	}

	protected String getUserToken() {
		return getActivityFace().getMeUserToken();
	}

	protected void setFacebookActive(boolean active) {
		facebookActive = active;
	}

	protected void performLogout() {
		// un-register from GCM
		unRegisterGcmService();

		// logout from facebook
		Session facebookSession = Session.getActiveSession();
		if (facebookSession != null) {
			facebookSession.closeAndClearTokenInformation();
			Session.setActiveSession(null);
		}

		preferencesEditor.putString(AppConstants.PASSWORD, StaticData.SYMBOL_EMPTY);
		preferencesEditor.putString(AppConstants.USER_TOKEN, StaticData.SYMBOL_EMPTY);
		preferencesEditor.commit();

		AppUtils.cancelNotifications(getActivity());
		getActivityFace().clearFragmentStack();
		// make pause to wait while transactions complete
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				getActivityFace().switchFragment(new WelcomeTabsFragment());
			}
		}, SWITCH_DELAY);

		// set default theme
		getActivityFace().setMainBackground(R.drawable.img_theme_green_felt);

		// clear comp game
		ChessBoardComp.resetInstance();
		getAppData().clearSavedCompGame();

		// clear username
		getAppData().setUserName(AppConstants.GUEST_NAME);
	}


	public int getStatusBarHeight() {
		Rect r = new Rect();
		Window w = getActivity().getWindow();
		w.getDecorView().getWindowVisibleDisplayFrame(r);
		return r.top;
	}
//	public void printHashKey() { Don't remove, use to find needed facebook hashkey
//		try {
//			PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(),
//					PackageManager.GET_SIGNATURES);
//			for (Signature signature : info.signatures) {
//				MessageDigest md = MessageDigest.getInstance("SHA");
//				md.update(signature.toByteArray());
//				Log.d("TEMPTAGHASH KEY:",
//						Base64.encodeToString(md.digest(), Base64.DEFAULT));
//			}
//		} catch (PackageManager.NameNotFoundException e) {
//		} catch (NoSuchAlgorithmException e) {
//		}
//	}
}
