package com.chess.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import com.chess.R;
import com.chess.backend.RestHelper;
import com.chess.backend.entity.LoadItem;
import com.chess.backend.interfaces.ActionBarUpdateListener;
import com.chess.backend.statics.AppData;
import com.chess.model.VideoItem;
import com.chess.ui.adapters.VideosAdapter;
import com.chess.ui.adapters.VideosPaginationAdapter;
import com.chess.ui.interfaces.ItemClickListenerFace;
import com.chess.utilities.AppUtils;

import java.util.ArrayList;
import java.util.List;

public class VideoListActivity extends LiveBaseActivity implements OnItemClickListener,
		View.OnClickListener, ItemClickListenerFace {
	private ListView listView;
	private VideosListItemsUpdateListener videosListItemUpdateListener;
    private String skill;
    private String category;
    private String keyword;
	private List<VideoItem> videosList;


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_list_screen);

	    Button videoUpgrade = (Button) findViewById(R.id.upgradeBtn);
        videoUpgrade.setOnClickListener(this);
        videoUpgrade.setVisibility(AppUtils.isNeedToUpgrade(this)? View.VISIBLE: View.GONE);

		listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(this);

        keyword = extras.getString(RestHelper.P_KEYWORD);
		category = extras.getString(RestHelper.P_CATEGORY);
		skill = extras.getString(RestHelper.P_SKILL_LEVEL);

		videosListItemUpdateListener = new VideosListItemsUpdateListener();
	}

	@Override
	protected void onResume() {
		super.onResume();

		updateList();
		hideKeyBoard();
	}

	private void updateList() {
		LoadItem loadItem = new LoadItem();
		loadItem.setLoadPath(RestHelper.GET_VIDEOS);
		loadItem.addRequestParams(RestHelper.P_ID, AppData.getUserToken(getContext()));
		loadItem.addRequestParams(RestHelper.P_PAGE_SIZE, RestHelper.V_VIDEO_LIST_CNT);
		loadItem.addRequestParams(RestHelper.P_PAGE, String.valueOf(0));
        if(keyword != null){ // weird hack because LoadItem -> NameValuePairs can't be Serialized
            loadItem.addRequestParams(RestHelper.P_KEYWORD, keyword);
        }else{
            loadItem.addRequestParams(RestHelper.P_SKILL_LEVEL, skill);
            loadItem.addRequestParams(RestHelper.P_CATEGORY, category);
        }


		//set Pagination adapter params
		ArrayList<VideoItem> itemsList = new ArrayList<VideoItem>();
		VideosAdapter videosAdapter = new VideosAdapter(this, itemsList);

		VideosPaginationAdapter paginationAdapter = new VideosPaginationAdapter(this, videosAdapter, videosListItemUpdateListener, loadItem);

		listView.setAdapter(paginationAdapter);
	}

	@Override
	public Context getMeContext() {
		return this;
	}

	private class VideosListItemsUpdateListener extends ActionBarUpdateListener<VideoItem> {
		public VideosListItemsUpdateListener() {
			super(getInstance());
			useList = true;
		}

		@Override
		public void updateListData(List<VideoItem> itemsList) {
			videosList = itemsList;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int pos, long id) {
		VideoItem videoItem = (VideoItem) adapter.getItemAtPosition(pos);

		Intent intent = new Intent(Intent.ACTION_VIEW);
		String videoUrl = videoItem.getViewUrl().trim();
		intent.setDataAndType(Uri.parse(videoUrl), "video/*");
		startActivity(intent);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.upgradeBtn) {
			startActivity(AppData.getMembershipVideoIntent(this));
		} else if(view.getId() == R.id.fullDescBtn){
			int pos = (Integer) view.getTag(R.id.list_item_id);
			VideoItem videoItem = videosList.get(pos);

			showSinglePopupDialog(videoItem.getTitle(), videoItem.getDescription());

//			new AlertDialog.Builder(context)
//					.setTitle()
//					.setMessage(videoItem.getDescription())
//					.setPositiveButton(context.getString(R.string.ok), null)
//					.create().show();
		} else if(view.getId() == R.id.playVideoBtn) {
			int pos = (Integer) view.getTag(R.id.list_item_id);
			VideoItem videoItem = videosList.get(pos);

			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse(videoItem.getViewUrl().trim()), "video/*");
			startActivity(intent);
		}


	}


}
