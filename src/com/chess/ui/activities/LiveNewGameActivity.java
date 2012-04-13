package com.chess.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.chess.R;
import com.chess.lcc.android.LccHolder;
import com.chess.live.client.Challenge;
import com.chess.model.GameListItem;
import com.chess.ui.core.AppConstants;
import com.chess.ui.core.IntentConstants;
import com.chess.ui.fragments.PopupDialogFragment;
import com.chess.utilities.MopubHelper;
import com.chess.utilities.Web;
import com.mopub.mobileads.MoPubView;

public class LiveNewGameActivity extends LiveBaseActivity implements OnClickListener {

	private int UPDATE_DELAY = 120000;
	private Button challengecreate;
	private Button currentGame;
	private Button upgradeBtn;
	private GameListItem gameListElement;
	private ChallengeDialogListener challengeDialogListener;
	private DirectChallengeDialogListener directChallengeDialogListener;
	private ReleasedByMeDialogListener releasedByMeDialogListener;
	private MoPubView moPubAdView;

	private void init() {
		challengeDialogListener = new ChallengeDialogListener();
		directChallengeDialogListener = new DirectChallengeDialogListener();
		releasedByMeDialogListener = new ReleasedByMeDialogListener();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.live_new_game);
		findViewById(R.id.mainView).setBackgroundDrawable(backgroundChessDrawable);

		upgradeBtn = (Button) findViewById(R.id.upgradeBtn);
		upgradeBtn.setOnClickListener(this);
		if (MopubHelper.isShowAds(mainApp)) {
			MopubHelper.showBannerAd(upgradeBtn, (MoPubView) findViewById(R.id.mopub_adview), mainApp);
		}

		/*if (MobclixHelper.isShowAds(mainApp)) {
			if (MobclixHelper.getBannerAdviewWrapper(mainApp) == null || MobclixHelper.getBannerAdview(mainApp) == null) {
				MobclixHelper.initializeBannerAdView(this, mainApp);
			}
		}*/
		init();

		findViewById(R.id.friendchallenge).setOnClickListener(this);
		challengecreate = (Button) findViewById(R.id.challengecreate);
		challengecreate.setOnClickListener(this);

		currentGame = (Button) findViewById(R.id.currentGame);
		currentGame.setOnClickListener(this);
	}

	@Override
	protected void onResume() {
		registerReceiver(challengesListUpdateReceiver, new IntentFilter(IntentConstants.CHALLENGES_LIST_UPDATE));
		super.onResume();
		if (lccHolder.getCurrentGameId() == null) {
			currentGame.setVisibility(View.GONE);
		} else if (mainApp.isLiveChess()) {
			currentGame.setVisibility(View.VISIBLE);
		}
//		enableScreenLockTimer();
	}

	@Override
	protected void onPause() {
		/*if (MobclixHelper.isShowAds(mainApp)) {
			MobclixHelper.pauseAdview(MobclixHelper.getBannerAdview(mainApp), mainApp);
		}*/
		unregisterReceiver(challengesListUpdateReceiver);
		super.onPause();
//		enableScreenLock();
	}

	@Override
	public void onLeftBtnClick(PopupDialogFragment fragment) {
		lccHolder.logout();
		backToHomeActivity();
	}

	@Override
	public void update(int code) {
		if (code == INIT_ACTIVITY) {
			if (appService != null) {
				if (!mainApp.isLiveChess()) {
					appService.RunRepeatableTask(OnlineScreenActivity.ONLINE_CALLBACK_CODE, 0, UPDATE_DELAY,
							"http://www." + LccHolder.HOST + AppConstants.API_ECHESS_OPEN_INVITES_ID +
									mainApp.getSharedData().getString(AppConstants.USER_TOKEN, ""),
							null/*progressDialog = MyProgressDialog
                                        .show(OnlineNewGame.this, null, getString(R.string.loadinggames), true)*/);
				} else {
					/*appService.RunRepeatble(Online.ONLINE_CALLBACK_CODE, 0, 2000,
													  progressDialog = MyProgressDialog
														.show(OnlineNewGame.this, null, getString(R.string.updatinggameslist), true));*/
					update(OnlineScreenActivity.ONLINE_CALLBACK_CODE);
				}
			}
		} else if (code == OnlineScreenActivity.ONLINE_CALLBACK_CODE) {
			// TODO show popup dialog

//			openChallengesLictView.setVisibility(View.GONE);
//			gameListItems.clear();
//			if (mainApp.isLiveChess()) {
//				gameListItems.addAll(lccHolder.getChallengesAndSeeksData());
//			} else {
//				gameListItems.addAll(ChessComApiParser.ViewOpenChallengeParse(responseRepeatable));
//			}
//			if (gamesAdapter == null) {
//				gamesAdapter = new OnlineGamesAdapter(this, R.layout.gamelistelement, gameListItems);
//				openChallengesLictView.setAdapter(gamesAdapter);
//			}
//			gamesAdapter.notifyDataSetChanged();
//			openChallengesLictView.setVisibility(View.VISIBLE);

		} else if (code == 2) {
			mainApp.showToast(getString(R.string.challengeaccepted));
			onPause();
			onResume();
		} else if (code == 3) {
			mainApp.showToast(getString(R.string.challengedeclined));
			onPause();
			onResume();
		} else if (code == 4) {
			onPause();
			onResume();
		}
	}

	/*@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		System.out.println("LCCLOG: onWindowFocusChanged hasFocus " + hasFocus);
		if (hasFocus && MobclixHelper.isShowAds(mainApp) && mainApp.isForceBannerAdOnFailedLoad()) {
			MobclixHelper.showBannerAd(upgradeBtn, this, mainApp);
		}
	}*/

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.upgradeBtn) {
			startActivity(mainApp.getMembershipAndroidIntent());

		} else if (view.getId() == R.id.friendchallenge) {
			startActivity(new Intent(this, LiveFriendChallengeActivity.class));
		} else if (view.getId() == R.id.challengecreate) {
			startActivity(new Intent(this, LiveCreateChallengeActivity.class));
		} else if (view.getId() == R.id.currentGame) {
			if (lccHolder.getCurrentGameId() != null && lccHolder.getGame(lccHolder.getCurrentGameId()) != null) {
				lccHolder.processFullGame(lccHolder.getGame(lccHolder.getCurrentGameId()));
			}
		}
	}

	private class ChallengeDialogListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface d, int pos) {
			if (pos == 0) {
				final Challenge challenge = lccHolder.getChallenge(gameListElement.getGameId());
				LccHolder.LOG.info("Accept challenge: " + challenge);
				lccHolder.getAndroid().runAcceptChallengeTask(challenge);
				lccHolder.removeChallenge(gameListElement.getGameId());
				update(GameBaseActivity.CALLBACK_COMP_MOVE);
			} else if (pos == 1) {
				final Challenge challenge = lccHolder.getChallenge(gameListElement.getGameId());
				LccHolder.LOG.info("Decline challenge: " + challenge);
				lccHolder.getAndroid().runRejectChallengeTask(challenge);
				lccHolder.removeChallenge(gameListElement.getGameId());
				update(3);
			}
		}
	}

	private class DirectChallengeDialogListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface d, int pos) {
			if (pos == 0) {
				final Challenge challenge = lccHolder.getChallenge(gameListElement.getGameId());
				LccHolder.LOG.info(AppConstants.CANCEL_MY_CHALLENGE + challenge);
				lccHolder.getAndroid().runCancelChallengeTask(challenge);
				lccHolder.removeChallenge(gameListElement.getGameId());
				update(4);
			} else if (pos == 1) {
				final Challenge challenge = lccHolder.getChallenge(gameListElement.getGameId());
				LccHolder.LOG.info(AppConstants.JUST_KEEP_MY_CHALLENGE + challenge);
			}
		}
	}

	private class ReleasedByMeDialogListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface d, int pos) {
			if (pos == 0) {
				final Challenge challenge = lccHolder.getSeek(gameListElement.getGameId());
				LccHolder.LOG.info("Cancel my seek: " + challenge);
				lccHolder.getAndroid().runCancelChallengeTask(challenge);
				lccHolder.removeSeek(gameListElement.getGameId());
				update(4);
			} else if (pos == 1) {
				final Challenge challenge = lccHolder.getSeek(gameListElement.getGameId());
				LccHolder.LOG.info("Just keep my seek: " + challenge);
			}
		}
	}

	private class EchessDialogListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface d, int pos) {
			if (pos == 0) {
				String result = Web.Request("http://www." + LccHolder.HOST + AppConstants.API_ECHESS_OPEN_INVITES_ID + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, "") + AppConstants.ACCEPT_INVITEID_PARAMETER + gameListElement.getGameId(), "GET", null, null);
				if (result.contains(AppConstants.SUCCESS)) {
					update(GameBaseActivity.CALLBACK_COMP_MOVE);
				} else if (result.contains(AppConstants.ERROR_PLUS)) {
					mainApp.showDialog(LiveNewGameActivity.this, AppConstants.ERROR, result.split("[+]")[1]);
				} else {
					//mainApp.showDialog(OnlineNewGame.this, "Error", result);
				}
			} else if (pos == 1) {

				String result = Web.Request("http://www." + LccHolder.HOST + AppConstants.API_ECHESS_OPEN_INVITES_ID + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, "") + AppConstants.DECLINE_INVITEID_PARAMETER + gameListElement.getGameId(), "GET", null, null);
				if (result.contains(AppConstants.SUCCESS)) {
					update(3);
				} else if (result.contains(AppConstants.ERROR_PLUS)) {
					mainApp.showDialog(LiveNewGameActivity.this, AppConstants.ERROR, result.split("[+]")[1]);
				} else {
					//mainApp.showDialog(OnlineNewGame.this, "Error", result);
				}
			}
		}
	}

//	@Override
//	public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
//		gameListElement = gameListItems.get(pos);
//		if (gameListElement.type == GameListItem.LIST_TYPE_CHALLENGES) {
//			final String title = gameListElement.values.get(GameListItem.OPPONENT_CHESS_TITLE);
//
//			if (gameListElement.values.get(GameListItem.IS_DIRECT_CHALLENGE).equals("1") && gameListElement.values.get(GameListItem.IS_RELEASED_BY_ME).equals("0")) {
//				new AlertDialog.Builder(LiveNewGameActivity.this)
//						.setTitle(title)
//						.setItems(new String[]{getString(R.string.accept),
//								getString(R.string.decline)}, challengeDialogListener)
//						.create().show();
//			} else if (gameListElement.values.get(GameListItem.IS_DIRECT_CHALLENGE).equals("1") && gameListElement.values.get(GameListItem.IS_RELEASED_BY_ME).equals("1")) {
//				new AlertDialog.Builder(LiveNewGameActivity.this)
//						.setTitle(title)
//						.setItems(new String[]{"Cancel", "Keep"}, directChallengeDialogListener)
//						.create().show();
//			} else if (gameListElement.values.get(GameListItem.IS_DIRECT_CHALLENGE).equals("0")
//					&& gameListElement.values.get(GameListItem.IS_RELEASED_BY_ME).equals("0")) {
//				final Challenge challenge = lccHolder.getSeek(gameListElement.getGameId());
//				LccHolder.LOG.info("Accept seek: " + challenge);
//				lccHolder.getAndroid().runAcceptChallengeTask(challenge);
//				lccHolder.removeSeek(gameListElement.getGameId());
//				update(GameBaseActivity.CALLBACK_COMP_MOVE);
//			} else if (gameListElement.values.get(GameListItem.IS_DIRECT_CHALLENGE).equals("0")
//					&& gameListElement.values.get(GameListItem.IS_RELEASED_BY_ME).equals("1")) {
//				new AlertDialog.Builder(LiveNewGameActivity.this)
//						.setTitle(title)
//						.setItems(new String[]{"Cancel", "Keep"}, releasedByMeDialogListener)
//						.create().show();
//			}
//
//		}
//	}
}
