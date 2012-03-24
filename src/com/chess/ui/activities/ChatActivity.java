package com.chess.ui.activities;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import com.chess.R;
import com.chess.lcc.android.LccHolder;
import com.chess.model.GameListElement;
import com.chess.ui.adapters.MessagesAdapter;
import com.chess.ui.core.AppConstants;
import com.chess.ui.core.CoreActivityActionBar;
import com.chess.ui.views.BackgroundChessDrawable;
import com.chess.utilities.ChessComApiParser;
import com.chess.utilities.MyProgressDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ChatActivity extends CoreActivityActionBar implements OnClickListener {
	private EditText sendText;
	private ListView chatListView;
	private MessagesAdapter messages = null;
	private final ArrayList<com.chess.model.Message> chatItems = new ArrayList<com.chess.model.Message>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_screen);
		findViewById(R.id.mainView).setBackgroundDrawable(new BackgroundChessDrawable(this));

		sendText = (EditText) findViewById(R.id.sendText);
		chatListView = (ListView) findViewById(R.id.chatLV);
		findViewById(R.id.send).setOnClickListener(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		findViewById(R.id.mainView).setBackgroundDrawable(new BackgroundChessDrawable(this));
	}

	@Override
	public void update(int code) {
		if (code == INIT_ACTIVITY) {
			if (appService != null) {
				appService.RunRepeatableTask(0, 0, 60000, "http://www." + LccHolder.HOST
						+ AppConstants.API_SUBMIT_ECHESS_ACTION_ID + mainApp.getSharedData().getString(AppConstants.USER_TOKEN, "") + AppConstants.CHESSID_PARAMETER
						+ extras.getString(GameListElement.GAME_ID) + "&command=CHAT&timestamp=" + extras.getString(GameListElement.TIMESTAMP),
						null/*
							 * progressDialog = MyProgressDialog.show(Chat.this, null,
							 * getString(R.string.gettingmessages), true)
							 */
				);
			}
		} else if (code == 0) {
			int before = chatItems.size();
			chatItems.clear();
			chatItems.addAll(ChessComApiParser.ReciveMessages(responseRepeatable));
			if (before != chatItems.size()) {
				if (messages == null) {
					messages = new MessagesAdapter(ChatActivity.this, R.layout.chat_item, chatItems);
					chatListView.setAdapter(messages);
				} else {
					messages.notifyDataSetChanged();
				}
				chatListView.setSelection(chatItems.size() - 1);
			}
		} else if (code == 1) {
			chatItems.clear();
			chatItems.addAll(ChessComApiParser.ReciveMessages(response));

			if (messages == null) {
				messages = new MessagesAdapter(ChatActivity.this, R.layout.chat_item, chatItems);
				chatListView.setAdapter(messages);
			} else {
				messages.notifyDataSetChanged();
			}
			sendText.setText("");
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(sendText.getWindowToken(), 0);
			chatListView.setSelection(chatItems.size() - 1);
		}
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.send) {
			String message = "";
			try {
				message = URLEncoder.encode(sendText.getText().toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				Log.e("Chat", e.toString());	 // TODO handle exception
				// correctly
			}

			if (appService != null) {
				String query = "http://www." + LccHolder.HOST + AppConstants.API_SUBMIT_ECHESS_ACTION_ID
						+ mainApp.getSharedData().getString(AppConstants.USER_TOKEN, "") + AppConstants.CHESSID_PARAMETER
						+ extras.getString(GameListElement.GAME_ID) + "&command=CHAT&message=" + message + AppConstants.TIMESTAMP_PARAMETER
						+ extras.getString(GameListElement.TIMESTAMP);
				appService.RunSingleTask(1, query,
						progressDialog = new MyProgressDialog(ProgressDialog.show(ChatActivity.this, null,
								getString(R.string.sendingmessage), true)));
			}
		}
	}
}
