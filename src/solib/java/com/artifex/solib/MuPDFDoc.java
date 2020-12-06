package com.artifex.solib;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.Location;
import com.artifex.mupdf.fitz.Outline;
import com.artifex.mupdf.fitz.PDFAnnotation;
import com.artifex.mupdf.fitz.PDFDocument;
import com.artifex.mupdf.fitz.PDFObject;
import com.artifex.mupdf.fitz.PDFPage;
import com.artifex.mupdf.fitz.Page;
import com.artifex.mupdf.fitz.SeekableInputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MuPDFDoc extends ArDkDoc
{
    private Document mDocument = null;
    private ArrayList<MuPDFPage> mPages = new ArrayList<>();
    private Worker mWorker = null;
    private int mPageCount = 0;
    private int mPageNumber = 0;
    private boolean mLoadAborted = false;
    private boolean mDestroyed = false;
    private ConfigOptions mDocCfgOpts = null;
    private SODocLoadListener mListener = null;
    private Context mContext;

    //  an interface for receiving javascript alerts
    public interface JsEventListener {
        void onAlert(String message);
    }

    //  an optional listener set by the holder of this doc
    private JsEventListener jsEventListener2 = null;
    public void setJsEventListener(JsEventListener listener) {jsEventListener2=listener;}

    //  a listener passed to mupdf, which in turn calls the holder's listener.
    public PDFDocument.JsEventListener jsEventListener = new PDFDocument.JsEventListener() {
        @Override
        public void onAlert(String message)
        {
            if (jsEventListener2!=null)
                jsEventListener2.onAlert(message);
        }
    };

    MuPDFDoc(Looper looper, SODocLoadListener listener, Context context, ConfigOptions cfg)
    {
        mListener = listener;
        mContext = context;
        mDocCfgOpts = cfg;

        mPageCount = 0;
        mPageNumber = 0;

        mLoadTime=System.currentTimeMillis();
        // initialise last save time to be our load time
        mLastSaveTime = mLoadTime;

        mWorker = new Worker(looper);
    }

    public void setDocument(Document document)
    {
        mDocument = document;
    }

    private long mLoadTime;
    public long getLoadTime() {return mLoadTime;}

    private String mOpenedPath = null;
    public void setOpenedPath(String path) {mOpenedPath = path;}
    public String getOpenedPath() {return mOpenedPath;}

    public void startWorker ()
    {
        mWorker.start();
    }

    public Worker getWorker() {return mWorker;}

    private void addPage(Page page)
    {
        MuPDFPage mpage = new MuPDFPage(this, page, mNumPages);
        mPages.add(mpage);
        mNumPages = mPages.size();
    }

    public Document getDocument() {
        return mDocument;
    }

    public static PDFDocument getPDFDocument(Document doc)
    {
        try {
            PDFDocument pdfDoc = (PDFDocument)doc;
            return pdfDoc;
        }
        catch (Exception e)
        {
        }

        return null;
    }

    @Override
    public ArDkPage getPage(int pageNumber, SOPageListener listener)
    {
        MuPDFPage page = mPages.get(pageNumber);
        page.addPageListener(listener);

        return page;
    }

    private boolean mLastSaveWasIncremental;
    public boolean lastSaveWasIncremental() {return mLastSaveWasIncremental;}

    private long mLastSaveTime = 0;
    public long getLastSaveTime() {return mLastSaveTime;}

    private boolean mForceReload;
    public boolean getForceReload() {return mForceReload;}
    @Override
    public void setForceReload(boolean force) {mForceReload = force;}

    private boolean mForceReloadAtResume;
    public boolean getForceReloadAtResume() {return mForceReloadAtResume;}
    @Override
    public void setForceReloadAtResume(boolean force) {
        mForceReloadAtResume = force;
    }

    //  this function tells us whether mupdf can save the file.
    //  It would be good if this were part of the mupdf API instead.
    public boolean canSave()
    {
        //  this doc can't be saved unless the underlying mupdf document is a PDF doc.
        PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
        if (pdfDoc==null)
            return false;
        return true;
    }

    public boolean canPrint()
    {
        //  if this doc can't be saved, it also can't be printed.
        //  that's because for printing we require a PDF file, and one can't
        //  be produced by mupdf for non-PDF formats.
        return canSave();
    }

    //  this tracks whether the document has been modified by the user.
    private boolean mIsModified = false;
    void setModified(boolean val) {mIsModified = val;}

    @Override
    public boolean getHasBeenModified() {

        //return mDocument.hasUnsavedChanges();

        //  The issue with using mDocument.hasUnsavedChanges() here is that when mupdf
        //  repairs a document, hasUnsavedChanges() returns true.
        //  we'll instead use setModified() to track when the user has made
        //  changes.  Which at this writing is only when adding annotations.

        return mIsModified;
    }

    @Override
    public void abortLoad() {
        destroyDoc();
    }

    @Override
    public void destroyDoc()
    {
        if (mDestroyed)
            return;
        mDestroyed = true;

        mLoadAborted = true;

        //  disable javascript, which should be done on the doc's worker thread,
        //  When that's complete, handle the rest of the destruction.
        Worker worker = getWorker();
        worker.add(new Worker.Task()
        {
            public void work()
            {
                if (mDocument != null)
                {
                    PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
                    if (pdfDoc!=null) {
                        //  disable javascript
                        pdfDoc.setJsEventListener(null);
                        pdfDoc.disableJs();
                    }
                }
            }

            public void run()
            {
                //  stop the worker
                if (mWorker!=null) {
                    mWorker.stop();
                    mWorker = null;
                }

                //  get rid of the pages
                if (mPages!=null)
                {
                    for (MuPDFPage page : mPages) {
                        page.releasePage();
                    }
                    mPages.clear();
                    mPages = null;
                }

                mPageCount = 0;
                mPageNumber = 0;

                if (mDocument!=null)
                    mDocument.destroy();
                mDocument = null;
            }
        });

    }

    @Override
    public void clearSelection()
    {
        int oldPage = selectedAnnotPagenum;
        selectedAnnotPagenum = -1;
        selectedAnnotIndex = -1;

        if (oldPage != -1) {
            mPages.get(oldPage).clearSelection();
        }

        int textSelPage = MuPDFPage.getTextSelPageNum();
        if (textSelPage != -1)
        {
            mPages.get(textSelPage).clearSelectedText();
        }
    }

    private int selectedAnnotIndex = -1;
    private int selectedAnnotPagenum = -1;
    public int getSelectedAnnotationPagenum() {return selectedAnnotPagenum;}
    public void setSelectedAnnotation(int pagenum, int index)
    {
        selectedAnnotPagenum = pagenum;
        selectedAnnotIndex = index;
    }
    public PDFAnnotation getSelectedAnnotation()
    {
        if (selectedAnnotPagenum!=-1 && selectedAnnotIndex!=-1 && selectedAnnotPagenum<mPages.size())
        {
            MuPDFPage page = mPages.get(selectedAnnotPagenum);
            PDFAnnotation annot = page.getAnnotation(selectedAnnotIndex);
            return annot;
        }
        return null;
    }

    @Override
    public int getNumPages() {
        return mNumPages;
    }

    //  a list of page numbers that have unapplied redaction marks
    private List<Integer> pagesWithRedactions = new ArrayList<Integer>();

    private void addPageWithRedactions(int pageNum) {
        Integer thePage = new Integer(pageNum);
        if (!pagesWithRedactions.contains(thePage))
            pagesWithRedactions.add(thePage);
    }

    private void removePageWithRedactions(int pageNum) {
        Integer thePage = new Integer(pageNum);
        if (pagesWithRedactions.contains(thePage))
            pagesWithRedactions.remove(thePage);
    }

    public void addRedactAnnotation()
    {
        int pageNum = MuPDFPage.getTextSelPageNum();
        if (pageNum != -1)
        {
            mPages.get(pageNum).addRedactAnnotation();
            update(pageNum);
            addPageWithRedactions(pageNum);
        }
    }

    public void addRedactAnnotation(int pageNum, Rect r)
    {
        if (pageNum != -1)
        {
            mPages.get(pageNum).addRedactAnnotation(r);
            update(pageNum);
            addPageWithRedactions(pageNum);
        }
    }

    public boolean hasRedactionsToApply()
    {
        return !pagesWithRedactions.isEmpty();
    }

    public void applyRedactAnnotation()
    {
        int count = mPages.size();
        for (int i=0; i<count; i++)
        {
            if (mPages.get(i).countAnnotations(PDFAnnotation.TYPE_REDACT)>0)
            {
                mPages.get(i).applyRedactAnnotation();
                update(i);
            }
        }
        pagesWithRedactions.clear();
    }

    private String mAuthor=null;
    public boolean setAuthor(String author)
    {
        mAuthor = author;
        return true;
    }

    public String getAuthor()
    {
        return mAuthor;
    }

    @Override
    public void createTextAnnotationAt(PointF point, int pageNum)
    {
        String author = getAuthor();
        mPages.get(pageNum).createTextAnnotationAt(point, author);
        update(pageNum);
    }

    @Override
    public void createSignatureAt(PointF point, final int pageNum)
    {
        mPages.get(pageNum).createSignatureAt(point);
        update(pageNum);
    }

    public void deleteWidget(int pageNum, MuPDFWidget widget)
    {
        mPages.get(pageNum).deleteWidget(widget);
        update(pageNum);
    }

    private static String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    public void update(final int pageNumber)
    {
        Worker worker = getWorker();
        worker.add(new Worker.Task()
        {
            public void work()
            {
                MuPDFPage page = mPages.get(pageNumber);
                if (page != null) {
                    page.update();
                }

            }
            public void run()
            {
                if (mListener!=null)
                {
                    //  this should cause the lib client to re-render pages.
                    mListener.onDocComplete();
                    //  this should cause the lib client to update its UI.
                    mListener.onSelectionChanged(pageNumber, pageNumber);
                }
            }
        });
    }

    public void onSelectionUpdate(final int pageNumber)
    {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                setSelectionStartPage(pageNumber);
                setSelectionEndPage(pageNumber);
                if (mListener != null)
                    mListener.onSelectionChanged(pageNumber, pageNumber);
            }
        });
    }

    @Override
    public boolean providePassword(String password)
    {
        boolean val =  mDocument.authenticatePassword(password);
        if (!val) {

            //  arrange for another password request
            if (mListener!=null)
                mListener.onError(ArDkLib.SmartOfficeDocErrorType_PasswordRequest, 0);
            return false;
        }

        //  things to do after the password is validated
        afterValidation();

        //  start loading pages
        loadNextPage();
        return true;
    }

    public void afterValidation()
    {
        //  set the page count
        mPageCount = mDocument.countPages();

        //  enable javascript
        if (mDocCfgOpts.isFormFillingEnabled())
        {
            PDFDocument pdfDoc = MuPDFDoc.getPDFDocument(mDocument);
            if (pdfDoc!=null) {

                //  enable javascript and establish a listener
                pdfDoc.enableJs();
                pdfDoc.setJsEventListener(jsEventListener);
            }
        }
    }

    @Override
    public void processKeyCommand(int command) {
        //  TODO
    }

    public interface MuPDFEnumerateTocListener {
        void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y);
    }

    public int enumerateToc(MuPDFEnumerateTocListener listener)
    {
        if (listener !=null)
        {
            try
            {
                handleCounter = 0;
                Outline[] outlines = mDocument.loadOutline();
                processOutline(outlines, handleCounter, listener);
            }
            catch (Exception e)
            {
            }
        }

        return 0;
    }

    int handleCounter = 0;

    private void processOutline(Outline[] outlines, int parent, MuPDFEnumerateTocListener listener)
    {
        if (outlines!=null && outlines.length>0)
        {
            for (int i=0; i<outlines.length; i++)
            {
                Outline  outline = outlines[i];
                Location loc     = mDocument.resolveLink(outline);
                int      page    = mDocument.pageNumberFromLocation(loc);

                if (page>=0)
                {
                    MuPDFPage mppage = mPages.get(page);
                }

                handleCounter++;
                listener.nextTocEntry(handleCounter, parent, page, outline.title, outline.uri, loc.x, loc.y);

                Outline[] down = outline.down;
                processOutline(down, handleCounter, listener);
            }
        }
    }

    public void loadNextPage()
    {
        if (mLoadAborted)
        {
            if (mListener!=null)
                mListener.onDocComplete();  //  TODO: error?
            return;
        }

        getWorker().add(new Worker.Task()
        {
            private boolean done = false;
            public void work()
            {
                //  this part takes place on the background thread

                if (mPageNumber < mPageCount)
                {
                    Page page = mDocument.loadPage(mPageNumber);
                    addPage(page);

                    //  keep track of pages with un-applied redaction marks
                    int numRedact = mPages.get(mPageNumber).countAnnotations(PDFAnnotation.TYPE_REDACT);
                    if (numRedact>0)
                        addPageWithRedactions(mPageNumber);
                }
                else
                {
                    //  no more pages
                    done = true;
                }
            }

            public void run()
            {
                //  this part takes place on the main thread

                if (done)
                {
                    if (mListener!=null)
                        mListener.onDocComplete();
                }
                else
                {
                    mPageNumber++;
                    if (mListener!=null)
                        mListener.onPageLoad(mPageNumber);

                    loadNextPage();
                }
            }
        });

    }

    public interface ReloadListener {
        void onReload();
    }

    public void reloadFile(final String path, final ReloadListener listener, final boolean forced)
    {
        final MuPDFDoc thisMupdfDoc = this;
        getWorker().add(new Worker.Task()
        {
            private Document newDoc;
            private ArrayList<MuPDFPage> newPages = new ArrayList<>();

            public void work()
            {
                //  this part takes place on the background thread
                //  here we load the file and make a new page list.
                //  we'll make them active on the foreground thread

                //  load the file
                newDoc = openFile(path);

                //  build a new page list
                int count = newDoc.countPages();
                for (int i=0; i<count; i++)
                {
                    PDFPage page = (PDFPage)newDoc.loadPage(i);
                    MuPDFPage mpage;
                    if (forced)
                    {
                        //  in this case we know that the population of pages is the same,
                        //  so we can just copy the old ones and give each one a new PDFPage.
                        mpage = mPages.get(i);
                        mpage.setPage(page);
                    }
                    else
                    {
                        mpage = new MuPDFPage(thisMupdfDoc, page, i);
                    }
                    newPages.add(mpage);
                }
            }

            public void run()
            {
                //  this part takes place on the main thread

                //  replace the page list
                ArrayList<MuPDFPage> oldPages = mPages;
                mPages = newPages;

                //  replace the Document
                Document oldDoc = mDocument;
                mDocument = newDoc;

                //  clear the now-unreferenced page list
                if (!forced) {
                    for (int i=0; i<oldPages.size(); i++) {
                        MuPDFPage page = oldPages.get(i);
                        if (page!=null)
                            page.releasePage();
                    }
                }
                oldPages.clear();

                //  destroy the now-unreferenced Document
                oldDoc.destroy();

                //  tell someone
                listener.onReload();
            }
        });
    }

    public static Document openFile(String path)
    {
        Document doc = null;

        try {

            SOSecureFS mSecureFs = ArDkLib.getSecureFS();
            if (mSecureFs != null && mSecureFs.isSecurePath(path))
            {
                doc = openSecure(path, mSecureFs);
            }
            else
            {
                doc = Document.openDocument(path);
            }
        }
        catch (Exception e)
        {
            //  exception while trying to open the file
            doc = null;
        }

        return doc;
    }

    private static Document openSecure(String path, final SOSecureFS secureFS)
    {
        try
        {
            final Object handle = secureFS.getFileHandleForReading(path);

            SeekableInputStream stream = new SeekableInputStream()
            {
                public void close() throws IOException
                {
                    secureFS.closeFile(handle);
                }

                public int read(byte[] b) throws IOException
                {
                    int numBytes = secureFS.readFromFile(handle, b);
                    return (numBytes == 0) ? -1 : numBytes;
                }

                public long seek(long offset, int whence) throws IOException
                {
                    long current = secureFS.getFileOffset(handle);
                    long length = secureFS.getFileLength(handle);
                    long pos = 0;

                    switch (whence)
                    {
                        case SEEK_SET:
                            pos = offset;
                            break;

                        case SEEK_CUR:
                            pos = current+offset;
                            break;

                        case SEEK_END:
                            pos = length+offset;
                            break;
                    }
                    secureFS.seekToFileOffset(handle, pos);
                    return pos;
                }

                public long position() throws IOException
                {
                    long current = secureFS.getFileOffset(handle);
                    return current;
                }
            };

            String extension = FileUtils.getExtension(path);
            Document doc = Document.openDocument(stream, extension);

            return doc;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private boolean isNull(PDFObject obj)
    {
        return obj==null || obj.equals(PDFObject.Null);
    }

    @Override
    public boolean hasXFAForm()
    {
        //  only valid for PDF docs
        if (!(getDocument() instanceof PDFDocument))
            return false;

        PDFObject obj = ((PDFDocument)getDocument()).getTrailer();

        if (!isNull(obj))
            obj = obj.get("Root");
        if (!isNull(obj))
            obj = obj.get("AcroForm");
        if (!isNull(obj))
            obj = obj.get("XFA");

        if (isNull(obj))
            return false;

        return true;
    }

    @Override
    public boolean hasAcroForm()
    {
        //  only valid for PDF docs
        if (!(getDocument() instanceof PDFDocument))
            return false;

        PDFObject obj = ((PDFDocument)getDocument()).getTrailer();

        if (!isNull(obj))
            obj = obj.get("Root");
        if (!isNull(obj))
            obj = obj.get("AcroForm");
        if (!isNull(obj))
            obj = obj.get("Fields");

        if (isNull(obj))
            return false;

        return obj.size() > 0;
    }

    @Override
    public String getDateFormatPattern()
    {
        //  this is the date format for date strings returned from muPDF docs.
        //  its used to convert date strings to Date objects.
        return "yyyy-MM-dd HH:mm";
    }

    //  keep track of whether we should show ther XFA warning
    private boolean mShowXFAWarning = false;
    public void setShowXFAWarning(boolean val) {mShowXFAWarning = val;}
    public boolean getShowXFAWarning() {return mShowXFAWarning;}

}
