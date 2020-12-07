package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ListPopupWindow;
import android.widget.RelativeLayout;

import com.artifex.mupdf.viewer.R;
import com.artifex.solib.ArDkBitmap;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.ConfigOptions;
import com.artifex.solib.FileUtils;
import com.artifex.solib.MuPDFLib;

import java.util.ArrayList;

public class NUIDocView
        extends FrameLayout implements DocViewHost, View.OnClickListener
{
    private final String mDebugTag = "NUIDocView";

    protected Activity activity() {return (Activity)getContext();}
    private boolean mStarted = false;
    protected boolean mShowUI = true;

    protected SODocSession mSession;
    private Boolean mEndSessionSilent;
    protected int mPageCount;
    private DocView mDocView;
    private String mDocUserPath;
    protected PageAdapter mAdapter;

    //  start page, get and set
    private int mStartPage=-1;
    protected void setStartPage(int page) {mStartPage = page;}
    protected int getStartPage() {return mStartPage;}

    //  viewing state
    protected ViewingState mViewingState;

    //  Factor by which our rendering buffer is greater than the screen in both width and height
    public final static int OVERSIZE_PERCENT = 20;
    public static int OVERSIZE_MARGIN;

    //  bitmaps for rendering.  We get these from the library, and then hand them to the DocViews.
    private ArDkBitmap[] bitmaps = {null,null};

    protected boolean mIsSession = false;
    private boolean mIsTemplate = false;
    protected Uri mStartUri = null;
    protected ConfigOptions mDocCfgOptions = null;
    private String mForeignData = null;

    // indicates the IME is currently composing text
    private boolean mIsComposing = false;

    //  track if we're active via pause/resume
    private boolean mIsActivityActive = false;
    protected boolean isActivityActive() {return mIsActivityActive;}

    //  current page number
    protected int mCurrentPageNum = 0;


    protected ListPopupWindow   mListPopupWindow;

    //  the library that's in charge of this document
    private ArDkLib mDocumentLib;

    //  a list of files to delete when the document is closed
    private ArrayList<String> mDeleteOnClose = new ArrayList<>();
    public void addDeleteOnClose(String path) {mDeleteOnClose.add(path);}

    //  the currently active instance of us.
    private static NUIDocView mCurrentNUIDocView=null;
    public static NUIDocView currentNUIDocView() {return mCurrentNUIDocView;}

    private View mDecorView = null;

    //  cough up the view's current session
    public SODocSession getSession() {return mSession;}

    private void setupBitmaps()
    {
        //  create bitmaps
        createBitmaps();
        setBitmapsInViews();
    }

    private void createBitmaps()
    {
        //  get current screen size
        Point displaySize = Utilities.getRealScreenSize(activity());

        //  make two bitmaps.
        //  make them large enough for both screen orientations, so we don't have to
        //  change them when the orientation changes.
        //  This relates to the fact that we can't use Bitmap.reconfigure() in ArDkBitmap due
        //  to our minimum API.

        int screenSize = Math.max(displaySize.x, displaySize.y);
        int bitmapSize = screenSize*(100+OVERSIZE_PERCENT)/100;
        OVERSIZE_MARGIN = (bitmapSize-screenSize)/2;

        for (int i=0; i<bitmaps.length; i++)
            bitmaps[i] = mDocumentLib.createBitmap(bitmapSize, bitmapSize);
    }

    public NUIDocView(Context context)
    {
        super(context);
        initialize(context);
    }

    public NUIDocView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public NUIDocView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(context);
    }

    // called right after this class is instantiated.
    public void setDocSpecifics(ConfigOptions cfgOpts)
    {
        mDocCfgOptions    = cfgOpts;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        //  call the super
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void initialize(Context context)
    {
        // Top-level window decor view.
        mDecorView = ((Activity)getContext()).getWindow().getDecorView();

        // Initialise the utilities class
        //FileUtils.init(context);
    }

    protected int getLayoutId()
    {
        return 0;
    }

    protected void disableUI()
    {
    }

    private void startUI()
    {
        mStarted = false;

        //  inflate the UI
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final ViewGroup view = (ViewGroup) inflater.inflate(getLayoutId(), null);

        final ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                NUIDocView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (!mStarted)
                {
                    enforceInitialShowUI(view);
                    afterFirstLayoutComplete();

                    mStarted = true;
                }
            }
        });

        addView(view);
    }

    protected void enforceInitialShowUI(View view)
    {
        boolean bShow = mDocCfgOptions.showUI();
        mFullscreen = !bShow;
    }

    private void initClipboardHandler()
    {
        String errorBase =
            "initClipboardHandler() experienced unexpected exception [%s]";

        /*try
        {
            //  find a registered handler
            SOClipboardHandler handler = ArDkLib.getClipboardHandler();
            if (handler==null)
                throw new ClassNotFoundException();

            handler.initClipboardHandler(activity());
        }
        catch (ExceptionInInitializerError e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                             "ExceptionInInitializerError"));
        }
        catch (LinkageError e)
        {
            Log.e(mDebugTag, String.format(errorBase, "LinkageError"));
        }
        catch (SecurityException e)
        {
            Log.e(mDebugTag, String.format(errorBase,
                                           "SecurityException"));
        }
        catch (ClassNotFoundException e)
        {
            Log.i(mDebugTag, "initClipboardHandler implementation unavailable");
        }*/
    }

    //  get the user path for the doc used in this view
    public String getDocFileExtension()
    {
        String ext="pdf";

        /*if (mState != null)
            ext = FileUtils.getExtension(mState.getUserPath());
        else if (mSession != null)
            ext = FileUtils.getExtension(mSession.getUserPath());
        else
            ext = FileUtils.getFileTypeExtension(getContext(), mStartUri);*/

        return ext;
    }

    //  start given a path
    public void start(final Uri uri, boolean template, ViewingState viewingState, String customDocData, NUIView.OnDoneListener listener, boolean showUI)
    {
        mIsTemplate = template;
        mStartUri = uri;
        String ext = FileUtils.getFileTypeExtension(getContext(), uri);
        mDocumentLib = MuPDFLib.getLib(activity());//ArDkUtils.getLibraryForPath(activity(), "filename."+ext);
        mViewingState = viewingState;
        if (viewingState!=null)
            setStartPage(viewingState.pageNumber);

        mShowUI = showUI;
        startUI();
        initClipboardHandler();
    }

    //  start given a session
    public void start(SODocSession session, ViewingState viewingState, String foreignData, NUIView.OnDoneListener listener)
    {
        mIsSession = true;
        mSession = session;
        mIsTemplate = false;
        mViewingState = viewingState;
        if (viewingState!=null)
            setStartPage(viewingState.pageNumber);
        mForeignData = foreignData;
        mDocumentLib = MuPDFLib.getLib(activity());//ArDkUtils.getLibraryForPath(activity(), session.getUserPath());

        startUI();
        initClipboardHandler();
    }

    //  start given a saved auto-open state
    public void start(String path, ViewingState viewingState, NUIView.OnDoneListener listener)
    {
        mIsSession = false;
        mViewingState = viewingState;
        if (viewingState!=null)
            setStartPage(viewingState.pageNumber);
        mDocumentLib = MuPDFLib.getLib(activity());//ArDkUtils.getLibraryForPath(activity(), path);

        startUI();
        initClipboardHandler();
    }

    protected boolean hasSearch() {return true;}
    protected boolean hasUndo() {return true;}
    protected boolean hasRedo() {return true;}

    protected boolean canSelect() {return true;}
    protected boolean hasReflow() {return false;}

//    //  edit toolbar object
//    protected NUIEditToolbar mNuiEditToolbar;

    protected void afterFirstLayoutComplete()
    {
        mFinished = false;

        /*
         * this is done here, and in ExplorerActivity to support docs being
         * sent to us.
         */
        if (mDocCfgOptions.usePersistentFileState())
        {
            //SOFileDatabase.init(activity());
        }

        /*
         * Initialise the custom save button only if the required resource is
         * available.
         */
        int customSaveId =
            getContext().getResources().getIdentifier(
                                                   "custom_save_button",
                                                   "id",
                                                   getContext().getPackageName());

        if (customSaveId != 0)
        {
        }

        //Continue setup
        onDeviceSizeChange();

        // Hide/Show buttons as per the current configration.
        setConfigurableButtons();

        //  make the adapter and view
        mAdapter = createAdapter();
        mDocView = createMainView(activity());
        mDocView.setHost(this);
        mDocView.setAdapter(mAdapter);
        mDocView.setDocSpecifics(mDocCfgOptions);

        if (usePagesView())
        {
        }

        //  add the view to the layout
        //  and set up its drag handles
        RelativeLayout layout = findViewById(R.id.doc_inner_container);
        layout.addView(mDocView,0);
        mDocView.setup(layout);

        if (usePagesView())
        {
            //  pages container
        }

        if (mDocCfgOptions.usePersistentFileState())
        {
            //  create a file database
            //mFileDatabase = SOFileDatabase.getDatabase();
        }

        //  final copy of us
        final Activity thisActivity = activity();
        final NUIDocView ndv = this;

        //  watch for a possible orientation change
        final ViewTreeObserver observer3 = getViewTreeObserver();
        observer3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                //  this layout listener stays active forever, so don't use it
                //  after we've finished.
                //  mDocView.finished() is added to not access a destroyed document in the
                //  subsequent onPossibleOrientationChange() call.
                if (mFinished || mDocView.finished()) {
                    observer3.removeOnGlobalLayoutListener(this);
                    return;
                }

                onPossibleOrientationChange();
            }
        });

        //  wait for the initial layout before loading the doc
        ViewTreeObserver observer = mDocView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDocView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                setupBitmaps();

                if (mIsSession)
                {
                    //  if the session was created earlier,
                    //  a load error may have happened before we arrive here.
                    //  so we should disable the UI.
                    if (mSession.hasLoadError())
                        disableUI();

                    mDocUserPath = mSession.getUserPath();

                    if (! mDocCfgOptions.usePersistentFileState())
                    {
                        // We do not expect this entry point when use of the
                        // file state database is disabled.
                        throw new UnsupportedOperationException();
                    }

                    //  make a state for this file and open it.
                    //  This will cause an internal copy to be created
                    //mFileState = mFileDatabase.stateForPath(mDocUserPath, mIsTemplate);

                    //mFileState.setForeignData(mForeignData);

                    //mSession.setFileState(mFileState);
                    //mFileState.openFile(mIsTemplate);

                    //  when opened from preview, assume we're starting with no changes.
                    //mFileState.setHasChanges(false);

                    //  give the open doc to the adapter and views
                    mDocView.setDoc(mSession.getDoc());

                    //  if we have an optional document listener,
                    //  set a password Runnable in the session.
                    if (mDocumentListener!=null) {
                        mSession.setPasswordHandler(new Runnable() {
                            @Override
                            public void run() {
                                mDocumentListener.onPasswordRequired();
                            }
                        });
                    }

                    mSession.addLoadListener(new SODocSession.SODocSessionLoadListener() {
                        @Override
                        public void onPageLoad(int pageNum) {
                            //  next page has loaded.
                            onPageLoaded(pageNum);
                        }

                        @Override
                        public void onDocComplete() {
                            //  all pages have loaded
                            onDocCompleted();
                            setPageNumberText();
                        }

                        @Override
                        public void onError(int error, int errorNum) {
                            if (mSession.isOpen()) {
                                disableUI();
                                //  There was an error, show a message
                                //  but don't show 'aborted error message' if doc loading was cancelled by user.
                                /*if (!mSession.isCancelled() || error!= ArDkLib.SmartOfficeDocErrorType_Aborted) {
                                    String message = Utilities.getOpenErrorDescription(getContext(), error);
                                    Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_error), message);
                                }*/
                            }
                        }

                        @Override
                        public void onCancel()
                        {
                            //  loading was cancelled
                        }

                        @Override
                        public void onSelectionChanged(final int startPage, final int endPage)
                        {
                            onSelectionMonitor(startPage, endPage);
                        }

                        @Override
                        public void onLayoutCompleted()
                        {
                            onLayoutChanged();
                        }
                    });

                    if (usePagesView())
                    {
                    }

                }
//                else if (mState!=null)
//                {
//                    if (! mDocCfgOptions.usePersistentFileState())
//                    {
//                        // We do not expect this entry point when use of the
//                        // file state database is disabled.
//                        throw new UnsupportedOperationException();
//                    }
//
//                    //  this is the auto-open case
//
//                    //  get the path from the state
//                    mDocUserPath = mState.getOpenedPath();
//
//                    //  open the given state (don't make a new one)
//                    mFileState = mState;
//                    mFileState.openFile(mIsTemplate);
//
//                    //  create a session
//                    mSession = new SODocSession(thisActivity, mDocumentLib);
//                    mSession.setFileState(mFileState);
//
//                    //  if we have an optional document listener,
//                    //  set a password Runnable in the session.
//                    if (mDocumentListener!=null) {
//                        mSession.setPasswordHandler(new Runnable() {
//                            @Override
//                            public void run() {
//                                mDocumentListener.onPasswordRequired();
//                            }
//                        });
//                    }
//
//                    //  establish file loading listener
//                    mSession.addLoadListener(new SODocSession.SODocSessionLoadListener() {
//                        @Override
//                        public void onPageLoad(int pageNum) {
//                            if(mFinished)
//                                return;
//                            //  next page has loaded.
//                            onPageLoaded(pageNum);
//                        }
//
//                        @Override
//                        public void onDocComplete() {
//                            if(mFinished)
//                                return;
//                            //  all pages have loaded
//                            onDocCompleted();
//                        }
//
//                        @Override
//                        public void onError(int error, int errorNum) {
//                            if(mFinished)
//                                return;
//                            disableUI();
//                            //  There was an error, show a message
//                            //  but don't show 'aborted error message' if doc loading was cancelled by user.
//                            /*if (!mSession.isCancelled() || error!= ArDkLib.SmartOfficeDocErrorType_Aborted) {
//                                String message = Utilities.getOpenErrorDescription(getContext(), error);
//                                Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_error), message);
//                            }*/
//                        }
//
//                        @Override
//                        public void onCancel()
//                        {
//                            //  loading was cancelled
//                        }
//
//                        @Override
//                        public void onSelectionChanged(final int startPage, final int endPage)
//                        {
//                            onSelectionMonitor(startPage, endPage);
//                        }
//
//                        @Override
//                        public void onLayoutCompleted()
//                        {
//                            onLayoutChanged();
//                        }
//                    });
//
//                    //  ... and we open the state's internal file.
//                    mSession.open(mFileState.getInternalPath(), mDocCfgOptions);
//
//                    //  give the open doc to the adapter and views
//                    mDocView.setDoc(mSession.getDoc());
//                    mAdapter.setDoc(mSession.getDoc());
//                }
                else
                {
                    //  export the content to a temp file
                    Uri    uri    = mStartUri;
                    String scheme = uri.getScheme();

                    if (scheme != null &&
                        scheme.equalsIgnoreCase(ContentResolver.SCHEME_CONTENT))
                    {
                        //  make a tmp file from the content
                        String path =
                            FileUtils.exportContentUri(getContext(), uri);

                        if (path.equals("---fileOpen"))
                        {
                            //  special case indicating that the file could not be opened.
                            /*Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_content_error),
                                    getContext().getString(R.string.sodk_editor_error_opening_from_other_app));*/
                            return;
                        }

                        if (path.startsWith("---"))
                        {
                            /*
                             * if the path starts with "---", that
                             * indicates that we should display
                             * an exception message.
                             */
                            /*String message = getResources().getString(R.string.sodk_editor_cant_create_temp_file);
                            Utilities.showMessage(thisActivity, getContext().getString(R.string.sodk_editor_content_error),
                                    getContext().getString(R.string.sodk_editor_error_opening_from_other_app) + ": \n\n" + message);*/

                            return;
                        }

                        mDocUserPath = path;

                        if (mIsTemplate)
                            addDeleteOnClose(path);
                    }
                    else
                    {
                        mDocUserPath = uri.getPath();

                        if (mDocUserPath == null)
                        {
                            /*Utilities.showMessage(
                                thisActivity,
                                getContext().getString(R.string.sodk_editor_invalid_file_name),
                                getContext().getString(R.string.sodk_editor_error_opening_from_other_app));

                            Log.e(mDebugTag,
                                  " Uri has no path: " + uri.toString());*/
                            return;
                        }
                    }

                    if (mDocCfgOptions.usePersistentFileState())
                    {
                        /*
                         * make a state for this file and open it.
                         * This will cause an internal copy to be created
                         */
                        //mFileState = mFileDatabase.stateForPath(mDocUserPath, mIsTemplate);
                    }
                    else
                    {
                        /*
                         * Use a dummy file state class that overrides operations
                         * we want to avoid.
                         */
                        //mFileState = new SOFileStateDummy(mDocUserPath);
                    }
                    //mFileState.openFile(mIsTemplate);

                    //  we are the target of a Open In or Share of some sort.
                    //  assume we have no changes.
                    //mFileState.setHasChanges(false);

                    //  ... and we open that file instead.
                    mSession = new SODocSession(thisActivity, mDocumentLib);
                    //mSession.setFileState(mFileState);

                    //  if we have an optional document listener,
                    //  set a password Runnable in the session.
                    if (mDocumentListener!=null) {
                        mSession.setPasswordHandler(new Runnable() {
                            @Override
                            public void run() {
                                mDocumentListener.onPasswordRequired();
                            }
                        });
                    }

                    mSession.addLoadListener(new SODocSession.SODocSessionLoadListener() {
                        @Override
                        public void onPageLoad(int pageNum) {
                            if(mFinished)
                                return;
                            //  next page has loaded.
                            onPageLoaded(pageNum);
                        }

                        @Override
                        public void onDocComplete() {
                            if(mFinished)
                                return;
                            //  all pages have loaded
                            onDocCompleted();
                        }

                        @Override
                        public void onError(int error, int errorNum) {
                            if(mFinished)
                                return;
                            disableUI();
                            //  There was an error, show a message
                            //  but don't show 'aborted error message' if doc loading was cancelled by user.
                            if (!mSession.isCancelled() || error!= ArDkLib.SmartOfficeDocErrorType_Aborted) {
                                String message = Utilities.getOpenErrorDescription(getContext(), error);
                            }
                        }

                        @Override
                        public void onCancel()
                        {
                            //  loading was cancelled
                            disableUI();
                        }

                        @Override
                        public void onSelectionChanged(final int startPage, final int endPage)
                        {
                            onSelectionMonitor(startPage, endPage);
                        }

                        @Override
                        public void onLayoutCompleted()
                        {
                            onLayoutChanged();
                        }
                    });
                    mSession.open(mDocUserPath, mDocCfgOptions);

                    //  give the open doc to the adapter and views
                    mDocView.setDoc(mSession.getDoc());
                    mAdapter.setDoc(mSession.getDoc());
                }
            }
        });

        //  tell the main document view about the viewing state
        getDocView().setViewingState(mViewingState);
    }

    private static final int ORIENTATION_PORTAIT = 1;
    private static final int ORIENTATION_LANDSCAPE = 2;
    private int mLastOrientation = 0;

    //  a way to trigger an orientation change
    private boolean mForceOrientationChange = false;
    public void triggerOrientationChange() {mForceOrientationChange=true;}

    private void onPossibleOrientationChange()
    {
        //  get current orientation
        Point p = Utilities.getRealScreenSize(activity());
        int orientation = ORIENTATION_PORTAIT;
        if (p.x>p.y)
            orientation = ORIENTATION_LANDSCAPE;

        //  see if it's changed, or if we've been triggered
        if ((mForceOrientationChange) || (orientation != mLastOrientation && mLastOrientation!=0))
            onOrientationChange();
        mForceOrientationChange = false;

        mLastOrientation = orientation;
    }

    protected void onOrientationChange()
    {
        mDocView.onOrientationChange();

        if (!isFullScreen())
            showUI(false);

        onDeviceSizeChange();
    }

    public void onConfigurationChange(Configuration newConfig)
    {
        //  config was changed, probably orientation.
        //  call the doc view
        if (mDocView!=null)
            mDocView.onConfigurationChange();
    }

    protected DocView createMainView(Activity activity)
    {
        return new DocView(activity);
    }

    protected boolean usePagesView() {return true;}

    //  called from the main DocView whenever the pages list needs to be updated.
    public void setCurrentPage(int pageNumber)
    {
        if (usePagesView())
        {
            //  set the new current page and scroll there.
        }

        //  keep track of the "current" page number
        mCurrentPageNum = pageNumber;

        //  and change the text in the lower right.
        setPageNumberText();

        //  save the current page number in the state
        //mSession.getFileState().setPageNumber(mCurrentPageNum);
    }

    protected void setPageNumberText()
    {
        //  do this in a handler; we may be called during onLayout.
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //  send  notification
                doUpdateCustomUI();
                if (mChangePageListener!=null)
                    mChangePageListener.onPage(mCurrentPageNum);
            }
        });
    }

    protected String getPageNumberText()
    {
        return String.format(getContext().getString(R.string.sodk_editor_page_d_of_d), mCurrentPageNum+1, getPageCount());
    }

    protected PageAdapter createAdapter()
    {
        return new PageAdapter(getContext(), this, PageAdapter.DOC_KIND);
    }

    protected void onDocCompleted()
    {
        //  not if we're finished
        if (mFinished)
            return;

        //  we may be called when pages have been added or removed
        //  so update the page count and re-layout
        mPageCount = mSession.getDoc().getNumPages();
        mAdapter.setCount(mPageCount);
        layoutNow();

        //  use the optional document listener
        if (mDocumentListener!=null)
            mDocumentListener.onDocCompleted();
    }

    protected int getPageCount() {return mPageCount;}

    protected void setPageCount(int newCount)
    {
        mPageCount = newCount;
        mAdapter.setCount(newCount);
        requestLayouts();
    }

    private void requestLayouts()
    {
        mDocView.requestLayout();
    }

    public void layoutNow()
    {
        //  cause the main and page list views to re-layout
        if (mDocView!=null)
            mDocView.layoutNow();
    }

    protected void layoutAfterPageLoad()
    {
        layoutNow();
    }

    protected void onPageLoaded(int pagesLoaded)
    {
        //  to see if this is the first call
        boolean first = (mPageCount==0);

        mPageCount = pagesLoaded;
        if (first)
        {
        }

        int oldCount = mAdapter.getCount();
        boolean pagesChanged = (mPageCount != oldCount);
        if (mPageCount<oldCount)
        {
            //  if the number of pages goes down, then remove all the DocPageViews.
            //  The correct number of them will be re-inserted at the next layout.
            //  this fixes 698220 - Two identical editing lines appear on two pages
            mDocView.removeAllViewsInLayout();
        }
        mAdapter.setCount(mPageCount);

        if (pagesChanged)
        {
            final ViewTreeObserver observer = getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);
                    if (mFinished)
                        return;

                    if (mDocView.getReflowMode())
                        System.out.println("");//onReflowScale();
                    else
                        System.out.println("");//mDocView.scrollSelectionIntoView();
                }
            });
            layoutAfterPageLoad();

            //  use the optional document listener
            if (mDocumentListener!=null)
                mDocumentListener.onPageLoaded(mPageCount);
        }
        else
            requestLayouts();

        handleStartPage();

        //  update the page count text in the lower right no more that once/sec.
        if (!mIsWaiting)
        {
            mIsWaiting = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setPageNumberText();
                    mIsWaiting = false;
                }
            }, 1000);
        }
    }

    private boolean mIsWaiting = false;

    protected void handleStartPage()
    {
        //  if we've been passed the start page, go there
        int start = getStartPage();
        if (start>=0 && getPageCount()>start)
        {
            setStartPage(-1);  //  but just once

            mDocView.setStartPage(start);
            mCurrentPageNum = start;
            setPageNumberText();

            if (mViewingState !=null)
            {
                //  show the page list and tab if requested
                if (mViewingState.pageListVisible) {
                    //  show the page list at the starting page
                    showPages(start);
                }

                //  give the doc view the viewing state
                //  these will get used in DocView.handleStartPage().
                getDocView().setScale(mViewingState.scale);
                getDocView().forceLayout();
            }
        }
    }

    protected void onDeviceSizeChange()
    {
    }

    public DocView getDocView() {return mDocView;}

    public ArDkDoc getDoc() {
        if (mSession==null)
            return null;
        return mSession.getDoc();
    }

    public boolean isPageListVisible()
    {
        /*RelativeLayout pages = findViewById(R.id.pages_container);
        if (null != pages)
        {
            int vis = pages.getVisibility();
            if (vis == View.VISIBLE)
                return true;
        }*/

        return false;
    }

    protected void showPages()
    {
        showPages(-1);
    }

    protected void showPages(final int pageNumber)
    {
        RelativeLayout pages = findViewById(R.id.pages_container);
        if (null != pages)
        {
            int vis = pages.getVisibility();
            if (vis != View.VISIBLE)
            {
                pages.setVisibility(View.VISIBLE);
                mDocView.onShowPages();
            }

            final ViewTreeObserver observer = mDocView.getViewTreeObserver();
            observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    observer.removeOnGlobalLayoutListener(this);

                    //  starting page will either be from the main DocView (most visible)
                    //  or given as a parameter
                    int page;
                    if (pageNumber==-1)
                        page = mDocView.getMostVisiblePage();
                    else
                        page = pageNumber;

                    //  send update UI notification
                    doUpdateCustomUI();
                }
            });
        }
    }

    protected void hidePages()
    {
        RelativeLayout pages = (RelativeLayout) findViewById(R.id.pages_container);
        if (null != pages)
        {
            int vis = pages.getVisibility();
            if (vis != View.GONE) {
                mDocView.onHidePages();
                pages.setVisibility(View.GONE);

                //  send update UI notification
                doUpdateCustomUI();
            }
        }
    }

    protected int getTargetPageNumber()
    {
        DocPageView dpv1 = getDocView().findPageContainingSelection();
        if (dpv1 != null)
            return dpv1.getPageNumber();

        //  get the document view's rect
        Rect r = new Rect();
        getDocView().getGlobalVisibleRect(r);

        //  find the page (including margins) under the center of the view
        DocPageView dpv = getDocView().findPageViewContainingPoint((r.left+r.right)/2, (r.top+r.bottom)/2, true);

        //  get the page number
        int pageNum = 0;
        if (dpv!=null)
            pageNum = dpv.getPageNumber();

        return pageNum;
    }

    protected boolean mFinished = false;

    public void prefinish()
    {
        //  notional fix for https://bugs.ghostscript.com/show_bug.cgi?id=699651
        if (mFinished)
            return;

        //  tell ourselves we're closing.
        mFinished  = true;

        //  tell the file database we're closed.
        /*if (null != mFileState) {
            closeFile();
        }*/

        //  hide the keyboard
        Utilities.hideKeyboard(getContext());

        //  tell doc views to finish
        //  they will in turn tell their pages to do the same.
        if (mDocView!=null)
        {
            mDocView.finish();
            mDocView = null;
        }

        if (usePagesView())
        {
        }

        //  stop any loading
        if (mSession!=null)
            mSession.abort();

        //  destroy the doc's tracked SOPage objects
        ArDkDoc theDoc = getDoc();
        if (theDoc!=null)
            theDoc.destroyPages();

        //  get rid of the page adapter
        if (mAdapter!=null)
            mAdapter.setDoc(null);
        mAdapter = null;

        if (mEndSessionSilent != null)
        {
            endDocSession(mEndSessionSilent.booleanValue());
            mEndSessionSilent = null;
        }

        //  attempt to reclaim memory.
        if (mDocumentLib!=null)
            mDocumentLib.reclaimMemory();
    }

    public void endDocSession(boolean silent)
    {
        /*
         * Finish the doc and pageList views to abort any
         * active page renders.
         *
         * This allows endSession to complete in a timely
         * manner.
         */
        if (mDocView!=null)
        {
            mDocView.finish();
        }

        if (usePagesView())
        {
        }

        if (mSession!=null)
            mSession.endSession(silent);
    }

    protected boolean shouldConfigureSaveAsPDFButton()
    {
        return true;
    }

    public void setConfigurableButtons()
    {
    }

    private void recycleBitmaps()
    {
        //  tell doc and pagelist views to dereference
        if (mDocView != null)
            mDocView.releaseBitmaps();

        for (int i=0; i<bitmaps.length; i++) {
            if (bitmaps[i]!=null) {
                bitmaps[i].getBitmap().recycle();
                bitmaps[i] = null;
            }
        }
    }

    public void onDestroy()
    {
        recycleBitmaps();

        //  delete temp files
        if (mDeleteOnClose!=null) {
            for (int i = 0; i < mDeleteOnClose.size(); i++) {
                FileUtils.deleteFile(mDeleteOnClose.get(i));
            }
            mDeleteOnClose.clear();
        }

        // Use the custom data leakage handlers if available.
    }

    public void onBackPressed()
    {
        goBack();
    }

    protected void prepareToGoBack()
    {
    }

    protected void goBack()
    {
        mEndSessionSilent = new Boolean(false);
        prefinish();
    }

    private void saveState()
    {
        DocView docView = getDocView();
        if (docView != null)
        {
            int pageNumber = mCurrentPageNum;
            float scale = docView.getScale();
            int scrollX = docView.getScrollX();
            int scrollY = docView.getScrollY();
            boolean pageListVisible = usePagesView() && docView.pagesShowing();

            //  set the values in the file state, for apps that are using
            //  persistent file state.
            if (mSession != null)
            {
                /*SOFileState state = mSession.getFileState();
                if ( state != null )
                {
                    state.setPageNumber( pageNumber );
                    state.setScale( scale );
                    state.setScrollX( scrollX );
                    state.setScrollY( scrollY );
                    state.setPageListVisible( pageListVisible );
                }*/
            }

            //  set the values in the ViewingState, for apps that are NOT using
            //  persistent file state.
            if ( mViewingState != null )
            {
                mViewingState.pageNumber = pageNumber;
                mViewingState.scale = scale;
                mViewingState.scrollX = scrollX;
                mViewingState.scrollY = scrollY;
                mViewingState.pageListVisible = pageListVisible;
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, final Intent data)
    {
        // Use the custom data leakage handlers if available.
    }

    public void triggerRender()
    {
        if (mDocView!=null)
            mDocView.triggerRender();
    }

    private long lastTypingTime = 0;
    public void onTyping()
    {
        //  record last typing time
        lastTypingTime = System.currentTimeMillis();
    }

    public boolean wasTyping()
    {
        // if typing took place within the last 500 msec, return true;
        long now = System.currentTimeMillis();
        return (now-lastTypingTime) <500;
    }

    public void setIsComposing(boolean isComposing)
    {
        mIsComposing = isComposing;
    }

    public boolean getIsComposing()
    {
        return mIsComposing;
    }

    public void onSelectionMonitor(int startPage, int endPage)
    {
    }

    public void onLayoutChanged()
    {
        //  this is called when a core layout is done.

        // Take care on shutdown.
        if (mSession == null || mSession.getDoc() == null || mFinished)
            return;

        mDocView.onLayoutChanged();
    }

    public void onPause(final Runnable whenDone)
    {
        //  not if we've been finished
        if (mDocView!=null && mDocView.finished()) {
            whenDone.run();
            return;
        }

        //  not if we've not been opened
        /*if (mFileState == null || mDocView == null || mDocView.getDoc()==null) {
            whenDone.run();
            return;
        }*/

        {
            whenDone.run();
        }
    }

    public void releaseBitmaps()
    {
        //  release the bitmaps (making them null)
        setValid(false);
        recycleBitmaps();
        setBitmapsInViews();
    }

    private void setValid(boolean val)
    {
        if (mDocView!=null)
            mDocView.setValid(val);
    }

    private void setBitmapsInViews()
    {
        //  tell the doc and page list views what bitmaps to use.
        //  these might be null.
        if (mDocView!=null)
            mDocView.setBitmaps(bitmaps);
    }

    protected void onResumeCommon()
    {
        //  keep track of the current view
        mCurrentNUIDocView = this;

        if (mDocUserPath != null)
        {
            //  create new bitmaps
            createBitmaps();
            setBitmapsInViews();
        }

        //  If a reload was requested while we were in the background, do it now
        if (mForceReloadAtResume)
        {
            mForceReloadAtResume = false;

            getDoc().setForceReloadAtResume(true);
            reloadFile();
        }
        else if (mForceReload)
        {
            mForceReload = false;

            getDoc().setForceReload(true);
            reloadFile();
        }

        mIsActivityActive = true;

        //  do a new layout so the content is redrawn.
        DocView docView = getDocView();
        if (docView!=null)
            docView.forceLayout();

        //  don't forget the page list (bug 699304)
        if (usePagesView())
        {
        }
    }

    public void onResume()
    {
        onResumeCommon();

        //  did we have changes when we went into the background?
        /*SOFileState state = SOFileState.getAutoOpen(getContext());
        if (state != null && mFileState != null)
        {
            //  The doc may have been saved since we went to the background
            //  because the Save As dialog is really an activity, which will bg us.
            //  so don't adjust the hasChanges value in that case.
            if (state.getLastAccess() > mFileState.getLastAccess())
            {
                mFileState.setHasChanges(state.hasChanges());
            }
        }*/

        //  clear autoOpen
        //SOFileState.clearAutoOpen(getContext());

    }

    public void showUI(final boolean bShow)
    {
        //  If there's a registered handler for exiting full screen, run it
        if (bShow)
        {
            //  set this here in case we were started with no UI
            mFullscreen = false;
        }

        //  if we're started with no UI, keep it that way.
        if (!mShowUI)
            return;

        layoutNow();

        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                afterShowUI(bShow);
            }
        });
        //  NOTHING after this please.
    }

    protected void afterShowUI(boolean bShow)
    {
        if (mDocCfgOptions.isFullscreenEnabled())
        {
            if (bShow)
            {
                mFullscreen = false;
                if (getDocView() != null)
                {
                    getDocView().onFullscreen(false);
                }
                layoutNow();
            }
        }
    }

    public void goToPage(int page)
    {
        goToPage(page, false);
    }

    public void goToPage(int page, boolean fast)
    {
        mDocView.scrollToPage(page, fast);
        if (usePagesView()) {
            //  ask the page list to highlight this as the current page
        }

        //  update the current page
        mCurrentPageNum = page;
        setCurrentPage(page);
    }

    @Override
    public void onClick(View v)
    {
        if (v == null)
            return;

        // Ignore button presses while we are finishing up.
        if (mFinished)
        {
            return;
        }

        //  full screen
        onFullScreen(v);
    }

    public boolean isLandscapePhone()
    {
        return (mLastOrientation == ORIENTATION_LANDSCAPE);
    }

    public boolean canCanManipulatePages() {return false;}

    private void pageUp()
    {
        DocView dv = getDocView();
        int scrollDistance = dv.getHeight()*9/10;
        dv.smoothScrollBy(0, scrollDistance, 400);
    }

    private void pageDown()
    {
        DocView dv = getDocView();
        int scrollDistance = -dv.getHeight()*9/10;
        dv.smoothScrollBy(0, scrollDistance, 400);
    }

    private void lineUp()
    {
        DocView dv = getDocView();
        int scrollDistance = dv.getHeight()*1/20;
        dv.smoothScrollBy(0, scrollDistance, 100);
    }

    private void lineDown()
    {
        DocView dv = getDocView();
        int scrollDistance = -dv.getHeight()*1/20;
        dv.smoothScrollBy(0, scrollDistance, 100);
    }

    public void reloadFile()
    {
    }

    private boolean mForceReload = false;
    public void forceReload()
    {
        mForceReload = true;
    }

    //  this is used if we save while pausing.
    //  in that case we defer reloading until we resume
    private boolean mForceReloadAtResume = false;
    public void forceReloadAtResume()
    {
        mForceReloadAtResume = true;
    }

    protected boolean mFullscreen = false;
    public boolean isFullScreen()
    {
        /*if (!mDocCfgOptions.isFullscreenEnabled())
            return false;*/
        return mFullscreen;
    }

    protected void onFullScreenHide()
    {
        hidePages();
        layoutNow();
    }

    protected void onFullScreen(View v)
    {
        //  not after we're done
        if (mFinished)
            return;
        if (getDocView()==null)
            return;

        //  no need
        if (isFullScreen())
            return;

        mFullscreen = true;

        //  hide keyboard
        Utilities.hideKeyboard(getContext());

        //  tell the document view
        getDocView().onFullscreen(true);

        //  hide everything
        onFullScreenHide();
    }

    //  optional listener for document events.
    private DocumentListener mDocumentListener = null;

    public void setDocumentListener(DocumentListener listener)
    {
        mDocumentListener = listener;
    }

    public void gotoInternalLocation(int page, RectF box)
    {
        //  Add a history entry for the spot we're leaving.
        DocView dv = getDocView();
        dv.scrollBoxToTop(page, box);
    }

    //  for no-UI API
    public void firstPage()
    {
        gotoPage(0);
    }

    //  for no-UI API
    public void lastPage()
    {
        gotoPage(getPageCount()-1);
    }

    //  for no-UI API
    private void gotoPage(int pageNumber)
    {
        mDocView.scrollToPage(pageNumber, true);
        setCurrentPage(pageNumber);
    }

    public int getPageNumber()
    {
        return mCurrentPageNum+1;
    }

    //  for no-UI API
    public void tableOfContents()
    {
    }

    //  for no-UI API
    public boolean isTOCEnabled()
    {
        return false;
    }

    //  optional Runnable for notifying no-UI apps to update their UI
    private Runnable mOnUpdateUIRunnable = null;
    public void setOnUpdateUI(Runnable runnable)
    {
        mOnUpdateUIRunnable = runnable;
    }

    protected void doUpdateCustomUI()
    {
        if (mFinished)
            return;

        if (mOnUpdateUIRunnable!=null)
            mOnUpdateUIRunnable.run();
    }

    public interface ChangePageListener {
        void onPage(int pageNumber);
    }
    private ChangePageListener mChangePageListener = null;
    public void setPageChangeListener(ChangePageListener listener) {mChangePageListener=listener;}

    //  this may be called by DocView to update the UI buttons
    public void updateUI()
    {
    }

    //  for no-UI API
    public int getScrollPositionX()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionX();
        return -1;
    }

    //  for no-UI API
    public int getScrollPositionY()
    {
        if (mDocView != null)
            return mDocView.getScrollPositionY();
        return -1;
    }

    //  for no-UI API
    public float getScaleFactor()
    {
        if (mDocView != null)
            return mDocView.getScaleFactor();
        return -1;
    }

    //  for no-UI API
    public void setScaleAndScroll (float newScale, int newX, int newY)
    {
        if (mDocView != null)
            mDocView.setScaleAndScroll(newScale, newX, newY);
    }

    //  keep track of changes in these values with each call to reportViewChanges
    private float scalePrev = 0;
    private int scrollXPrev = -1;
    private int scrollYPrev = -1;
    private Rect selectionPrev = null;

    //  the following function is called when scrolling, scaling, or selection changes.
    //  it does some filtering to weed out repetitions, and then calls the no-UI
    //  DocumentListener, if there is one.

    public void reportViewChanges()
    {
        //  don't bother if there is no listener
        if (mDocumentListener==null)
            return;

        if (mDocView == null)
            return;

        //  get current scale, scroll values
        float scale = mDocView.getScale();
        int scrollX = mDocView.getScrollX();
        int scrollY = mDocView.getScrollY();

        //  get the selection rect, if any
        Rect selectionRect = null;

        //  see if anything's changed since the last call
        boolean changed = false;
        if (scale!=scalePrev)
            changed = true;
        if (scrollX!=scrollXPrev)
            changed = true;
        if (scrollY!=scrollYPrev)
            changed = true;
        if (!Utilities.compareRects(selectionPrev, selectionRect))
            changed = true;
        if (!changed)
            return;  //  nothing changed

        //  save new values for next time
        scalePrev = scale;
        scrollXPrev = scrollX;
        scrollYPrev = scrollY;
        selectionPrev = selectionRect;

        //  call the listener
        mDocumentListener.onViewChanged(scale, scrollX, scrollY, selectionRect);
    }
}
