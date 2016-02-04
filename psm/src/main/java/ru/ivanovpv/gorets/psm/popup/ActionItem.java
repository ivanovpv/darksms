package ru.ivanovpv.gorets.psm.popup;

import android.graphics.drawable.Drawable;
import android.view.View.OnClickListener;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/popup/ActionItem.java $
 */

public class ActionItem {
	private Drawable icon;
	private String title;
	private OnClickListener listener;
	
	public ActionItem() {}
	
	public ActionItem(Drawable icon) {
		this.icon = icon;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public Drawable getIcon() {
		return this.icon;
	}
	

	public void setOnClickListener(OnClickListener listener) {
		this.listener = listener;
	}
	
	public OnClickListener getListener() {
		return this.listener;
	}
}
