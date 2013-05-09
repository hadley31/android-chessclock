package com.chess.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.ServerErrorCode;
import com.chess.backend.entity.DataHolder;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.TacticsDataHolder;
import com.chess.backend.entity.new_api.LoginItem;
import com.chess.backend.entity.new_api.RegisterItem;
import com.chess.backend.interfaces.AbstractUpdateListener;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.FlurryData;
import com.chess.backend.statics.SoundPlayer;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.ui.activities.CoreActivityActionBar;
import com.chess.ui.fragments.daily.DailyGamesNotificationFragment;
import com.chess.ui.fragments.home.HomePlayFragment;
import com.chess.ui.fragments.home.HomeTabsFragment;
import com.chess.ui.fragments.sign_in.SignInFragment;
import com.chess.ui.interfaces.ActiveFragmentInterface;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.flurry.android.FlurryAgent;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

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

	protected static final String MODE = "mode";

	public static final int CENTER_MODE = 1;
	public static final int RIGHT_MENU_MODE = 2;

	//	private LoginUpdateListener loginUpdateListener;
	private LoginUpdateListenerNew loginUpdateListener;

	private int loginReturnCode;
	private ActiveFragmentInterface activityFace;
//	protected static Facebook facebook;
	protected static Handler handler;
	private EditText loginUsernameEdt;
	private EditText passwordEdt;

	protected SharedPreferences preferences;
	protected SharedPreferences.Editor preferencesEditor;
	private int titleId;
	private GraphUser facebookUser;
	private UiLifecycleHelper facebookUiHelper;
	private boolean facebookActive;
	protected View loadingView;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityFace = (ActiveFragmentInterface) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		preferences = AppData.getPreferences(getActivity());
		preferencesEditor = preferences.edit();

		handler = new Handler();
		setHasOptionsMenu(true);
	}

//	protected void setTitle(int titleId) {
//		getActivity().setTitle(titleId);
//	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		loadingView = view.findViewById(R.id.loadingView);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
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

		updateTitle();
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
		this.titleId = titleId;
	}

	protected void updateTitle(int titleId) {
		getActivityFace().updateTitle(titleId);
	}

	private void updateTitle() {
		if (titleId != 0) {
			getActivityFace().updateTitle(titleId);
		}
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
				facebookUser = user;
			}
		});


		loginUpdateListener = new LoginUpdateListenerNew();
	}

	private Session.StatusCallback callback = new Session.StatusCallback() {
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

	private void loginWithFacebook(Session session){
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.CMD_LOGIN);
		loadItem.setRequestMethod(RestHelper.POST);
		loadItem.addRequestParams(RestHelper.P_FACEBOOK_ACCESS_TOKEN, session.getAccessToken());
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

	protected ActiveFragmentInterface getActivityFace (){
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

	protected void signInUser(){
		String userName = getTextFromField(loginUsernameEdt);
		if (userName.length() < MIN_USERNAME_LENGTH || userName.length() > MAX_USERNAME_LENGTH) {
			loginUsernameEdt.setError(getString(R.string.validateUsername));
			loginUsernameEdt.requestFocus();
			return;
		}

		String pass = getTextFromField(passwordEdt);
		if (pass.length() == 0) {
			passwordEdt.setError(getString(R.string.invalid_password));
			passwordEdt.requestFocus();
			return;
		}

		LoadItem loadItem = new LoadItem();
//		loadItem.setLoadPath(RestHelper.LOGIN);
		loadItem.setLoadPath(RestHelper.CMD_LOGIN);
		loadItem.setRequestMethod(RestHelper.POST);
//		loadItem.addRequestParams(RestHelper.P_USER_NAME, userName);
		loadItem.addRequestParams(RestHelper.P_USER_NAME_OR_MAIL, userName);
		loadItem.addRequestParams(RestHelper.P_PASSWORD, getTextFromField(passwordEdt));
		loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.P_USER_NAME);
		loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.P_TACTICS_RATING);

//		new PostDataTask(loginUpdateListener).executeTask(loadItem);
		new RequestJsonTask<LoginItem>(loginUpdateListener).executeTask(loadItem);

		loginReturnCode = SIGNIN_CALLBACK_CODE;
	}

	@Override
	public void onClick(View view) {

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
				loadingView.setVisibility(show? View.VISIBLE: View.GONE);
			}
		}
	}

	private class LoginUpdateListenerNew extends AbstractUpdateListener<LoginItem> {
		public LoginUpdateListenerNew() {
			super(getContext(), LoginItem.class);
		}

		@Override
		public void showProgress(boolean show) {
			if (show){
				showPopupHardProgressDialog(R.string.signing_in_);
			} else {
				if(isPaused)
					return;

				dismissProgressDialog();
			}
		}

		@Override
		public void updateData(LoginItem returnedObj) {
			if (loginReturnCode == SIGNIN_FACEBOOK_CALLBACK_CODE) {
				FlurryAgent.logEvent(FlurryData.FB_LOGIN);
			}
			if (!TextUtils.isEmpty(returnedObj.getData().getUsername())) {
				preferencesEditor.putString(AppConstants.USERNAME, returnedObj.getData().getUsername().trim().toLowerCase());
			}
//			preferencesEditor.putString(AppConstants.PREF_USER_AVATAR_URL, returnedObj.getData().getAvatarUrl()); // TODO restore
			preferencesEditor.putString(AppConstants.PREF_USER_AVATAR_URL, "http://d1lalstwiwz2br.cloudfront.net/images_users/avatars/erik_l.gif");
			if (returnedObj.getData().getTacticsRating() != 0) {
				preferencesEditor.putInt(AppConstants.PREF_USER_TACTICS_RATING, returnedObj.getData().getTacticsRating());
			}
			preferencesEditor.putInt(AppConstants.USER_PREMIUM_STATUS, returnedObj.getData().getPremiumStatus());
			processLogin(returnedObj.getData());
		}

		@Override
		public void errorHandle(Integer resultCode) {
			dismissProgressDialog();

			if (RestHelper.containsServerCode(resultCode)) {
				// get server code
				int serverCode = RestHelper.decodeServerCode(resultCode);
				switch (serverCode){
					case ServerErrorCode.INVALID_USERNAME_PASSWORD:
						passwordEdt.setError(getResources().getString(R.string.invalid_password));
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
			} else {
			}
		}
	}

	protected void processLogin(RegisterItem.Data returnedObj) {
		if (passwordEdt == null) { // if accidently return in wrong callback, when widgets are not initialized
			return;
		}

		preferencesEditor.putString(AppConstants.PASSWORD, passwordEdt.getText().toString().trim());

		try {
			preferencesEditor.putString(AppConstants.USER_TOKEN, URLEncoder.encode(returnedObj.getLoginToken(), HTTP.UTF_8));
		} catch (UnsupportedEncodingException ignored) {
			preferencesEditor.putString(AppConstants.USER_TOKEN, returnedObj.getLoginToken());
//			showSinglePopupDialog(R.string.error, R.string.error_occurred_while_login); // or use that logic?
//			return;
		}
// 		preferencesEditor.putString(AppConstants.USER_SESSION_ID, response[3]); // TODO used only for live, so should be separate connection to live
		preferencesEditor.commit();

		if (getActivity() == null) {
			return;
		}

		AppData.setLiveChessMode(getActivity(), false);
		DataHolder.reset();
		TacticsDataHolder.reset();

		getActivityFace().registerGcm();

		afterLogin();
	}

	protected void afterLogin(){
		FlurryAgent.logEvent(FlurryData.LOGGED_IN);
//		if (AppData.isNotificationsEnabled(getActivity())){
//			checkMove();
//		}

		backToHomeFragment();
	}

//	public class SampleAuthListener implements SessionEvents.AuthListener {
//		@Override
//		public void onAuthSucceed() {
//			LoadItem loadItem = new LoadItem();
//			loadItem.setLoadPath(RestHelper.CMD_LOGIN);
//			loadItem.setRequestMethod(RestHelper.POST);
//			loadItem.addRequestParams(RestHelper.P_FACEBOOK_ACCESS_TOKEN, facebook.getAccessToken());
//			loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.V_USERNAME);
//			loadItem.addRequestParams(RestHelper.P_FIELDS, RestHelper.V_TACTICS_RATING);
//
//			new RequestJsonTask<LoginItem>(loginUpdateListener).executeTask(loadItem);
//			loginReturnCode = SIGNIN_FACEBOOK_CALLBACK_CODE;
//		}
//
//		@Override
//		public void onAuthFail(String error) {
//			showToast(getString(R.string.login_failed)+ StaticData.SYMBOL_SPACE + error);
//		}
//	}

//	public class SampleLogoutListener implements SessionEvents.LogoutListener {
//		@Override
//		public void onLogoutBegin() {
//			showToast(R.string.login_out);
//		}
//
//		@Override
//		public void onLogoutFinish() {
//			showToast(R.string.you_logged_out);
//		}
//	}

//	protected List<String> getItemsFromArray(String[] array){
//		List<String> items = new ArrayList<String>();
//		items.addAll(Arrays.asList(array));
//		return items;
//	}

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//		inflater.inflate(R.menu.sign_out, menu);
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//			case android.R.id.home:
//				getActivityFace().toggleLeftMenu();
//				break;
//		}
//		return true;
//	}

//	protected LccHelper getLccHolder() {
//		return getActivityFace().getMeLccHolder();
//	}

	protected Fragment findFragmentByTag(String tag) {
		return getFragmentManager().findFragmentByTag(tag);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {   // Should be called to enable OptionsMenu handle
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_games:
				getActivityFace().changeRightFragment(HomePlayFragment.newInstance(RIGHT_MENU_MODE));
				getActivityFace().toggleRightMenu();
				break;
			case R.id.menu_notifications:
				getActivityFace().changeRightFragment(new DailyGamesNotificationFragment());
				getActivityFace().toggleRightMenu();
				break;
		}
		return true;
	}

	protected void logTest(String messageToLog) {
		Log.d("TEST", messageToLog);
	}
}
