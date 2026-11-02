package na.eraka.Dialogos;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import na.eraka.utils.CustomButton;

public abstract class BaseDialog {
    /*
    *This is an abstract class that bring the diferent methods  of the dialog constructor
    * */
    public abstract void setDialog(Context mContext);
    public abstract void setDialog(Context mContext,int layout);
    public abstract BaseDialog changeVisibilityView(int v, int visibility);
    public abstract BaseDialog changeIcon(CustomButton button, int drawable);
    public abstract BaseDialog setTitle(String title);
    public abstract BaseDialog setMessage(String message);
    public abstract BaseDialog setTextButton(int v,String text);
    public abstract BaseDialog setLayout(int layout);
    public abstract BaseDialog settingButton( ProgressBar v, int layout);
    public abstract BaseDialog settingButton(CustomButton v, int layout);
    public abstract BaseDialog settingButton(TextView v, int layout);
    public abstract BaseDialog setOnClick(View v,View.OnClickListener clickListener);
    public abstract BaseDialog setCancelable(boolean isCancelable);
    public abstract View getObject(int view);

}
