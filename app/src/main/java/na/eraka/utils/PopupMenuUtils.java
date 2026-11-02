package na.eraka.utils;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;

import na.eraka.R;
import na.eraka.utils.preferences.User;
import com.stonefacesoft.pictogramslibrary.utils.ValidateContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PopupMenuUtils {

    private PopupMenu popupMenu;
    private Context mContext;

    public  PopupMenuUtils(Context context,View view){
        this.mContext = context;
        popupMenu = new PopupMenu(mContext,view);
    }

    /** Eraka: id of the "Add to sentence" item added by inflateWithAddToSentence(). */
    public static final int ITEM_ADD_TO_SENTENCE = 0x0E7A1;

    /**
     * Eraka (16 Oct 2026): gallery long-press menu. "Add to sentence" first; the OTTAA
     * edit/delete items only when editing is allowed.
     */
    public void inflateWithAddToSentence(boolean withEditItems){
        if (withEditItems) {
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        }
        popupMenu.getMenu().add(android.view.Menu.NONE, ITEM_ADD_TO_SENTENCE, 0,
                mContext.getString(R.string.eraka_add_to_sentence));
        popupMenu.show();
    }

    public void inflateIt(){
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu,popupMenu.getMenu());
        if (!User.getInstance(mContext).isPremium()) {
            popupMenu.getMenu().getItem(0).setIcon(R.drawable.ic_padlock);
        }
        popupMenu.show();
    }

    public PopupMenuUtils addClickListener(PopupMenu.OnMenuItemClickListener listener){
        popupMenu.setOnMenuItemClickListener(listener);
        return this;
    }

    public PopupMenu getPopupMenu() {
        return popupMenu;
    }

    public void dismissPopupMenu() {
        // If the PopupMenu object is not null, then dismiss it
        if (popupMenu != null) {
            popupMenu.dismiss();
        }
    }
}
