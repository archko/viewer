package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.artifex.solib.MuPDFWidget;

public class DocPdfView extends DocView
{
    private static final String  TAG           = "DocPdfView";
    protected static final int    SCROLL_GAP = 5;
    private int tapPageMargin;

    public DocPdfView(Context context) {
        this(context, null);
    }

    public DocPdfView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DocPdfView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        tapPageMargin = (int)dm.xdpi;
        if (tapPageMargin < 100)
            tapPageMargin = 100;
        if (tapPageMargin > dm.widthPixels/5)
            tapPageMargin = dm.widthPixels/5;
    }

    //  this is set while a signature is being edited
    private MuPDFWidget sigEditingWidget = null;

    @Override
    protected boolean handleFullscreenTap(float fx, float fy)
    {
        Point p = eventToScreen(fx,fy);
        final DocPageView dpv = findPageViewContainingPoint(p.x, p.y, false);
        if (dpv==null)
            return false;
        p = dpv.screenToPage(p);
        return dpv.handleFullscreenTap(p.x, p.y);
    }

    //  for PDF files, we're not entertaining single-tap, since the text can't be edited.
    protected boolean canEditText() {return false;}

    protected boolean onSingleTap(float x, float y, DocPageView dpv)
    {
        final Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        if (y < tapPageMargin) {
            smoothScrollBy(getScrollX(), viewport.height() - SCROLL_GAP);
            return true;
        } else if (y > super.getHeight() - tapPageMargin) {
            smoothScrollBy(getScrollX(), -viewport.height() + SCROLL_GAP);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void doDoubleTap(float fx, float fy)
    {
        //  note: DocView.doDoubleTap() restricts double-tapping to editable docs.
        //  here we don't, we use it to select text.

        //  but if editing is configured off, we don't allow this.
        if (!mDocCfgOptions.isEditingEnabled())
            return;

        //  not if we're in full-screen
        if (((NUIDocView)mHostActivity).isFullScreen())
            return;

        doDoubleTap2(fx, fy);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        super.onLayout(changed, left, top, right, bottom);

        if (finished())
            return;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //  prevent further touch event if this view is done.
        if (finished())
            return true;

        return super.onTouchEvent(event);
    }

    @Override
    public void onReloadFile()
    {
        for (int i=0; i<getPageCount(); i++)
        {
            DocMuPdfPageView cv = (DocMuPdfPageView)getOrCreateChild(i);
            cv.onReloadFile();
        }
    }
}
