/*
 * Copyright 2011 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package actionbarcompat;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.chess.R;
import com.chess.backend.statics.AppConstants;
import com.chess.backend.statics.StaticData;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that implements the action bar pattern for pre-Honeycomb devices.
 */
public class ActionBarHelperBase extends ActionBarHelper implements View.OnClickListener {
	private static final String MENU_RES_NAMESPACE = "http://schemas.android.com/apk/res/android";
	private static final String MENU_ATTR_ID = "id";
	private static final String MENU_ATTR_SHOW_AS_ACTION = "showAsAction";

	protected Set<Integer> mActionItemIds = new HashSet<Integer>();
	private boolean noActionBar;

	protected ActionBarHelperBase(ActionBarActivity activity) {
		super(activity);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState == null || !savedInstanceState.getBoolean(AppConstants.SMALL_SCREEN))
			mActivity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		else
			noActionBar = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPostCreate(Bundle savedInstanceState) {
		mActivity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar_compat);
		setupActionBar();

		SimpleMenu menu = new SimpleMenu(mActivity);
		mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
		mActivity.onPrepareOptionsMenu(menu);
		for (int i = 0; i < menu.size(); i++) {
			MenuItem item = menu.getItem(i);
			if (mActionItemIds.contains(item.getItemId())) {
				addActionItemCompatFromMenuItem(item);
			}
		}
	}

	/**
	 * Sets up the compatibility action bar with the given title.
	 */
	private void setupActionBar() {
		final ViewGroup actionBarCompat = getActionBarCompat();
		if (actionBarCompat == null) {
			return;
		}

		LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(0,
				ViewGroup.LayoutParams.MATCH_PARENT);
		springLayoutParams.weight = 1;

		// Add Home button
		SimpleMenu tempMenu = new SimpleMenu(mActivity);
		SimpleMenuItem homeItem = new SimpleMenuItem(tempMenu, android.R.id.home, 0,
				mActivity.getString(R.string.app_name));
		homeItem.setIcon(R.drawable.ic_home);
		addActionItemCompatFromMenuItem(homeItem);

		// Add title text
		TextView titleText = new TextView(mActivity, null, R.attr.actionbarCompatTitleStyle);
		titleText.setLayoutParams(springLayoutParams);
		titleText.setText(mActivity.getTitle());
		actionBarCompat.addView(titleText);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRefreshActionItemState(boolean refreshing) {
		View refreshButton = mActivity.findViewById(R.id.actionbar_compat_item_refresh);
		View refreshIndicator = mActivity.findViewById(R.id.actionbar_compat_item_refresh_progress);

		if (refreshButton != null) {
			refreshButton.setVisibility(refreshing ? View.GONE : View.VISIBLE);
		}
		if (refreshIndicator != null) {
			refreshIndicator.setVisibility(refreshing ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void showSearchPanel(boolean show) {
		View searchButton = mActivity.findViewById(R.id.menu_search);
		View searchPanel = mActivity.findViewById(R.id.actionbar_compat_item_search_panel);
		
		if(searchButton != null){
			searchButton.setVisibility(show? View.GONE : View.VISIBLE);
		}

		if(searchPanel != null){
			searchPanel.setVisibility(show? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public void showMenuItemById(int id, boolean show){
		if(!noActionBar) {
            View view = getActionBarCompat();
            if(view != null)
                view.findViewById(id).setVisibility(show ? View.VISIBLE : View.GONE);
        }
	}

	@Override
	public void showMenuItemById(int itemId, boolean connected, Menu menu) {

	}

	/**
	 * Action bar helper code to be run in
	 * {@link android.app.Activity#onCreateOptionsMenu(android.view.Menu)}.
	 * <p/>
	 * NOTE: This code will mark on-screen menu items as invisible.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Hides on-screen action items from the options menu.
		for (Integer id : mActionItemIds) {
			menu.findItem(id).setVisible(false);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onTitleChanged(CharSequence title, int color) {
		TextView titleView = (TextView) mActivity.findViewById(R.id.actionbar_compat_title);
		if (titleView != null) {
			titleView.setText(title);
		}
	}

	/**
	 * Returns a {@link android.view.MenuInflater} that can read action bar
	 * metadata on pre-Honeycomb devices.
	 */
	@Override
	public MenuInflater getMenuInflater(MenuInflater superMenuInflater) {
		return new WrappedMenuInflater(mActivity, superMenuInflater);
	}

	/**
	 * Returns the {@link android.view.ViewGroup} for the action bar on phones
	 * (compatibility action bar). Can return null, and will return null on
	 * Honeycomb.
	 */
	private ViewGroup getActionBarCompat() {
		return (ViewGroup) mActivity.findViewById(R.id.actionbar_compat);
	}

	/**
	 * Adds an action button to the compatibility action bar, using menu
	 * information from a {@link android.view.MenuItem}. If the menu item ID is
	 * <code>menu_refresh</code>, the menu item's state can be changed to show a
	 * loading spinner using
	 * {@link actionbarcompat.ActionBarHelperBase#setRefreshActionItemState(boolean)}
	 * .
	 */
	private View addActionItemCompatFromMenuItem(final MenuItem item) {
		final int itemId = item.getItemId();

		final ViewGroup actionBar = getActionBarCompat();
		if (actionBar == null) {
			return null;
		}

		// Create the button
		ImageButton actionButton = new ImageButton(mActivity, null,
				itemId == android.R.id.home ? R.attr.actionbarCompatItemHomeStyle : R.attr.actionbarCompatItemStyle);
		actionButton.setLayoutParams(new ViewGroup.LayoutParams((int) mActivity.getResources().getDimension(
				itemId == android.R.id.home ? R.dimen.actionbar_compat_button_home_width
						: R.dimen.actionbar_compat_button_width), ViewGroup.LayoutParams.MATCH_PARENT));
		if (itemId == R.id.menu_refresh) {
			actionButton.setId(R.id.actionbar_compat_item_refresh);
		}
		actionButton.setImageDrawable(item.getIcon());
		actionButton.setScaleType(ImageView.ScaleType.CENTER);
		actionButton.setContentDescription(item.getTitle());
		actionButton.setId(itemId);
		actionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
			}
		});

		actionBar.addView(actionButton);

		if (itemId == R.id.menu_refresh) {
			// Refresh buttons should be stateful, and allow for indeterminate progress indicators,
			// so add those.
			ProgressBar indicator = new ProgressBar(mActivity, null, R.attr.actionbarCompatProgressIndicatorStyle);

			final int buttonWidth = mActivity.getResources().getDimensionPixelSize(
					R.dimen.actionbar_compat_button_width);
			final int buttonHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.actionbar_compat_height);
			final int progressIndicatorWidth = buttonWidth / 2;

			LinearLayout.LayoutParams indicatorLayoutParams = new LinearLayout.LayoutParams(progressIndicatorWidth,
					progressIndicatorWidth);
			indicatorLayoutParams.setMargins((buttonWidth - progressIndicatorWidth) / 2,
					(buttonHeight - progressIndicatorWidth) / 2, (buttonWidth - progressIndicatorWidth) / 2, 0);
			indicator.setLayoutParams(indicatorLayoutParams);
			indicator.setVisibility(View.GONE);
			indicator.setId(R.id.actionbar_compat_item_refresh_progress);
			actionBar.addView(indicator);
		}


		if (itemId == R.id.menu_search) {
			actionButton.setOnClickListener(this);

			// Add search container
			LinearLayout searchPanel = new LinearLayout(mActivity);
			LinearLayout.LayoutParams searchPanelLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			searchPanel.setLayoutParams(searchPanelLayoutParams);
			searchPanel.setVisibility(View.GONE);
			searchPanel.setOrientation(LinearLayout.HORIZONTAL);
			searchPanel.setId(R.id.actionbar_compat_item_search_panel);

			// Add EditText to Panel
			EditText searchEditText = new EditText(mActivity, null, R.attr.actionbarCompatSearchEditTextStyle);
			LinearLayout.LayoutParams searchEditLayoutParams = new LinearLayout.LayoutParams(144,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			searchEditText.setLayoutParams(searchEditLayoutParams);
			searchEditText.setId(R.id.actionbar_compat_item_search_edit);
			searchEditText.addTextChangedListener(searchWatcher);
			searchEditText.setOnEditorActionListener(searchActionListener);

			searchPanel.addView(searchEditText);

			// Add search button
			ImageButton searchButton2 = new ImageButton(mActivity, null, R.attr.actionbarCompatSearchButtonStyle);
			int buttonWidth = mActivity.getResources().getDimensionPixelSize(
					R.dimen.actionbar_compat_button_width);
			int buttonHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.actionbar_compat_height);
			LinearLayout.LayoutParams searchButtonLayoutParams = new LinearLayout.LayoutParams(buttonWidth,
					buttonHeight);
			searchButton2.setLayoutParams(searchButtonLayoutParams);
			searchButton2.setScaleType(ImageView.ScaleType.CENTER);
			searchButton2.setId(R.id.actionbar_compat_item_search_button);
			searchButton2.setOnClickListener(this);
			searchButton2.setImageResource(R.drawable.ic_action_search);

			searchPanel.addView(searchButton2);

			actionBar.addView(searchPanel);
		}
		return actionButton;
	}

	private TextWatcher searchWatcher = new TextWatcher() {
		@Override
		public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
		}

		@Override
		public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			mActivity.onSearchAutoCompleteQuery(charSequence.toString());
		}

		@Override
		public void afterTextChanged(Editable editable) {

		}
	};


	private TextView.OnEditorActionListener searchActionListener = new TextView.OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
			if(actionId == EditorInfo.IME_ACTION_SEARCH || keyEvent.getAction() == KeyEvent.FLAG_EDITOR_ACTION
					|| keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER ){
				mActivity.onSearchQuery(textView.getText().toString().trim());
				return true;
			}
			return false;
		}
	};


	@Override
	public void onClick(View view) {
		if(view.getId() == R.id.menu_search){
			showSearchPanel(true);
			final EditText searchEdit = (EditText) mActivity.findViewById(R.id.actionbar_compat_item_search_edit);
//			searchEdit.setText(null);
			searchEdit.requestFocus();
			searchEdit.post(new Runnable() {
				@Override
				public void run() {
					mActivity.showKeyBoard(searchEdit);
				}
			});
		}else if(view.getId() == R.id.actionbar_compat_item_search_button){
			showSearchPanel(false);
			EditText searchEdit = (EditText) mActivity.findViewById(R.id.actionbar_compat_item_search_edit);
			String query = searchEdit.getText().toString().trim();
			if(!query.equals(StaticData.SYMBOL_EMPTY))
				mActivity.onSearchQuery(query);
		}
	}

	/**
	 * A {@link android.view.MenuInflater} that reads action bar metadata.
	 */
	private class WrappedMenuInflater extends MenuInflater {
		MenuInflater mInflater;

		public WrappedMenuInflater(Context context, MenuInflater inflater) {
			super(context);
			mInflater = inflater;
		}

		@Override
		public void inflate(int menuRes, Menu menu) {
			loadActionBarMetadata(menuRes);
			mInflater.inflate(menuRes, menu);
		}

		/**
		 * Loads action bar metadata from a menu resource, storing a list of
		 * menu item IDs that should be shown on-screen (i.e. those with
		 * showAsAction set to always or ifRoom).
		 *
		 * @param menuResId
		 */
		private void loadActionBarMetadata(int menuResId) {
			XmlResourceParser parser = null;
			try {
				parser = mActivity.getResources().getXml(menuResId);

				int eventType = parser.getEventType();
				int itemId;
				int showAsAction;

				boolean eof = false;
				while (!eof) {
					switch (eventType) {
						case XmlPullParser.START_TAG:
							if (!parser.getName().equals("item")) {
								break;
							}

							itemId = parser.getAttributeResourceValue(MENU_RES_NAMESPACE, MENU_ATTR_ID, 0);
							if (itemId == 0) {
								break;
							}

							showAsAction = parser.getAttributeIntValue(MENU_RES_NAMESPACE, MENU_ATTR_SHOW_AS_ACTION, -1);
							if (showAsAction == MenuItem.SHOW_AS_ACTION_ALWAYS
									|| showAsAction == MenuItem.SHOW_AS_ACTION_IF_ROOM
									|| showAsAction == (MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW|MenuItem.SHOW_AS_ACTION_ALWAYS)) {
								mActionItemIds.add(itemId);
							}
							break;

						case XmlPullParser.END_DOCUMENT:
							eof = true;
							break;
					}

					eventType = parser.next();
				}
			} catch (XmlPullParserException e) {
				throw new InflateException("Error inflating menu XML", e);
			} catch (IOException e) {
				throw new InflateException("Error inflating menu XML", e);
			} finally {
				if (parser != null) {
					parser.close();
				}
			}
		}

	}
}
