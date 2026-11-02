package na.eraka.Interfaces;

public  interface DialogOptionsListener {
    void showLanguajeDialog(String name, int value, int options);
    void showDialogOptionsDownloadFile(String name, int value, int options, String preferences);
    void showDialogOptionsSettings(String name, int value, int options, String preferences);
    void showDialogOptionsSkilledHand(String name, int value, int options, String preferences);
    void showDialogOptionsSkilledHand(String name, boolean value, int values, int options, String preferences);
    void showDialogOptionsDownloadFile(String name, String key, String value, int values, int options, String preference);


}
