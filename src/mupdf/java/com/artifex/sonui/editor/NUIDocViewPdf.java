package com.artifex.sonui.editor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.artifex.mupdf.viewer.R;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.FileUtils;
import com.artifex.solib.MuPDFDoc;
import com.artifex.solib.SOLinkData;

public class NUIDocViewPdf extends NUIDocView
{

    private Button mTocButton;
    private boolean tocEnabled = false;
    private String path;

    public NUIDocViewPdf(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocViewPdf(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocViewPdf(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    private void initialize(Context context)
    {
    }

    @Override
    protected int getLayoutId()
    {
        return R.layout.sodk_editor_pdf_document;
    }

    @Override
    protected DocView createMainView(Activity activity)
    {
        return new DocPdfView(activity);
    }

    @Override
    protected void afterFirstLayoutComplete()
    {
        super.afterFirstLayoutComplete();

        if (mDocCfgOptions.featureTracker != null && !mDocCfgOptions.isRedactionsEnabled()) {
        }

        setupToc();
    }

    private void setupToc()
    {
        //  create the button
        mTocButton = new Button(getContext());// createToolbarButton(R.id.toc_button);
        mTocButton.setEnabled(false);
        tocEnabled = false;
    }

    @Override
    protected PageAdapter createAdapter()
    {
        return new PageAdapter(activity(), this, PageAdapter.PDF_KIND);
    }

    protected void updateUIAppearance()
    {
        getPdfDocView().onSelectionChanged();

        //  send update UI notification
        doUpdateCustomUI();
    }

    public DocPdfView getPdfDocView()
    {
        return (DocPdfView)getDocView();
    }

    @Override
    protected void goBack()
    {
        super.goBack();
    }

    private void onTocButton(View v )
    {
        TocDialog dialog = new TocDialog(getContext(), getDoc(), this,
                new TocDialog.TocDialogListener() {
                    @Override
                    public void onItem(SOLinkData linkData)
                    {
                        //  Add a history entry for the spot we're leaving.
                        DocView dv = getDocView();

                        //  add history for where we're going
                        int dy = dv.scrollBoxToTopAmount(linkData.page, linkData.box);

                        //  scroll to the new page and box
                        dv.scrollBoxToTop(linkData.page, linkData.box);

                        updateUIAppearance();
                    }
                });
        dialog.show();
    }

    @Override
    protected void onDeviceSizeChange()
    {
        super.onDeviceSizeChange();

        TocDialog.onRotate();
    }

    @Override
    public void onClick(View v) {
        // Ignore button presses while we are finishing up.
        if (mFinished) {
            return;
        }

        super.onClick(v);

        if (v == mTocButton)
            onTocButton(v);
    }

    @Override
    protected void prepareToGoBack()
    {
        // if the document open failed, we'll have no doc.
        if (mSession!=null && mSession.getDoc()==null)
            return;
    }

    private boolean firstSelectionCleared = false;

    @Override
    protected void onDocCompleted()
    {
        //  not if we're finished
        if (mFinished)
            return;

        //  address a bug where PDF files with an annotation open initially
        //  with the annotation selected.
        if (!firstSelectionCleared) {
            mSession.getDoc().clearSelection();
            firstSelectionCleared = true;
        }

        //  get the page count
        mPageCount = mSession.getDoc().getNumPages();

        if (mPageCount<=0) {
            //  no pages is an error
            //String message = Utilities.getOpenErrorDescription(getContext(), 17);
            //Utilities.showMessage((Activity)getContext(), getContext().getString(R.string.sodk_editor_error), message);
            //disableUI();
            return;
        }

        //  we may be called when pages have been added or removed
        //  so update the page count and re-layout
        mAdapter.setCount(mPageCount);
        layoutNow();

        //  disable TOC button
        mTocButton.setEnabled(false);
        tocEnabled = false;

        //  enumerate the TOC.  If there is at least one entry, enable the button
        ArDkLib.enumeratePdfToc(getDoc(), new ArDkLib.EnumeratePdfTocListener() {
            @Override
            public void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y)
            {
                //  enable the TOC button
                mTocButton.setEnabled(true);
                tocEnabled = true;
            }
        });
    }

    @Override
    public void reloadFile()
    {
        MuPDFDoc doc = ((MuPDFDoc)getDoc());
        //String path;
        boolean forced = false;
        boolean forcedAtResume;

        forcedAtResume = doc.getForceReloadAtResume();
        forced = doc.getForceReload();
        doc.setForceReloadAtResume(false);
        doc.setForceReload(false);

        /*if (forcedAtResume)
        {
            //  we were set to force a reload on resume.
            //  this is the save-while-backgrounding case.
            //  see: pauseHandler in DataLeakHandlers
            //  for this case we use the internal path.
            path  = mSession.getFileState().getInternalPath();
        }
        else if (forced)
        {
            path  = mSession.getFileState().getUserPath();
            if (path == null)
                path  = mSession.getFileState().getOpenedPath();
        }
        else
        {
            path  = mSession.getFileState().getOpenedPath();

            //  check for incremental saving.
            //  we don't reload in that case.
            if ( doc.lastSaveWasIncremental() )
                return;

            //  check if the original file has been updated since it was loaded
            //  this will be true when the previous save operation replaced the original
            //  we don't reload unless this is true.
            long   loadTime = doc.getLoadTime();
            long   modTime  = FileUtils.fileLastModified( path );

            //  FileUtils.fileLastModified may return 0, so don't use it for comparison below
            if (modTime == 0)
                return;

            if ( modTime < loadTime )
                return;
        }*/

        // we're doing a save as - ensure we pick up the new document path and set the MuPDFDoc's
        // openedPath member accordingly
        if (!forcedAtResume)
            doc.setOpenedPath( path );

        //  reload the file and refresh page list
        doc.reloadFile(path, new MuPDFDoc.ReloadListener()
        {
            @Override
            public void onReload()
            {
                //  this part takes place on the main thread

                //  tell the views about it.
                //  they will in turn tell their page views about it.
                if (getDocView() != null)
                    getDocView().onReloadFile();

            }
        }, forced || forcedAtResume);
    }

    protected void checkXFA()
    {
        if (mDocCfgOptions.isFormFillingEnabled())
        {
            //  to see if this is the first call
            boolean first = (mPageCount==0);
            if (first)
            {
                //  when the doc starts loading, see if it has only XFA forms.
                //  if so, display a message. But we still open the document.
                boolean hasXFA = getDoc().hasXFAForm();
                boolean hasAcro = getDoc().hasAcroForm();
                if (hasXFA && !hasAcro)
                {
                    /*Utilities.showMessage((Activity)getContext(),
                            getContext().getString(R.string.sodk_editor_xfa_title),
                            getContext().getString(R.string.sodk_editor_xfa_body));*/
                }

                //  if this is a dual form document, set up for showing the warning
                if (hasXFA && hasAcro)
                    ((MuPDFDoc)getDoc()).setShowXFAWarning(true);
            }
        }
    }

    protected void onPageLoaded(int pagesLoaded)
    {
        checkXFA();

        super.onPageLoaded(pagesLoaded);
    }

    @Override
    protected void layoutAfterPageLoad()
    {
    }

    @Override
    protected boolean shouldConfigureSaveAsPDFButton()
    {
        return false;
    }

    @Override
    protected void onFullScreen(View v)
    {
        //  reset modes (draw and note)
        updateUIAppearance();

        super.onFullScreen(v);
    }

    //  for no-UI API
    @Override
    public void tableOfContents()
    {
        onTocButton(null);
    }

    //  for no-UI API
    @Override
    public boolean isTOCEnabled()
    {
        return tocEnabled;
    }
}
