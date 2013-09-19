package com.chess.db.tasks;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.chess.backend.interfaces.TaskUpdateInterface;
import com.chess.statics.StaticData;
import com.chess.statics.Symbol;
import com.chess.backend.tasks.AbstractUpdateTask;
import com.chess.db.DbDataProvider;
import com.chess.db.DbScheme;
import com.chess.db.QueryParams;


public class LoadDataFromDbTask extends AbstractUpdateTask<Cursor, Long> {

	private ContentResolver contentResolver;
	private QueryParams params;

	public LoadDataFromDbTask(TaskUpdateInterface<Cursor> taskFace, QueryParams params, ContentResolver resolver) {
		super(taskFace);
		this.params = params;
		this.contentResolver = resolver;
	}

	@Override
	protected Integer doTheTask(Long... ids) {
		int result = StaticData.EMPTY_DATA;

		if (ids != null && ids.length > 0) {
			item = contentResolver.query(ContentUris.withAppendedId(params.getUri(), ids[0]), params.getProjection(),
					params.getSelection(), params.getArguments(), params.getOrder());
		} else {
			if (params.isUseRawQuery()) {
				ContentProviderClient client = contentResolver.acquireContentProviderClient(DbScheme.PROVIDER_NAME);
				SQLiteDatabase dbHandle = ((DbDataProvider) client.getLocalContentProvider()).getDbHandle();
				StringBuilder projection = new StringBuilder();
				for (String projections : params.getProjection()) {
					projection.append(projections).append(Symbol.COMMA);
				}
				// TODO hide to down level
				item = dbHandle.rawQuery("SELECT " + projection.toString().substring(0, projection.length() - 1)
						+ " FROM " + params.getDbName() + " " + params.getCommands(), null);
				client.release();

			} else {
				item = contentResolver.query(params.getUri(), params.getProjection(), params.getSelection(),
						params.getArguments(), params.getOrder());
			}
		}

		if (item != null && item.moveToFirst()) {
			result = StaticData.RESULT_OK;
		}

		return result;
	}

}
