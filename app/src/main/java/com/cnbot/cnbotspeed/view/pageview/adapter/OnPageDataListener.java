package com.cnbot.cnbotspeed.view.pageview.adapter;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public interface OnPageDataListener<T> {
	int ONE = 1;
	int TWO = 2;
	int THREE = 3;
	int FOUR = 4;
	int HORIZONTAL = 0;
	int VERTICAL = 1;
	int LINEAR = 0;
	int GRID = 1;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ONE, TWO})
	@interface PageRow {
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({ONE, TWO, THREE, FOUR})
	@interface PageColumn {
	}

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({HORIZONTAL, VERTICAL})
	@interface LayoutOrientation {
	}
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({LINEAR, GRID})
	@interface LayoutFlag {
	}

	@PageRow
	int getPageColumn();

	@PageColumn
	int getPageRow();

	@LayoutOrientation
	int getLayoutOrientation();

	@LayoutFlag
	int getLayoutFlag();

	boolean isLooping();

	int getPageCount();

	int getRawItemCount();

	List<T> getRawData();
}
