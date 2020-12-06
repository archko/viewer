package com.artifex.sonui.editor;

import android.app.Activity;
import android.content.Intent;

import java.io.IOException;
import java.lang.UnsupportedOperationException;

import com.artifex.solib.ArDkDoc;
import com.artifex.solib.ConfigOptions;

/**
 * This interface specifies the basis for implementing a class allowing
 * proprietary handling of application buttons actioning data save/export
 * from the application.
 */

public interface SODataLeakHandlers
{
    /**
     * This method initialises the DataLeakHandlers object <br><br>.
     *
     * @param activity     The current activity.
     * @param cfgOpts      The Config Options to reference.
     *
     * @throws IOException if the temporary folder cannot be created.
     */
    public void initDataLeakHandlers(Activity activity, ConfigOptions cfgOpts)
        throws IOException;

    /**
     * This method finalizes the DataLeakHandlers object <br><br>.
     */
    public void finaliseDataLeakHandlers();

    /**
     * This method will be called when a launched activity exits.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify
     *                    who this result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 final Intent data);

    /**
     * This method will be called when the application is backgrounded.<br><br>
     *
     * The implementor may save the document to a temporary location to allow
     * restoration on a future run.
     *
     * @param doc               The document object.
     * @param sModificationsdoc True if the document has unsaved modifications.
     * @param whenDone          run this when everything is done. Can be null.
     */
    public void pauseHandler(ArDkDoc doc, boolean hasModifications, Runnable whenDone);

    /**
     * This method will be called when an external link has been activated.
     * <br><br>
     *
     * The implementor should launch an application to display the link target.
     *
     * @param url The Url to be launched.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void launchUrlHandler(String url)
        throws UnsupportedOperationException;

    /**
     * This method will be called when the application "SavePdfAs" button is
     * pressed.<br><br>
     *
     * The implementor should allow the user to select the file save location,
     * then execute the export.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void saveAsPdfHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;

    /**
     * This method will be called when the application "OpenIn" button is
     * pressed.<br><br>
     *
     * The implementor should save the document to a suitable location
     * suitable for opening in a partner application..
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void openInHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;

    /**
     * This method will be called when the application "OpenPdfIn" button is
     * pressed.<br><br>
     *
     * The implementor should export the document to a suitable location
     * suitable for opening in a partner application.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void openPdfInHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;

    /**
     * This method will be called when the application "Share" button is
     * pressed.<br><br>
     *
     * The implementor should save the document to a suitable location
     * suitable for sharing in a partner application.
     *
     * @param filename The name of the file being edited.
     * @param doc      The document object.
     *
     * @throws UnsupportedOperationException if the handler is not implemented.
     *         In this case the button will be omitted from the user interface.
     */
    public void shareHandler(String filename, ArDkDoc doc)
        throws UnsupportedOperationException;
}
