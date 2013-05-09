package com.chess.ui.fragments.daily;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.chess.R;
import com.chess.backend.GetAndSaveUserStats;
import com.chess.ui.fragments.CommonLogicFragment;
import com.chess.ui.fragments.NavigationMenuFragment;
import com.chess.ui.fragments.stats.StatsGameDetailsFragment;
import com.chess.ui.fragments.stats.StatsGameFragment;
import com.slidingmenu.lib.SlidingMenu;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 15.04.13
 * Time: 11:39
 */
public class DailyTabsFragment extends CommonLogicFragment implements RadioGroup.OnCheckedChangeListener {

	private RadioGroup tabRadioGroup;
	private int previousCheckedId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActivity().startService(new Intent(getActivity(), GetAndSaveUserStats.class));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// activate Left and Right menu fragments
		getActivityFace().setTouchModeToSlidingMenu(SlidingMenu.TOUCHMODE_FULLSCREEN);
		getActivityFace().changeLeftFragment(new NavigationMenuFragment());
//		getActivityFace().changeRightFragment(new DailyGamesNotificationFragment());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_base_tabs_frame, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		setTitle(R.string.daily_chess);

		getActivityFace().setCustomActionBarViewId(R.layout.new_home_actionbar);

		((TextView)view.findViewById(R.id.leftTabBtn)).setText(R.string.games);
		((TextView)view.findViewById(R.id.centerTabBtn)).setText(R.string.new_);
		((TextView)view.findViewById(R.id.rightTabBtn)).setText(R.string.stats);

		showActionBar(true);

//		Fragment homeGamesFragment = new DailyGameSetupFragment();
		Fragment homeGamesFragment = new DailyGamesFragment();
		changeInternalFragment(homeGamesFragment);

		tabRadioGroup = (RadioGroup) view.findViewById(R.id.tabRadioGroup);
		tabRadioGroup.setOnCheckedChangeListener(this);
		tabRadioGroup.check(R.id.leftTabBtn);// TODO centerTabBtn

		previousCheckedId = tabRadioGroup.getCheckedRadioButtonId();
	}

	@Override
	public void onResume() {
		super.onResume();
		updateTabs();

		getActivityFace().setBadgeValueForId(R.id.menu_games, 7);
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		updateTabs();
	}

	private void updateTabs() {
		int checkedButtonId = tabRadioGroup.getCheckedRadioButtonId();
		if (checkedButtonId != previousCheckedId) {
			previousCheckedId = checkedButtonId;
			switch (checkedButtonId) {
				case R.id.leftTabBtn:
					Fragment dailyGamesFragment = getFragmentManager().findFragmentByTag(DailyGamesFragment.class.getSimpleName());
					if (dailyGamesFragment == null) {
						dailyGamesFragment = new DailyGamesFragment();
					}
					changeInternalFragment(dailyGamesFragment);
					break;
				case R.id.centerTabBtn:
					Fragment dailyGameSetupFragment = getFragmentManager().findFragmentByTag(DailyGameSetupFragment.class.getSimpleName());
					if (dailyGameSetupFragment == null) {
						dailyGameSetupFragment = new DailyGameSetupFragment();
					}
					changeInternalFragment(dailyGameSetupFragment);
					break;
				case R.id.rightTabBtn:
					changeInternalFragment(StatsGameDetailsFragment.newInstance(StatsGameFragment.DAILY_CHESS));
					break;
			}
		}
	}

	private void changeInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.tab_content_frame, fragment, fragment.getClass().getSimpleName());
		transaction.addToBackStack(fragment.getClass().getSimpleName());
		transaction.commit();
	}

//	@Override
//	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {   // Should be called to enable OptionsMenu handle
//		super.onCreateOptionsMenu(menu, inflater);
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//			case R.id.menu_games:
//				getActivityFace().toggleRightMenu();
//				break;
//		}
//		return true;
//	}

	@Override
	public void onPositiveBtnClick(DialogFragment fragment) {
		String tag = fragment.getTag();
		if (isTagEmpty(fragment)) {
			return;
		}

		if (tag.equals("test")) {
			showToast("test ok passed");
		}
		super.onPositiveBtnClick(fragment);
	}
}
