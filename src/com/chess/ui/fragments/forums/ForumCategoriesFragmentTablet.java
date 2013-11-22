package com.chess.ui.fragments.forums;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.chess.R;
import com.chess.db.DbDataManager;
import com.chess.db.DbHelper;
import com.chess.db.DbScheme;
import com.chess.ui.adapters.CommonCategoriesCursorAdapter;
import com.chess.ui.fragments.BasePopupsFragment;
import com.chess.ui.fragments.articles.ArticleCategoriesFragmentTablet;
import com.chess.ui.fragments.settings.SettingsProfileFragment;
import com.chess.ui.fragments.settings.SettingsThemeCustomizeFragment;
import com.chess.ui.interfaces.FragmentParentFace;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 12.11.13
 * Time: 15:27
 */
public class ForumCategoriesFragmentTablet extends ForumCategoriesFragment implements FragmentParentFace{

	private boolean noCategoriesFragmentsAdded;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.new_common_tablet_content_frame, container, false);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor cursor = (Cursor) parent.getItemAtPosition(position);
		int categoryId = DbDataManager.getInt(cursor, DbScheme.V_ID);

//		if (noCategoriesFragmentsAdded) {
			changeInternalFragment(ForumTopicsFragmentTablet.createInstance(categoryId, this));
//			noCategoriesFragmentsAdded = false;
//		} else {
//			changeInternalFragment(ForumTopicsFragmentTablet.createInstance(categoryId, this));
//		}
	}

	@Override
	protected boolean loadFromDb() {
		Cursor cursor = DbDataManager.query(getContentResolver(), DbHelper.getForumCategories());
		if (cursor != null && cursor.moveToFirst()) {
			categoriesCursorAdapter.changeCursor(cursor);

			need2update = false;

			// open first category by default

			int categoryId = DbDataManager.getInt(cursor, DbScheme.V_ID);
			changeInternalFragment(ForumTopicsFragmentTablet.createInstance(categoryId, this));
			noCategoriesFragmentsAdded = true;

			return true;
		}

		return false;
	}

	@Override
	protected void init() {
		super.init();
		categoriesCursorAdapter.setLayoutId(R.layout.new_common_titled_list_item_thin_white);

	}

	@Override
	public void changeFragment(BasePopupsFragment fragment) {
		openInternalFragment(fragment);
	}

	private void changeInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.innerFragmentContainer, fragment).commitAllowingStateLoss();
	}

	private void openInternalFragment(Fragment fragment) {
		FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
		transaction.replace(R.id.innerFragmentContainer, fragment, fragment.getClass().getSimpleName());
		transaction.addToBackStack(fragment.getClass().getSimpleName());
		transaction.commit();
	}

	@Override
	public boolean showPreviousFragment() {
		if (getActivity() == null) {
			return false;
		}
		int entryCount = getChildFragmentManager().getBackStackEntryCount();
		if (entryCount > 0) {
			int last = entryCount - 1;
			FragmentManager.BackStackEntry stackEntry = getChildFragmentManager().getBackStackEntryAt(last);
			if (stackEntry != null && stackEntry.getName().equals(SettingsThemeCustomizeFragment.class.getSimpleName())) {
				noCategoriesFragmentsAdded = true;
			}

			return getChildFragmentManager().popBackStackImmediate();
		} else {
			return super.showPreviousFragment();
		}
	}
}