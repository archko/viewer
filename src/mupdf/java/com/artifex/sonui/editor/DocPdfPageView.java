package com.artifex.sonui.editor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.MuPDFWidget;

public class DocPdfPageView extends DocPageView
{
    public DocPdfPageView(Context context, ArDkDoc theDoc)
    {
        super(context, theDoc);
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        //  draw the base page
        super.onDraw(canvas);

        //  if we're not valid, do no more.
        if (!isValid())
            return;

        if (mPage==null)
            return;

        //  draw ink annotations

        //  draw selection
        drawSelection(canvas);

        //  draw search highlight
        drawSearchHighlight(canvas);
    }

    protected void drawSelection(Canvas canvas)
    {
    }

    protected void drawSearchHighlight(Canvas canvas)
    {
    }

    public void createNote(float x, float y)
    {
        //  where, in page coordinates
        PointF pScreen = new PointF(x, y);
        PointF pPage = screenToPage(pScreen);

        //  create it
        getDoc().createTextAnnotationAt(pPage, getPageNumber());

        invalidate();
    }

    public void createSignatureAt(float x, float y)
    {
        //  where, in page coordinates
        PointF pScreen = new PointF(x, y);
        PointF pPage = screenToPage(pScreen);

        //  create it
        getDoc().createSignatureAt(pPage, getPageNumber());
    }

    public void collectFormFields()
    {
    }

    public MuPDFWidget getNewestWidget()
    {
        return null;
    }

    //-----------------------------------------------------

    @Override
    protected void launchHyperLink(final String url)
    {
        //  ask first

        /*final DocPdfPageView dppv = this;
        final Context context = getContext();
        Utilities.yesNoMessage((Activity)context,
                context.getString(R.string.sodk_editor_open_link),
                "\n"+url+"\n",
                context.getString(R.string.sodk_editor_OK),
                context.getString(com.artifex.sonui.editor.R.string.sodk_editor_cancel),
                new Runnable() {
                    @Override
                    public void run() {
                        //  OK, so do it
                        DocPdfPageView.super.launchHyperLink(url);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        //  cancelled
                    }
                });*/

    }
}
