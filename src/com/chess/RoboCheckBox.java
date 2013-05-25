package com.chess;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.CheckBox;

public class RoboCheckBox extends CheckBox {

	private String ttfName = FontsHelper.DEFAULT_FONT;

	public RoboCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
        setupFont(context, attrs);
	}

	public RoboCheckBox(Context context) {
		super(context);
	}

	public RoboCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
        setupFont(context, attrs);
	}

	private void setupFont(Context context, AttributeSet attrs) {
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RoboTextView);
		if (array == null) {
			return;
		}
		try {
			if (array.hasValue(R.styleable.RoboTextView_ttf)) {
				ttfName = array.getString(R.styleable.RoboTextView_ttf);
			}
		} finally {
			array.recycle();
		}

		init(context);
	}

	private void init(Context context) {
		if (!isInEditMode()) {
			setTypeface(FontsHelper.getInstance().getTypeFace(context, ttfName));
		}
	}

	public void setFont(String font) {
		ttfName = font;
		init(getContext());
	}

}
