package com.artifex.sonui.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import androidx.annotation.ColorRes;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.webkit.MimeTypeMap;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.artifex.solib.FileUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Utilities
{
    private static final String mDebugTag = "Utilities";
    private static String       mFileStateForPrint = null;

    public static void showMessage(final Activity activity, final String title, final String body)
    {
        //showMessage(activity, title, body, activity.getResources().getString(R.string.sodk_editor_ok));
    }

    //  This keeps track of the last-displayed alert dialog
    //  shown by one of the methods in this class.
    //  An activity that's showing one of these can dismiss it by
    //  calling dismissCurrentAlert()
    //
    //  this is designed to address http://bugs.ghostscript.com/show_bug.cgi?id=698650
    //  where NUIActivity.finish() is called while the alert is showing.
    //  NUIActivity.finish() now calls dismissCurrentAlert().
    //
    //  While we're at it, we'll make sure there's only one alert at a time showing,
    //  by removing one that may be showing before displaying another.

    private static AlertDialog currentMessageDialog = null;

    //  for no-UI
    public interface MessageHandler {
        void showMessage(String title, String body, String okLabel, Runnable whenDone);

        void yesNoMessage(String title, String body,
                          String yesButtonLabel, String noButtonLabel,
                          Runnable yesRunnable, Runnable noRunnable);
    }
    private static MessageHandler mMessageHandler = null;
    public static void setMessageHandler(MessageHandler handler){mMessageHandler=handler;}

    public static void showMessage(final Activity activity, final String title, final String body, final String okLabel)
    {
        //  for no-UI
        if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.showMessage(title, body, okLabel, null);
                }
            });
            return;
        }

        /*activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissCurrentAlert();
                currentMessageDialog =
                new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style)
                        .setTitle(title)
                        .setMessage(body)
                        .setCancelable(false)
                        .setPositiveButton(okLabel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                currentMessageDialog = null;
                            }
                        }).create();
                currentMessageDialog.show();
            }
        });*/
    }

    public static void dismissCurrentAlert()
    {
        if (currentMessageDialog != null)
        {
            try {
                currentMessageDialog.dismiss();
            }
            catch (Exception e) {
            }

            currentMessageDialog = null;
        }
    }

    public static void showMessageAndFinish(final Activity activity, final String title, final String body)
    {
        //  for no-UI
        /*if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.showMessage(title, body, activity.getResources().getString(R.string.sodk_editor_ok), new Runnable() {
                        @Override
                        public void run() {
                            activity.finish();
                        }
                    });
                }
            });
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissCurrentAlert();
                currentMessageDialog = new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style)
                        .setTitle(title)
                        .setMessage(body)
                        .setCancelable(false)
                        .setPositiveButton(R.string.sodk_editor_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                currentMessageDialog = null;
                                activity.finish();
                            }
                        }).create();
                currentMessageDialog.show();
            }
        });*/
    }

    public static void showMessageAndWait(final Activity activity, final String title, final String body, final int styleId, final Runnable runnable)
    {
        //  for no-UI
        /*if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.showMessage(title, body, activity.getResources().getString(R.string.sodk_editor_ok), runnable);
                }
            });
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissCurrentAlert();
                currentMessageDialog = new AlertDialog.Builder(activity, styleId)
                        .setTitle(title)
                        .setMessage(body)
                        .setCancelable(false)
                        .setPositiveButton(R.string.sodk_editor_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                currentMessageDialog = null;
                                runnable.run();
                            }
                        }).create();
                currentMessageDialog.show();
            }
        });*/
    }

    public static void showMessageAndWait(final Activity activity, final String title, final String body, final Runnable runnable)
    {
        //showMessageAndWait(activity, title, body, R.style.sodk_editor_alert_dialog_style, runnable);
    }

    public static void yesNoMessage(final Activity activity, final String title, final String body,
                                    final String yesButtonLabel, final String noButtonLabel,
                                    final Runnable yesRunnable, final Runnable noRunnable)
    {
        //  for no-UI
        if (mMessageHandler!=null)
        {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMessageHandler.yesNoMessage(title, body, yesButtonLabel, noButtonLabel, yesRunnable, noRunnable);
                }
            });
            return;
        }

        /*activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                dismissCurrentAlert();

                AlertDialog.Builder dialog = new AlertDialog.Builder(activity, R.style.sodk_editor_alert_dialog_style);

                dialog.setTitle(title);
                dialog.setMessage(body);

                dialog.setCancelable(false);

                dialog.setPositiveButton(yesButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        currentMessageDialog = null;
                        if (yesRunnable!=null)
                            yesRunnable.run();
                    }
                });

                dialog.setNegativeButton(noButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        currentMessageDialog = null;
                        if (noRunnable!=null)
                            noRunnable.run();
                    }
                });

                currentMessageDialog = dialog.create();
                currentMessageDialog.show();
            }
        });*/
    }

    private static String getMimeTypeFromExtension (String path)
    {
        String mime = null;
        String ext = FileUtils.getExtension(path);

        if (ext.compareToIgnoreCase("xps")==0)
            mime = "application/vnd.ms-xpsdocument";
        if (ext.compareToIgnoreCase("cbz")==0)
            mime = "application/x-cbz";
        if (ext.compareToIgnoreCase("svg")==0)
            mime = "image/svg+xml";

        return mime;
    }

    public static String getMimeType (String path)
    {
        String ext = FileUtils.getExtension(path);
        String mime = null;

        if (ext != null) {
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }

        if (mime==null) {
            mime = getMimeTypeFromExtension(path);
        }

        return mime;
    }

    public interface SigningFactoryListener
    {
        NUIPKCS7Signer getSigner(Activity context);
        NUIPKCS7Verifier getVerifier(Activity context);
    }

    public static SigningFactoryListener mSigningFactory = null;

    public static void setSigningFactoryListener (SigningFactoryListener factory)
    {
        mSigningFactory = factory;
    }

    public static NUIPKCS7Signer getSigner( Activity context )
    {
        if (mSigningFactory != null)
            return mSigningFactory.getSigner( context );
        else
            return null;
    }

    public static NUIPKCS7Verifier getVerifier( Activity context )
    {
        if (mSigningFactory != null)
            return mSigningFactory.getVerifier( context );
        else
            return null;
    }


    public static final String generalStore = "general";

    public static boolean isLandscapePhone(Context context)
    {
        return false;
    }

    public static int convertDpToPixel(float dp)
    {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int)Math.round(px);
    }

    //  this function returns the size of the screen as the app sees it.
    //  In split screen mode, this is *smaller* than the real screen size.
    public static Point getScreenSize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        return new Point(metrics.widthPixels, metrics.heightPixels);
    }

    //  this function returns the *actual* size of the screen.
    public static Point getRealScreenSize(Activity activity)
    {
        WindowManager wm  = activity.getWindowManager();
        return getRealScreenSize(wm);
    }

    //  this function returns the *actual* size of the screen.
    public static Point getRealScreenSize(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return getRealScreenSize(wm);
    }

    //  this function returns the *actual* size of the screen.
    private static Point getRealScreenSize(WindowManager wm)
    {
        Display display = wm.getDefaultDisplay();

        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17)
        {
            //  new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        }
        else if (Build.VERSION.SDK_INT >= 14)
        {
            //  reflection for this weird in-between time
            try
            {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            }
            catch (Exception e)
            {
                //this may not be 100% accurate, but it's all we've got
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("sonui", "Couldn't use reflection to get the real display metrics.");
            }
        }
        else
        {
            //this may not be 100% accurate, but it's all we've got
            realWidth = display.getWidth();
            realHeight = display.getHeight();
            Log.e("sonui", "Can't get real display matrix.");
        }

        return new Point(realWidth, realHeight);
    }

    public static void hideKeyboard(Context context)
    {
        if (Activity.class.isInstance(context))
        {
            Activity activity = (Activity)context;
            //  fix https://bugs.ghostscript.com/show_bug.cgi?id=702763
            //  get the window token from the activity content
            IBinder windowToken = activity.findViewById(android.R.id.content).getRootView().getWindowToken();
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(windowToken, 0);
        }
    }

    public static void hideKeyboard(Context context, View view)
    {
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(Context context)
    {
        //  show keyboard
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public static void setFileStateForPrint(String filename)
    {
        mFileStateForPrint = filename;
    }

    public static String getFileStateForPrint()
    {
        return mFileStateForPrint;
    }

    public static File getRootDirectory(Context context)
    {
        return Environment.getExternalStorageDirectory();
    }

    public static File getDownloadDirectory(Context context)
    {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    private static String getHtmlColorStringFromResource(@ColorRes int resourceId, Context context)
    {
        int htmlHexCodeLen = 6; /* HTML color hex codes do not include the alpha channel */
        int colorInt = context.getResources().getColor(resourceId);
        int colorStringLen;

        /* The hex string function doesn't pad the integer, but most resources have an alpha
        channel above 00 to be visible, so should be 7 or 8 digits */
        String colorString = Integer.toHexString(colorInt);
        colorStringLen = colorString.length();

        if (colorStringLen > htmlHexCodeLen)
        {
            colorString = colorString.substring(colorStringLen - htmlHexCodeLen);
            colorString = "#" + colorString;
        }
        else
        {
            colorString = "rgba(0, 0, 0, 0);"; /* Transparent */
        }

        return colorString;
    }

    public static String getApplicationName(Context context)
    {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId                    = applicationInfo.labelRes;

        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() :
                               context.getString(stringId);
    }

    private static SODocSession.SODocSessionLoadListenerCustom
                                                mSessionLoadListener = null;

    public static void setSessionLoadListener(
                           SODocSession.SODocSessionLoadListenerCustom listener)
    {
        mSessionLoadListener = listener;
    }

    public static SODocSession.SODocSessionLoadListenerCustom
                                                        getSessionLoadListener()
    {
        return mSessionLoadListener;
    }

    private static final Set<String> RTL;

    static
    {
        Set<String> lang = new HashSet<String>();
        lang.add("ar"); // Arabic
        lang.add("dv"); // Divehi
        lang.add("fa"); // Persian (Farsi)
        lang.add("ha"); // Hausa
        lang.add("he"); // Hebrew
        lang.add("iw"); // Hebrew (old code)
        lang.add("ji"); // Yiddish (old code)
        lang.add("ps"); // Pashto, Pushto
        lang.add("ur"); // Urdu
        lang.add("yi"); // Yiddish
        RTL = Collections.unmodifiableSet(lang);
    }

    public static boolean isRTL(Context context)
    {
        InputMethodManager inputMethodManager = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        InputMethodSubtype inputMethodSubtype = inputMethodManager.getCurrentInputMethodSubtype();

        // Handle the case whereby the IMM does not have a subtype.
        if (inputMethodSubtype == null)
        {
            return false;
        }

        String tag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)  //  API 24
            tag = inputMethodSubtype.getLanguageTag();
        else
            tag = inputMethodSubtype.getLocale();

        boolean result = RTL.contains(tag);
        return result;
    }

    public static int[] screenToWindow(int[] screenLoc, Context context)
    {
        //  this function converts a screen location to one relative
        //  to the window for the given context.
        //  if we're in split screen mode, these can be different.

        //  where is our window on screen?
        int decorLoc[] = new int[2];
        ((Activity)context).getWindow().getDecorView().getLocationOnScreen(decorLoc);

        //  convert to window-relative
        int newLoc[] = new int[2];
        newLoc[0] = screenLoc[0] - decorLoc[0];
        newLoc[1] = screenLoc[1] - decorLoc[1];

        return newLoc;
    }

    public static String getOpenErrorDescription(Context context, int errcode)
    {
        String desc="error";
        return desc;
    }

    public static boolean isChromebook(Context context)
    {
        //  detect if we're running on a Chromebook.
        //  this comes from
        //      https://stackoverflow.com/questions/39784415/how-to-detect-programmatically-if-android-app-is-running-in-chrome-book-or-in

        return context.getPackageManager().hasSystemFeature("org.chromium.arc.device_management");
    }

    public static String formatFloat(float value)
    {
        //  This function formats a floating point number using the
        //  smallest number of decimal places necessary. If that's 0,
        //  then the decimal point is dropped as well.
        //
        //  String.format is used so the result is in the default locale.

        if (value % 1 == 0) {
            //  it's a whole number
            return String.format("%.0f", value);
        }

        //  count the decimal places and format with the
        //  appropriate format specifier.
        String text = Double.toString(Math.abs((double)value));  //  creates, for example, "1.23"
        int integerPlaces = text.indexOf('.');
        int decimalPlaces = text.length() - integerPlaces - 1;
        String fmt = "%." + decimalPlaces + "f";
        return String.format(fmt, value);
    }

    //
    //  this function reformats a date according to the current locale.
    //
    public static String formatDateForLocale(Context context, String inDateStr, String inFormat)
    {
        try
        {
            //  parse the incoming string using the given format
            SimpleDateFormat df = new SimpleDateFormat(inFormat);
            Date date = df.parse(inDateStr);

            //  reformat the date with the system's date format, which will be locale-specific
            java.text.DateFormat dateFormat;
            String format = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
            if (TextUtils.isEmpty(format))
                dateFormat = DateFormat.getDateFormat(context);
            else
                dateFormat = new SimpleDateFormat(format);
            String result = dateFormat.format(date);

            //  add the time, also locale-specific
            dateFormat = DateFormat.getTimeFormat(context);
            result += " " + dateFormat.format(date);

            //  return the reformatted date
            return result;
        }
        catch (Exception e)
        {
        }

        //  if we got here, there was some error, so just return the original string
        return inDateStr;
    }

    public static boolean isEmulator()
    {
        //  see:  https://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator

        if ("goldfish".equals(Build.HARDWARE))
            return true;
        if ("ranchu".equals(Build.HARDWARE))
            return true;

        if (Build.FINGERPRINT.contains("generic"))
            return true;

        return false;
    }

    // ISO8601 date time string to Date
    public static Date iso8601ToDate(String iso8601)
    {
        String str = iso8601.replace("[Zz]", "+00:00");
        try
        {
            str = str.substring(0, 22) + str.substring(23); // remove ':'
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(str);
        }
        catch (IndexOutOfBoundsException e)
        {
            e.printStackTrace();
            return null;
        }
        catch (ParseException  e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //  this function retutns the width of the widest child View
    //  in the given ListView
    public static int getListViewWidth(ListView listView) {

        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return 0;
        }

        int maxWidth = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            maxWidth = Math.max(maxWidth, listItem.getMeasuredWidth());
        }

        return maxWidth;
    }

    public static boolean isPermissionRequested(Context context, String permission)
    {
        //  this function check the list of requested permissions
        //  (those that appear in uses-permission items in the manifest)
        //  for the given permission and returns true if found.

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String p : info.requestedPermissions) {
                    if (p.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //  compare two rects for equality
    //  returns true if they are the same
    public static boolean compareRects(Rect r1, Rect r2)
    {
        if (r1==null) {
            if (r2==null) {
                //  both are null
                return true;
            }
            else {
                //  one is null, the other not
                return false;
            }
        }
        else {
            if (r2==null) {
                //  one is null, the other not
                return false;
            }
            else {
                //  both are not null, so compare
                return r1.equals(r2);
            }
        }
    }

}
