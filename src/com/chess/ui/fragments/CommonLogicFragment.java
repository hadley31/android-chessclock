package com.chess.ui.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import com.chess.ui.activities.CoreActivityActionBar;
import com.chess.ui.interfaces.ActiveFragmentInterface;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 02.01.13
 * Time: 10:18
 */
public class CommonLogicFragment extends BasePopupsFragment {

	private ActiveFragmentInterface activityFace;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activityFace = (ActiveFragmentInterface) activity;
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

	public void onVisibilityChanged(boolean visible) {

	}

}
