package com.chess.model;

import android.graphics.drawable.Drawable;
import com.chess.statics.Symbol;

public class SelectionItem {
	private Drawable image;
	private String text = Symbol.EMPTY;
	private String code = Symbol.EMPTY;
	private boolean checked;

	public SelectionItem(Drawable image, String text) {
		this.image = image;
		this.text = text;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public Drawable getImage() {
		return image;
	}

	public String getText() {
		return text;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
