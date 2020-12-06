package com.artifex.sonui.editor;

public interface DocViewHost
{
    void setCurrentPage(int pageNumber);
    DocView getDocView();
    void prefinish();
    void layoutNow();
    void updateUI();
    void reportViewChanges();
}
