package com.artifex.sonui.editor;

import android.app.Activity;

import com.artifex.solib.ConfigOptions;
import com.artifex.solib.ArDkLib;
import com.artifex.solib.ArDkDoc;
import com.artifex.solib.SODocLoadListener;
import com.artifex.solib.ArDkPage;
import com.artifex.solib.ArDkRender;

import java.util.concurrent.CopyOnWriteArrayList;

public class SODocSession {

    private ArDkDoc mDoc;
    public ArDkDoc getDoc() {return mDoc;}

    private String mUserPath = null;
    public String getUserPath() {return mUserPath;}

    private boolean mOpen = false;
    private boolean mCancelled = false;
    private boolean mLoadError = false;

    private final ArDkLib mLibrary;

    private final Activity mActivity;

    public SODocSession(Activity activity, ArDkLib library)
    {
        mActivity = activity;
        mLibrary = library;
    }

    public interface SODocSessionLoadListener
    {
        void onPageLoad(int pageNum);
        void onDocComplete();
        void onError(int error, int errorNum);
        void onCancel();
        void onSelectionChanged(int startPage, int endPage);
        void onLayoutCompleted();
    }

    public interface SODocSessionLoadListenerCustom extends
        SODocSessionLoadListener
    {
        void onSessionReject();
        void onSessionComplete(boolean silent);
    }

    private int mPageCount = 0;
    private boolean mCompleted = false;

    //  an array of load listeners, managed by
    //  addLoadListener() and removeLoadListener()
    private CopyOnWriteArrayList<SODocSessionLoadListener> mListeners = new CopyOnWriteArrayList<>();

    //  add a new load listener
    public void addLoadListener(final SODocSessionLoadListener listener) {

        if (listener!=null)
        {
            //  add it to the list
            mListeners.add(listener);

            if (mPageCount>0)
                listener.onPageLoad(mPageCount);
            if (mCompleted)
                listener.onDocComplete();
        }

        mListenerCustom = Utilities.getSessionLoadListener();

        if (mListenerCustom!=null)
        {
            if (mPageCount>0)
                mListenerCustom.onPageLoad(mPageCount);
            if (mCompleted)
                mListenerCustom.onDocComplete();
        }
    }

    //  remove a load listener
    public void removeLoadListener(final SODocSessionLoadListener listener) {
        mListeners.remove(listener);
    }

    private void clearListeners()
    {
        mListeners.clear();
    }

    private SODocSessionLoadListenerCustom mListenerCustom=null;

    //  an optional Runnable to use when a document password is required.
    private Runnable mPasswordHandler = null;
    public void setPasswordHandler(Runnable r) {mPasswordHandler=r;}

    public void open(String path, ConfigOptions cfg)
    {
        mUserPath = path;

        mPageCount = 0;
        mCompleted = false;

        mOpen = true;
        final SODocSession session = this;
        mDoc = mLibrary.openDocument(path, new SODocLoadListener() {
            @Override
            public void onPageLoad(int pageNum) {
                //  we might arrive here after the doc loading has been aborted
                if (!mOpen || mCancelled)
                    return;

                mPageCount = Math.max(pageNum, mPageCount);

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onPageLoad(pageNum);
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onPageLoad(pageNum);
                }
            }

            @Override
            public void onDocComplete() {
                //  we might arrive here after the doc loading has been aborted
                if (!mOpen || mCancelled || mLoadError)
                    return;

                mCompleted = true;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onDocComplete();
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onDocComplete();
                }
            }

            @Override
            public void onError(final int error, final int errorNum)
            {
                if (error == ArDkLib.SmartOfficeDocErrorType_PasswordRequest)
                {
                    //  if an optional password runnable has been specified, use it
                    //  instead of handling it below.
                    if (mPasswordHandler!=null)
                    {
                        mPasswordHandler.run();
                        return;
                    }

                    return;
                }

                mLoadError = true;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onError(error, errorNum);
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onError(error, errorNum);
                }

                mOpen = false;
            }

            @Override
            public void onSelectionChanged(final int startPage, final int endPage)
            {
                //  we might arrive here after the doc loading has been aborted
                if (!mOpen)
                    return;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onSelectionChanged(startPage, endPage);
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onSelectionChanged(startPage, endPage);
                }
            }

            public void onLayoutCompleted()
            {
                //  we might arrive here after a core layout is done
                if (!mOpen)
                    return;

                if (mOpen) {
                    for (SODocSessionLoadListener listener : mListeners) {
                        listener.onLayoutCompleted();
                    }
                    if (mListenerCustom!=null)
                        mListenerCustom.onLayoutCompleted();
                }
            }
        }, mActivity, cfg);
    }

    public void abort()
    {
        //  dismiss current alert.  This could be a message, or perhaps a password dialog.
        Utilities.dismissCurrentAlert();

        mOpen = false;
        clearListeners();
        mListenerCustom = null;

        //  stop loading
        if (mDoc != null)
            mDoc.abortLoad();

        //  destroy first-page render and page
        //  this must come before destroying the doc.
        cleanup();
    }

    public void destroy()
    {
        abort();

        //  destroy the doc
        if (mDoc != null) {
            mDoc.destroyDoc();
            mDoc = null;
        }
    }

    public void endSession(boolean silent)
    {
        // End any current document session
        if (mListenerCustom != null)
        {
            mListenerCustom.onSessionComplete(silent);
            mListenerCustom = null;
        }

        abort();
    }

    public boolean isOpen() {return mOpen;}
    public boolean isCancelled() {return mCancelled;}

    //  these objects are used to render a thumbnail of the first page\
    private ArDkRender mRender = null;
    private ArDkPage mPage = null;

    private void cleanup()
    {
        if (mRender!=null)
            mRender.destroy();
        mRender = null;

        if (mPage !=null)
            mPage.releasePage();
        mPage = null;
    }

    public boolean hasLoadError()
    {
        return mLoadError;
    }
}
