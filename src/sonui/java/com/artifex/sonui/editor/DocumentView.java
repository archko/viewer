/**
 *  This class implements a new no-UI API that allows a developer to
 *  embed a DocumentView into their app and have total control of the UI.
 *
 * Copyright (C) Artifex, 2020. All Rights Reserved.
 *
 * @author Artifex
 */

package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;

public class DocumentView extends NUIView {

    public DocumentView(Context context) {
        super(context);
    }

    public DocumentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * start the view with a given file path
     *
     * @param uri       the file path
     * @param page      start at a given page number (zero-based)
     * @param showUI    controls whether the built-in UI is used, true = yes
     */
    public void start(Uri uri, int page, boolean showUI)
    {
        makeNUIView(uri, null);
        addView(mDocView);
        mDocView.setDocumentListener(mDocumentListener);
        if (page<0)
            page = 0;
        ViewingState vstate = new ViewingState(page);
        mDocView.start(uri, false, vstate, null, mDoneListener, showUI);
    }

    /**
     *  Show the list of page thumbnails.
     */
    public void showPageList()
    {
        if (mDocView != null)
            mDocView.showPages();
    }

    /**
     *  Hide the list of page thumbnails.
     */
    public void hidePageList()
    {
        if (mDocView != null)
            mDocView.hidePages();
    }

    /**
     *  Determine if the page list is visible
     *
     * @return  true if it's visible
     */
    public boolean isPageListVisible()
    {
        if (mDocView!=null)
            return mDocView.isPageListVisible();
        return false;
    }

    /**
     * specify a Runnable that should run when the selection changes.
     *
     * @param updateUIRunnable - the Runnable
     */
    public void setOnUpdateUI(Runnable updateUIRunnable)
    {
        if (mDocView != null)
            mDocView.setOnUpdateUI(updateUIRunnable);
    }

    /**
     * get the current page count.
     *
     * @return - the page count
     */
    public int getPageCount()
    {
        if (mDocView != null)
            return mDocView.getPageCount();
        return 0;
    }

    /**
     * find out if the document has been changed since it was last opened or saved.
     *
     * @return - true if it's been modified, false if not.
     */
    public boolean isDocumentModified()
    {
        /*if (mDocView != null)
            return mDocView.isDocumentModified();*/
        return false;
    }

    /**
     * go to the first page
     */
    public void firstPage()
    {
        if (mDocView != null)
            mDocView.firstPage();
    }

    /**
     * go to the last page
     */
    public void lastPage()
    {
        if (mDocView != null)
            mDocView.lastPage();
    }

    /**
     * get the current page number
     *
     * @return - the page number
     */
    public int getPageNumber()
    {
        if (mDocView != null)
            return mDocView.getPageNumber();
        return 0;
    }

    /**
     * use the built-in UI to display the table of contents
     */
    public void tableOfContents()
    {
        if (mDocView != null)
            mDocView.tableOfContents();
    }

    /**
     * return whether the document has a table of contents
     *
     * @return
     */
    public boolean isTOCEnabled()
    {
        if (mDocView != null)
            return mDocView.isTOCEnabled();
        return false;
    }

    /**
     * interface for enumerating the table of contents
     */
    public interface EnumeratePdfTocListener {
        /**
         * nextTocEntry is called once for each entry in the Table of Contents
         * handle and parentHandle can be used to navigate the TOC hierarchy
         * if page>=0 it's an internal link, so use x and y
         * if page<0 and url in not null, url is an external link
         *
         * @param handle        my ID number in the hierarchy
         * @param parentHandle  my parent's ID number in the hierarchy
         * @param page          target page number
         * @param label         user-friendly label
         * @param url           url for an external link
         * @param x             x - coordinate on the page.
         * @param y             y - coordinate on the page.
         */
        void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y);
    }

    /**
     * enumerate the document's TOC. The listener's nextTocEntry() function
     * will be called for each TOC entry.
     *
     * @param listener
     */
    public void enumeratePdfToc(final EnumeratePdfTocListener listener)
    {
    }

    /**
     * given a page number and a rectangle, scroll so that location in the document is
     * visible.
     *
     * @param page      the page
     * @param box       the rectangle
     */
    public void gotoInternalLocation(int page, RectF box)
    {
        if (mDocView != null)
            mDocView.gotoInternalLocation(page, box);
    }

    /**
     * scroll to the given page number.
     *
     * @param pageNum
     */
    public void goToPage(int pageNum)
    {
        if (mDocView != null) {

            //  enforce range
            if (pageNum<0)
                pageNum = 0;
            if (pageNum>getPageCount()-1)
                pageNum = getPageCount()-1;

            //  put the keyboard away
            Utilities.hideKeyboard(getContext());

            //  go
            mDocView.goToPage(pageNum, true);
        }
    }

    /**
     * interface for monitoring changes in which page is being displayed
     */
    public interface ChangePageListener {
        void onPage(int pageNumber);
    }
    private ChangePageListener mChangePageListener = null;

    /**
     * set a ChangePageListener on the document.
     *
     * @param listener - the listener
     */
    public void setPageChangeListener(ChangePageListener listener) {
        mChangePageListener = listener;
        if (mDocView != null) {
            mDocView.setPageChangeListener(new NUIDocView.ChangePageListener() {
                @Override
                public void onPage(int pageNumber) {
                    if (mChangePageListener!=null) {
                        mChangePageListener.onPage(pageNumber);
                    }
                }
            });
        }
    }

    /**
     * call this to set a DocumentListener on this view.
     */
    protected DocumentListener mDocumentListener = null;
    public void setDocumentListener(DocumentListener listener)
    {
        mDocumentListener = listener;
    }

    /**
     * return the view's current x-scroll position
     * a return value of -1 indicates an error
     */
    public int getScrollPositionX()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionX();
        return -1;
    }

    /**
     * return the view's current y-scroll position
     * a return value of -1 indicates an error
     */
    public int getScrollPositionY()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionY();
        return -1;
    }

    /**
     * return the view's current scale factor
     * a return value of -1 indicates an error
     */
    public float getScaleFactor()
    {
        if (mDocView != null)
            return mDocView.getScaleFactor();
        return -1;
    }

    /**
     * set new scale factor, x- and y-scroll values for the view.
     */
    public void setScaleAndScroll (float newScale, int newX, int newY)
    {
        if (mDocView != null)
            mDocView.setScaleAndScroll(newScale, newX, newY);
    }
}
