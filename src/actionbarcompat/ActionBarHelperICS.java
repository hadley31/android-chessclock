/*
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package actionbarcompat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

/**
 * An extension of {@link actionbarcompat.ActionBarHelper} that provides Android
 * 4.0-specific functionality for IceCreamSandwich devices. It thus requires API level 14.
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActionBarHelperICS extends ActionBarHelperHoneycomb {
	protected ActionBarHelperICS(ActionBarActivity activity) {
		super(activity);
	}

	@Override
	protected Context getActionBarThemedContext() {
		if (mActivity != null && mActivity.getActionBar() != null) {
			return mActivity.getActionBar().getThemedContext();
		} else {
			return null;
		}
	}
}
