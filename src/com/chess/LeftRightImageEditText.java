package com.chess;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 03.01.13
 * Time: 13:45
 */
public class LeftRightImageEditText extends RoboEditText {

	public static final int ONE = 0;
	public static final int TOP = 1;
	public static final int MID = 2;
	public static final int BOT = 3;

	public static final float BORDER_OFFSET = 2.0f;

	private Drawable rightIcon;
	private int rightImageWidth;
	private int rightImageHeight;

	private Drawable icon;
	private int imageWidth;
	private boolean initialized;
	private ShapeDrawable backForImage;
	private int roundMode;
	private Paint linePaint;
	private int lineYStop;
	private int lineYStart;
	private int lineXStop;
	private int lineXStart;
	private float backImageWidth;
	private int rightImageOffset;
	private boolean enlargeHeight;
	private boolean rightPaddingSet;
	private int bottomPadding;
	private int currentViewHeight;


	public LeftRightImageEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	public LeftRightImageEditText(Context context) {
		super(context);
	}

	public LeftRightImageEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		Resources resources = context.getResources();
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.EnhancedField);
		int color;
		try {
			rightIcon = array.getDrawable(R.styleable.EnhancedField_rightImage);
			roundMode = array.getInteger(R.styleable.EnhancedField_round_mode, ONE);

			color = array.getInteger(R.styleable.EnhancedField_color, Color.WHITE);
			icon = array.getDrawable(R.styleable.EnhancedField_leftImage);
		} finally {
			array.recycle();
		}

		if (rightIcon != null) {
			setRightIcon(rightIcon);
		}

		float density = resources.getDisplayMetrics().density;

		imageWidth = icon.getIntrinsicWidth();
		int imageHeight = icon.getIntrinsicHeight();
		icon.setBounds(0, 0, imageWidth, imageHeight);

		float radius = resources.getDimension(R.dimen.new_round_button_radius);
		float[] outerR;
		switch (roundMode) {
			case ONE:
				outerR = new float[]{radius, radius, radius, radius, 0, 0, 0, 0};
				break;
			case TOP:
				outerR = new float[]{radius, radius, 0, 0, 0, 0, 0, 0};
				break;
			case MID:
				outerR = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
				break;
			case BOT:
				outerR = new float[]{0, 0, 0, 0, 0, 0, radius, radius};
				break;
			default:
				outerR = new float[]{radius, radius, radius, radius, 0, 0, 0, 0};
				break;
		}
		backForImage = new ShapeDrawable(new RoundRectShape(outerR, null, null));
		backForImage.getPaint().setColor(color);

		linePaint = new Paint();
		linePaint.setColor(resources.getColor(R.color.light_grey_border));
		linePaint.setStrokeWidth(1);
		linePaint.setStyle(Paint.Style.STROKE);

		backImageWidth = resources.getDimension(R.dimen.new_edit_field_height);

		if (rightImageHeight > backImageWidth) {
			enlargeHeight = true;
		}

		rightImageOffset = (int) (26 * density);

		bottomPadding = (int) (10 * density);
	}

	@Override
	protected void onDraw(Canvas canvas) { // TODO use Picture?
		if (!initialized) {
			initImage();
		}
		int height = getHeight();
		int width = getWidth();

		backForImage.draw(canvas);

		// place image
		canvas.save();
		float imgCenterX = (backImageWidth - imageWidth)/2;
		float imgCenterY = (height - imageWidth)/2;
		canvas.translate(imgCenterX, imgCenterY);
		icon.draw(canvas);
		canvas.restore();

		if (rightIcon != null) {
			// place second image
			canvas.save();
			imgCenterX = width - rightImageOffset/2 - rightImageWidth;
			imgCenterY = (height - rightImageWidth)/2;
			canvas.translate(imgCenterX, imgCenterY);
			rightIcon.draw(canvas);
			canvas.restore();
		}

		if (roundMode != ONE) { // don't draw bottom/top lines for standalone field
			canvas.drawLine(lineXStart, lineYStart, lineXStop, lineYStop, linePaint);
		}
		if (roundMode == MID) { // draw second line for middle position
			canvas.drawLine(lineXStart, 0, lineXStop, 0, linePaint);
		}

		// set padding to make text selection work correct
		if (enlargeHeight) {
			setPadding((int) (backImageWidth + bottomPadding), 0, 0, (int) (currentViewHeight/2 - getTextSize()/2));
		} else {
			setPadding((int) (backImageWidth + bottomPadding), 0, 0, bottomPadding);
		}

		if (!rightPaddingSet) {
			int paddingBottom = getPaddingBottom();
			int paddingTop = getPaddingTop();
			int paddingRight = getPaddingRight();
			int paddingLeft = getPaddingLeft();
			setPadding(paddingLeft, paddingTop, paddingRight + rightImageWidth, paddingBottom);
			rightPaddingSet = true;
		}

		super.onDraw(canvas);
	}

	private void initImage() {
		int width = getWidth();
		int height = getHeight();

		lineXStart = (int) BORDER_OFFSET;
		lineXStop = (int) (width - BORDER_OFFSET);
		lineYStart = height - 1;
		lineYStop = height - 1;

		int x0 = (int)BORDER_OFFSET;
		int y0 = 0;
		int x1 = (int) backImageWidth;
		int y1 = 0;

		switch (roundMode) {
			case ONE:
				y0 = (int) BORDER_OFFSET;
				y1 = (int) (height - BORDER_OFFSET + 1);

				break;
			case TOP:
				y0 = (int)BORDER_OFFSET;
				y1 = (int) (height - BORDER_OFFSET + 1);

				break;
			case MID:
				y0 = (int)BORDER_OFFSET - 1;
				y1 = (int) (height - BORDER_OFFSET) +1;
				break;
			case BOT:
				lineYStart = 0;
				lineYStop = 0;

				y0 = (int)BORDER_OFFSET - 1;
				y1 =  (int) (height - BORDER_OFFSET);
				break;
		}
		backForImage.setBounds(x0, y0, x1, y1);

		initialized = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (enlargeHeight) {
			int parentWidth = MeasureSpec.getSize(widthMeasureSpec);

			currentViewHeight = rightImageHeight + rightImageOffset;
			setMeasuredDimension(parentWidth, currentViewHeight);
		}else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	public void setRightIcon(int iconId) {
		setRightIcon(getResources().getDrawable(iconId));
	}

	public void setRightIcon(Drawable icon) {
		rightIcon = icon;
		rightImageWidth = rightIcon.getIntrinsicWidth();
		rightImageHeight = rightIcon.getIntrinsicHeight();
		rightIcon.setBounds(0, 0, rightImageWidth, rightImageHeight);
		invalidate();
	}

	public int getRightImageWidth() {
		return rightImageWidth;
	}

	public int getRightImageHeight() {
		return rightImageHeight;
	}
}
