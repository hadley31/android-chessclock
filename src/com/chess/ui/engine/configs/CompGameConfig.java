package com.chess.ui.engine.configs;

import android.os.Parcel;
import android.os.Parcelable;
import com.chess.backend.statics.AppConstants;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 14.04.13
 * Time: 20:05
 */
public class CompGameConfig implements Parcelable {
	private int strength;
	private int mode;

	public static class Builder{
		private int strength;
		private int mode;

		/**
		 * Create new Seek game with default values
		 */
		public Builder(){
			mode = AppConstants.GAME_MODE_COMPUTER_VS_HUMAN_WHITE;
			strength = 5;
		}

		public Builder setStrength(int strength) {
			this.strength = strength;
			return this;
		}

		public Builder setMode(int mode) {
			this.mode = mode;
			return this;
		}

		public CompGameConfig build(){
			return new CompGameConfig(this);
		}
	}

	private CompGameConfig(Builder builder) {
		this.strength = builder.strength;
		this.mode = builder.mode;
	}

	public int getStrength() {
		return strength;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	protected CompGameConfig(Parcel in) {
		strength = in.readInt();
		mode = in.readInt();
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(strength);
		dest.writeInt(mode);
	}

	public static final Parcelable.Creator<CompGameConfig> CREATOR = new Parcelable.Creator<CompGameConfig>() {
		public CompGameConfig createFromParcel(Parcel in) {
			return new CompGameConfig(in);
		}

		public CompGameConfig[] newArray(int size) {
			return new CompGameConfig[size];
		}
	};
}
