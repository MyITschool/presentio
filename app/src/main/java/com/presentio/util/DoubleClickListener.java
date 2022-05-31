package com.presentio.util;

import android.os.SystemClock;
import android.view.View;

/**
 * Link - https://gist.github.com/srix55/ec64d2f6a371c80bbbc4
 * Author - Srikanth Venkatesh
 */
public abstract class DoubleClickListener implements View.OnClickListener {
	private static final long DEFAULT_QUALIFICATION_SPAN = 200;
	private long doubleClickQualificationSpanInMillis;
	private long timestampLastClick;
	
	protected DoubleClickListener() {
		this(DEFAULT_QUALIFICATION_SPAN);
	}
	
	protected DoubleClickListener(long doubleClickQualificationSpanInMillis) {
		this.doubleClickQualificationSpanInMillis = doubleClickQualificationSpanInMillis;
		timestampLastClick = 0;
	}

	@Override
	public void onClick(View v) {
		if((SystemClock.elapsedRealtime() - timestampLastClick) < doubleClickQualificationSpanInMillis) {
			onDoubleClick();
		}
		timestampLastClick = SystemClock.elapsedRealtime();
	}
	
	public abstract void onDoubleClick();
}