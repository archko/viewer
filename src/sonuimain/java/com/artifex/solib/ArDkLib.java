package com.artifex.solib;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;

public abstract class ArDkLib
{
    protected static final String mDebugTag = "ArDkLib";

    //  possible document loading errors
    public static final int SmartOfficeDocErrorType_NoError = 0;
    public static final int SmartOfficeDocErrorType_UnsupportedDocumentType = 1;
    public static final int SmartOfficeDocErrorType_EmptyDocument = 2;
    public static final int SmartOfficeDocErrorType_UnableToLoadDocument = 4;
    public static final int SmartOfficeDocErrorType_UnsupportedEncryption = 5;
    public static final int SmartOfficeDocErrorType_Aborted = 6;
    public static final int SmartOfficeDocErrorType_OutOfMemory = 7;

    /** A password is required to open this document.
     *
     * The app should provide it using SmartOffice_providePassword
     * or if it doesn't want to proceed call SmartOfficeDoc_destroy or
     * SmartOfficeDoc_abortLoad.
     */
    public static final int SmartOfficeDocErrorType_PasswordRequest = 0x1000;

    //  subclasses will implement this
    public abstract ArDkDoc openDocument(String path, SODocLoadListener listener, Context context, ConfigOptions cfg);

    public interface EnumeratePdfTocListener {
        void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y);
    }

    public static void enumeratePdfToc(final ArDkDoc doc, final EnumeratePdfTocListener listener)
    {
        ((MuPDFDoc)doc).enumerateToc(new MuPDFDoc.MuPDFEnumerateTocListener() {
            @Override
            public void nextTocEntry(int handle, int parentHandle, int page, String label, String url, float x, float y)
            {
                listener.nextTocEntry(handle, parentHandle, page, label, url, x, y);
            }
        });
    }

    private static SOSecureFS mSecureFS = null;
    public static void setSecureFS(SOSecureFS fs) {mSecureFS=fs;}
    public static SOSecureFS getSecureFS() {return mSecureFS;}

    private static ConfigOptions mAppConfigOptions = null;
    public static void setAppConfigOptions(ConfigOptions co) {mAppConfigOptions=co;}
    public static ConfigOptions getAppConfigOptions()
    {
        if (mAppConfigOptions == null)
        {
            throw new RuntimeException("No registered ConfigOptions found.");
        }
        return mAppConfigOptions;
    }

    public abstract ArDkBitmap createBitmap(int w, int h);

    public void reclaimMemory()
    {
    }

    public static boolean isNightMode(Context context)
    {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
    }

    /**
     * Function to delay an action until it can be run on the UiThread
     *
     * @param action   Action to run on the UiThread
     */
    protected static final void runOnUiThread(Runnable action)
    {
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        if (mainHandler!=null)
            mainHandler.post(action);
    }
}
