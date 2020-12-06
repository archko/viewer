package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.MuPDFPage;

public class DocMuPdfPageView extends DocPdfPageView
{
    private final String mDebugTag = "DocPdfPageView";


    public DocMuPdfPageView(Context context, ArDkDoc theDoc)
    {
        super(context, theDoc);
    }

    @Override
    protected void setOrigin()
    {
        //  get a copy of the pagerect
        Rect originRect = new Rect();
        originRect.set(mPageRect);

        //  adjust based on the location of our window
        View window = ((Activity)getContext()).getWindow().getDecorView();
        int windowLoc[] = new int[2];
        window.getLocationOnScreen(windowLoc);
        originRect.offset(-windowLoc[0], -windowLoc[1]);

        //  set the origin
        mRenderOrigin.set(originRect.left, originRect.top);
    }

    @Override
    protected void changePage(int thePageNum)
    {
        super.changePage(thePageNum);

        collectFormFields();
    }

    @Override
    public void update(RectF area)
    {
        //  we're being asked to update a portion of the page.

        //  not if we're finished.
        if (isFinished())
            return;

        //  not if we're not visible
        if (!isShown())
            return;

        //  right now updates are coming just from selection changes
        //  so this is all we need to do.
        //  later we'll need to re-render the area.
        invalidate();
    }

    public void onReloadFile()
    {
        MuPDFDoc doc = ((MuPDFDoc)getDoc());

        //  after a reload, we refresh our page from the document
        dropPage();
        changePage(getPageNumber());

        //  because we have a new page, the current state of editing form fields
        //  is invalid, so we have to start over.
        ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();
        if (docCfgOpts.isFormFillingEnabled()) {
            collectFormFields();
            invalidate();
        }

        doc.update(getPageNumber());
    }

    @Override
    protected boolean canDoubleTap(int x, int y)
    {
        //  in PDF documents, a single-tap will select a marked redaction.
        //  We want to prevent a double-tap from also selecting the inderlying text.
        //  so prevent double-=tapping of marked redactions.

        MuPDFPage pdfPage = ((MuPDFPage)mPage);
        Point pPage = screenToPage(new Point(x,y));
        int index = pdfPage.findSelectableAnnotAtPoint(pPage, MuPDFPage.PDF_ANNOT_REDACT);
        if (index != -1)
            return false;

        return true;
    }

    @Override
    public boolean onSingleTap(int x, int y, boolean canEditText, ExternalLinkListener listener)
    {
        ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();

        //  assume nothing selected
        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        doc.setSelectedAnnotation(-1, -1);

        //  NOTE: when double-tapping, a single-tap will also happen first.
        //  so that must be safe to do.

        Point pPage = screenToPage(x, y);

        boolean stopped = true;
        if (docCfgOpts.isFormFillingEnabled() && docCfgOpts.isEditingEnabled()) {
            //  stop previous editor
            invalidate();
        }

        //  is it a hyperlink?
        if (tryHyperlink(pPage, listener))
            return true;

        if (!docCfgOpts.isFormFillingEnabled()) {
        }

        return false;
    }

    @Override
    public void endRenderPass()
    {
        super.endRenderPass();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        //  see if the active editor wants to handle this event
        return super.dispatchTouchEvent(event);
    }

    protected boolean mFullscreen = false;

    @Override
    public void onFullscreen(boolean bFull)
    {
        mFullscreen = bFull;

        if (mFullscreen)
        {
            //  get out of editing a form
            boolean stopped = true;
            if (getDocView()!=null)
            {
                ConfigOptions docCfgOpts = getDocView().getDocConfigOptions();
                if (docCfgOpts.isFormFillingEnabled() && docCfgOpts.isEditingEnabled()) {
                    //  stop previous editor
                    invalidate();
                }
            }
        }
    }

    public int addRedactAnnotation(Rect r)
    {
        //  how many redactions did we start with?
        MuPDFPage pdfPage = ((MuPDFPage)mPage);
        int count = pdfPage.countAnnotations();

        //  add a new one
        Point p1 = screenToPage(r.left, r.top);
        Point p2 = screenToPage(r.right, r.bottom);
        Rect r2 = new Rect(p1.x, p1.y, p2.x, p2.y);
        ((MuPDFDoc)getDoc()).addRedactAnnotation(getPageNumber(), r2);

        //  assume that the new one is last in the list
        return count;
    }
}
