package com.chess.ui.fragments.sign_in;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.entity.new_api.RegisterItem;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.AppData;
import com.chess.backend.statics.FlurryData;
import com.chess.backend.statics.StaticData;
import com.chess.backend.tasks.RequestJsonTask;
import com.chess.ui.fragments.BasePopupsFragment;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.utilities.AppUtils;
import com.facebook.android.Facebook;
import com.flurry.android.FlurryAgent;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 30.12.12
 * Time: 20:07
 */
public class SignUpFragment extends ProfileSetupsFragment implements View.OnClickListener{

	protected Pattern emailPattern = Pattern.compile("[a-zA-Z0-9\\._%\\+\\-]+@[a-zA-Z0-9\\.\\-]+\\.[a-zA-Z]{2,4}");
	protected Pattern gMailPattern = Pattern.compile("[a-zA-Z0-9\\._%\\+\\-]+@[g]");   // TODO use for autoComplete
	private static final String DEFAULT_COUNTRY = "XX";  // International

	private EditText userNameEdt;
	private EditText emailEdt;
	private EditText passwordEdt;
	private EditText passwordRetypeEdt;

	private String userName;
	private String email;
	private String password;
	private RegisterUpdateListener registerUpdateListener;
	private String[] countryCodes;
	private String countryCodeName;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_sign_up_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		countryCodes = getResources().getStringArray(R.array.new_countries_codes);

		userNameEdt = (EditText) view.findViewById(R.id.usernameEdt);
		emailEdt = (EditText) view.findViewById(R.id.emailEdt);
		passwordEdt = (EditText) view.findViewById(R.id.passwordEdt);
		passwordRetypeEdt = (EditText) view.findViewById(R.id.passwordRetypeEdt);
		view.findViewById(R.id.RegSubmitBtn).setOnClickListener(this);

		userNameEdt.addTextChangedListener(new FieldChangeWatcher(userNameEdt));
		emailEdt.addTextChangedListener(new FieldChangeWatcher(emailEdt));
		passwordEdt.addTextChangedListener(new FieldChangeWatcher(passwordEdt));
		passwordRetypeEdt.addTextChangedListener(new FieldChangeWatcher(passwordRetypeEdt));

		setLoginFields(userNameEdt, passwordEdt);
//		Spinner countrySpinner = (Spinner) view.findViewById(R.id.country);  // TODO create in CreateProfileFragment
//		countrySpinner.setAdapter(new WhiteSpinnerAdapter(getActivity(), getItemsFromArray(tmp2)));
//		countrySpinner.setOnItemSelectedListener(this);


		registerUpdateListener = new RegisterUpdateListener();
	}

	@Override
	public void onResume() {
		super.onResume();

		String userCountry = AppData.getUserCountry(getActivity());
		if (userCountry == null) {
			String locale = getResources().getConfiguration().locale.getCountry();

			if (locale != null) {
				int i;
				boolean found = false;
				for (i = 0; i < countryCodes.length; i++) {
					String countryCode = countryCodes[i];
					if (locale.equals(countryCode)) {
						found = true;
						break;
					}
				}
				if (found) {
					countryCodeName = countryCodes[i];
				} else {
					countryCodeName = DEFAULT_COUNTRY;
				}
			}
		}
	}

	@Override
	public void onClick(View view) {
		super.onClick(view);
		if (view.getId() == R.id.RegSubmitBtn) {
			if (!checkRegisterInfo()){
				return;
			}

			if (!AppUtils.isNetworkAvailable(getActivity())){ // check only if live
				popupItem.setPositiveBtnId(R.string.wireless_settings);
				showPopupDialog(R.string.warning, R.string.no_network, BasePopupsFragment.NETWORK_CHECK_TAG);
				return;
			}

			submitRegisterInfo();
		}
	}

	private boolean checkRegisterInfo() {
		userName = encodeField(userNameEdt);
		email = encodeField(emailEdt);
		password = encodeField(passwordEdt);

		if (userName.length() < 3) {
			userNameEdt.setError(getString(R.string.too_short));
			userNameEdt.requestFocus();
			return false;
		}

		if (!emailPattern.matcher(getTextFromField(emailEdt)).matches()) {
			emailEdt.setError(getString(R.string.invalidEmail));
			emailEdt.requestFocus();
			return true;
		}

		if (email.equals(StaticData.SYMBOL_EMPTY)) {
			emailEdt.setError(getString(R.string.can_not_be_empty));
			emailEdt.requestFocus();
			return false;
		}

		if (password.length() < 6) {
			passwordEdt.setError(getString(R.string.too_short));
			passwordEdt.requestFocus();
			return false;
		}

		if (!password.equals(passwordRetypeEdt.getText().toString())) {
			passwordRetypeEdt.setError(getString(R.string.pass_dont_match));
			passwordRetypeEdt.requestFocus();
			return false;
		}


		return true;
	}

	private void submitRegisterInfo() {
		LoadItem loadItem = new LoadItem();
//		loadItem.setLoadPath(RestHelper.REGISTER);
		loadItem.setLoadPath(RestHelper.CMD_REGISTER);
		loadItem.setRequestMethod(RestHelper.POST);
		loadItem.addRequestParams(RestHelper.P_USER_NAME, userName);
		loadItem.addRequestParams(RestHelper.P_PASSWORD, password);
		loadItem.addRequestParams(RestHelper.P_EMAIL, email);
		loadItem.addRequestParams(RestHelper.P_COUNTRY_CODE, countryCodeName);
		loadItem.addRequestParams(RestHelper.P_APP_TYPE, RestHelper.V_ANDROID);

//		new GetStringObjTask(registerUpdateListener).executeTask(loadItem);
		new RequestJsonTask<RegisterItem>(registerUpdateListener).executeTask(loadItem);
	}

	private class RegisterUpdateListener extends CommonLogicFragment.ChessUpdateListener<RegisterItem> {

		public RegisterUpdateListener() {
			super(RegisterItem.class);
		}

		@Override
		public void updateData(RegisterItem returnedObj) {
			FlurryAgent.logEvent(FlurryData.NEW_ACCOUNT_CREATED);
			showToast(R.string.congratulations);

			preferencesEditor.putString(AppConstants.USERNAME, userNameEdt.getText().toString().toLowerCase());
			preferencesEditor.putInt(AppConstants.USER_PREMIUM_STATUS, RestHelper.V_BASIC_MEMBER);
			processLogin(returnedObj.getData());
		}
	}


//	@Override
//	protected void afterLogin() {
//		FlurryAgent.logEvent(FlurryData.LOGGED_IN);     // duplicate logic -> moved to CommonLogicFragment class
////		if (AppData.isNotificationsEnabled(getActivity())){
////			checkMove();
////		}
//
//		backToHomeFragment();
//	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {  // TODO restore
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == Activity.RESULT_OK ){
			if(requestCode == Facebook.DEFAULT_AUTH_ACTIVITY_CODE){
				CommonLogicFragment.facebook.authorizeCallback(requestCode, resultCode, data);
			}else if(requestCode == BasePopupsFragment.NETWORK_REQUEST){
				submitRegisterInfo();
			}
		}
	}

	private String encodeField(EditText editText) {
		String value = "";
		try {
			value = URLEncoder.encode(getTextFromField(editText), HTTP.UTF_8);
		} catch (UnsupportedEncodingException e) {
			editText.setError(getString(R.string.encoding_unsupported));
		}
		return value;
	}

	private class FieldChangeWatcher implements TextWatcher {
		private EditText editText;

		public FieldChangeWatcher(EditText editText) {
			this.editText = editText;
		}

		@Override
		public void onTextChanged(CharSequence str, int start, int before, int count) {
			if (str.length() > 1) {
				editText.setError(null);

//				if (gMailPattern.matcher(getTextFromField(emailEdt)).matches()){ // TODO use with autoComplete
//					emailEdt.setText(str + "mail.com");
//					emailEdt.requestFocus();
//				}
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void afterTextChanged(Editable s) {
		}
	}

}