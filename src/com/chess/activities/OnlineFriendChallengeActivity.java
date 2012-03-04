package com.chess.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import com.chess.R;
import com.chess.adapters.ChessSpinnerAdapter;
import com.chess.core.AppConstants;
import com.chess.core.CoreActivityActionBar;
import com.chess.lcc.android.LccHolder;
import com.chess.utilities.ChessComApiParser;
import com.chess.utilities.MyProgressDialog;
import com.chess.views.BackgroundChessDrawable;

public class OnlineFriendChallengeActivity extends CoreActivityActionBar implements OnClickListener {
	private Spinner iplayas;
	private Spinner daysPerMoveSpinner;
	private Spinner friendsSpinner;
	private CheckBox isRated;
	private RadioButton chess960;

	private int[] daysArr = new int[]{
			1,
			2,
			3,
			5,
			7,
			14
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		init();

		setContentView(R.layout.online_challenge_friend);
		findViewById(R.id.mainView).setBackgroundDrawable(new BackgroundChessDrawable(this));

		daysPerMoveSpinner = (Spinner) findViewById(R.id.dayspermove);
		daysPerMoveSpinner.setAdapter(new ChessSpinnerAdapter(this,R.array.dayspermove ));

		chess960 = (RadioButton) findViewById(R.id.chess960);

		iplayas = (Spinner) findViewById(R.id.iplayas);
		iplayas.setAdapter(new ChessSpinnerAdapter(this, R.array.playas));

		friendsSpinner = (Spinner) findViewById(R.id.friend);
		friendsSpinner.setAdapter(new ChessSpinnerAdapter(this, new String[]{""} ));

		isRated = (CheckBox) findViewById(R.id.ratedGame);
		findViewById(R.id.createchallenge).setOnClickListener(this);
	}


	@Override
	public void Update(int code) {
		if (code == ERROR_SERVER_RESPONSE) {
			finish();
		} else if (code == INIT_ACTIVITY && !mainApp.isLiveChess()) {
			if (appService != null) {
				appService.RunSingleTask(0,
						"http://www." + LccHolder.HOST + "/api/get_friends?id=" + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, ""),
						progressDialog = new MyProgressDialog(ProgressDialog.show(OnlineFriendChallengeActivity.this, null, getString(R.string.gettingfriends), true))
				);
			}
		} else if (code == 0 || (code == INIT_ACTIVITY && mainApp.isLiveChess())) {
			String[] FRIENDS;
			if (mainApp.isLiveChess()) {
				FRIENDS = lccHolder.getOnlineFriends();
			} else {
				FRIENDS = ChessComApiParser.GetFriendsParse(response);
			}

			ArrayAdapter<String> adapterF = new ArrayAdapter<String>(OnlineFriendChallengeActivity.this,
					android.R.layout.simple_spinner_item,
					FRIENDS);
			adapterF.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			friendsSpinner.setAdapter(adapterF);
			if (friendsSpinner.getSelectedItem().equals("")) {
				new AlertDialog.Builder(OnlineFriendChallengeActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(getString(R.string.sorry))
						.setMessage(getString(R.string.nofriends))
						.setPositiveButton(getString(R.string.invitetitle), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.chess.com")));
							}
						})
						.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
//								finish();
							}
						}).setCancelable(false)
						.create().show();
			}
		} else if (code == 1) {
			mainApp.ShowDialog(this, getString(R.string.congratulations), getString(R.string.onlinegamecreated));
		}
	}

	private void init() {

	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.createchallenge) {
			if (friendsSpinner.getCount() == 0) {
				return;
			}

			int color = iplayas.getSelectedItemPosition();
			int days = 1;
			days = daysArr[daysPerMoveSpinner.getSelectedItemPosition()];
			int israted = 0;
			int gametype = 0;

			if (isRated.isChecked()) {
				israted = 1;
			} else {
				israted = 0;
			}
			if (chess960.isChecked()) {
				gametype = 2;
			}
			String query = "http://www." + LccHolder.HOST + "/api/echess_new_game?id=" + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, "") +
					"&timepermove=" + days +
					"&iplayas=" + color +
					"&israted=" + israted +
					"&game_type=" + gametype +
					"&opponent=" + friendsSpinner.getSelectedItem().toString().trim();
			if (appService != null) {
				appService.RunSingleTask(1,
						query,
						progressDialog = new MyProgressDialog(ProgressDialog
								.show(OnlineFriendChallengeActivity.this, null, getString(R.string.creating), true))
				);
			}
		}
	}

}
