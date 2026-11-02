package na.eraka.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import na.eraka.R;

public class CustomButton extends ConstraintLayout {
    private ImageView icono;
    private TextView textView;
    public CustomButton(Context context) {
        super(context);

    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ConstraintLayout constraintLayout=(ConstraintLayout) inflater.inflate(R.layout.custom_imagebutton,this);
        icono=constraintLayout.findViewById(R.id.icono);
        textView=constraintLayout.findViewById(R.id.nombre);
    }

    public CustomButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public void setIcono(ImageView icono) {
        this.icono = icono;
    }
    public void setTextView(){

    }

    public void setTexto(String texto){
        if(textView!=null)
        textView.setText(texto);
    }
    public void setImageView(Context c,int drawable){
        icono.setImageDrawable(c.getResources().getDrawable(drawable));
    }
}
