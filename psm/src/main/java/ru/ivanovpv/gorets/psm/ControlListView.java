package ru.ivanovpv.gorets.psm;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.BitSet;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/ControlListView.java $
 */

/**
 * Created by IntelliJ IDEA.
 * User: pivanov
 * Date: 12.11.2010
 * Time: 12:13:27
 * To change this template use File | Settings | File Templates.
 */
public abstract class ControlListView extends ListView
{
    protected final String TAG=this.getClass().getName();
    protected SimpleAdapter adapter=new SimpleAdapter();
    protected ArrayList<Integer> indices=new ArrayList<Integer>();
    protected ArrayList<View> views=new ArrayList<View>();
    protected BitSet enabled=new BitSet();
    protected View emptyView;

    protected ControlListView(Context context)
    {
        super(context);
        // this.setAdapter(adapter);
    }

    protected ControlListView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        //this.setAdapter(adapter);
    }

    protected ControlListView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        //this.setAdapter(adapter);
    }

    public abstract View getView(int position, View convertView, ViewGroup parent);

    public int getSize()
    {
        if(indices.size() == 0 && emptyView != null)
            return 1;
        else
            return indices.size();
    }

    @Override
    public void setEmptyView(View view)
    {
        emptyView=view;
    }

    public BaseAdapter getAdapter()
    {
        return adapter;
    }

    public void setDefaultAdapter()
    {
        this.setAdapter(adapter);
    }

    public void addElement(int index, boolean enabled)
    {
        indices.add(index);
        if(enabled)
            this.enabled.set(this.getSize() - 1);
        adapter.notifyDataSetChanged();
    }

    public void addView(int index, View view, boolean enabled)
    {
        indices.add(index, index);
        views.add(index, view);
        if(enabled)
            this.enabled.set(index);
        adapter.notifyDataSetChanged();
    }

    public void addView(View view, boolean enabled)
    {
        views.add(view);
        indices.add(views.size() - 1);
        if(enabled)
            this.enabled.set(this.getSize() - 1);
        adapter.notifyDataSetChanged();
    }

    public void clear()
    {
        indices.clear();
        enabled.clear();
        adapter.notifyDataSetChanged();
    }

    public void cleanUp()
    {
        indices.clear();
        enabled.clear();
    }

    class SimpleAdapter extends BaseAdapter
    {
        public int getCount()
        {
            return ControlListView.this.getSize();
        }

        public Object getItem(int position)
        {
            return indices.get(position);
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            return ControlListView.this.getView(position, convertView, parent);
        }

        @Override
        public boolean areAllItemsEnabled()
        {
            return false;
        }

        @Override
        public boolean isEnabled(int position)
        {
            return enabled.get(position);
        }

        @Override
        public int getItemViewType(int position)
        {
            // Don't let ListView try to reuse the views.
            return AdapterView.ITEM_VIEW_TYPE_IGNORE;
        }
    }
}
