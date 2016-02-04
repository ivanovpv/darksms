package com.viewpagerindicator;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/com/viewpagerindicator/TitleProvider.java $
 */

/**
 * A TitleProvider provides the title to display according to a view.
 */
public interface TitleProvider {
    /**
     * Returns the title of the view at position
     * @param position
     * @return
     */
    public String getTitle(int position);
}
