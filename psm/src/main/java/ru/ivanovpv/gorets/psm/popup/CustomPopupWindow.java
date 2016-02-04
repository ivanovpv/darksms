package ru.ivanovpv.gorets.psm.popup;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.PopupWindow;
import ru.ivanovpv.gorets.psm.R;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/popup/CustomPopupWindow.java $
 */

public class CustomPopupWindow {
	protected final View anchor;
	protected final PopupWindow window;
	private View root;
	private Drawable background = null;
	protected final WindowManager windowManager;
	
	public CustomPopupWindow(View anchor) {
		this.anchor = anchor;
		this.window = new PopupWindow(anchor.getContext());

		// Если происходит прикосновение за пределами диалогового окна,то окно закрывается
		window.setTouchInterceptor(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					CustomPopupWindow.this.window.dismiss();
					
					return true;
				}
				
				return false;
			}
		});

		windowManager = (WindowManager) anchor.getContext().getSystemService(Context.WINDOW_SERVICE);
		
		onCreate();
	}

	protected void onCreate() {}


	protected void onShow() {}

	protected void preShow() {
		if (root == null) {
			throw new IllegalStateException("error");
		}
		
		onShow();

		if (background == null) {
			window.setBackgroundDrawable(new BitmapDrawable());
		} else {
			window.setBackgroundDrawable(background);
		}
		
		window.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		window.setTouchable(true);
		window.setFocusable(true);
		window.setOutsideTouchable(true);

		window.setContentView(root);
	}

	public void setBackgroundDrawable(Drawable background) {
		this.background = background;
	}

	public void setContentView(View root) {
		this.root = root;
		
		window.setContentView(root);
	}

	public void setContentView(int layoutResID) {
		LayoutInflater inflator =
				(LayoutInflater) anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		setContentView(inflator.inflate(layoutResID, null));
	}


	public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
		window.setOnDismissListener(listener);
	}


	public void showDropDown() {
		showDropDown(0, 0);
	}

	public void showDropDown(int xOffset, int yOffset) {
		preShow();

		window.setAnimationStyle(R.style.Animations_PopDownMenu);

		window.showAsDropDown(anchor, xOffset, yOffset);
	}

	public void showLikeQuickAction() {
		showLikeQuickAction(0, 0);
	}

	public void showLikeQuickAction(int xOffset, int yOffset) {
		preShow();

		window.setAnimationStyle(R.style.Animations_PopUpMenu_Center);

		int[] location = new int[2];
		anchor.getLocationOnScreen(location);

		Rect anchorRect =
				new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1]
					+ anchor.getHeight());

		root.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		root.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		int rootWidth 		= root.getMeasuredWidth();
		int rootHeight 		= root.getMeasuredHeight();

		int screenWidth 	= windowManager.getDefaultDisplay().getWidth();

		int xPos 			= ((screenWidth - rootWidth) / 2) + xOffset;
		int yPos	 		= anchorRect.top - rootHeight + yOffset;


		if (rootHeight > anchorRect.top) {
			yPos = anchorRect.bottom + yOffset;
			
			window.setAnimationStyle(R.style.Animations_PopDownMenu_Center);
		}

		window.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos);
	}
	
	public void dismiss() {
		window.dismiss();
	}
}
