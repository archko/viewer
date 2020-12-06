package com.artifex.sonui.editor;

import android.content.Context;
import android.util.AttributeSet;

public class NUIDocViewMuPdf extends NUIDocViewPdf
{
    public NUIDocViewMuPdf(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocViewMuPdf(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocViewMuPdf(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    private void initialize(Context context)
    {
    }

    @Override
    public void setConfigurableButtons()
    {
        super.setConfigurableButtons();
    }

    @Override
    protected void checkXFA()
    {
        //  do nothing
    }
}
