package na.eraka.utils.Handlers;

import android.os.Message;

import na.eraka.MainBoardActivity;
import na.eraka.SettingsActivity;

public class MainBoardHandler extends HandlerUtils {
    private SettingsActivity preferences;
    private MainBoardActivity principal;


    public static final int CARGARJSON=2;
    public static final int ERRORCARGARJSON=3;

    public static final int SHAREACTION = 7;

    // Result of a Whisper transcription that's been through NLG cleanup.
    // Unlike TRANSLATEDPHRASE, this does NOT auto-speak — it routes to a
    // confirmation step first, since the source is a possibly-misheard
    // transcription rather than a deliberately-built pictogram sequence.
    public static final int WHISPERPHRASE = 8;

    private  int fallasLeerJson=0;
    public MainBoardHandler(SettingsActivity preferences){
        this.preferences=preferences;
    }
    public MainBoardHandler(MainBoardActivity principal){
        this.principal=principal;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case SHOWDIALOG:
                if(preferences!=null){
                    preferences.mostrarDialogo();
                }
                break;
            case DISMISSDIALOG:
                if(preferences!=null){
                    preferences.cancelarDialogo();
                }
                break;
            case CARGARJSON :
                if(principal!=null)
                    principal.CargarJson();
                break;
            case ERRORCARGARJSON:
                if(principal!=null){
                    fallasLeerJson++;
                    if(fallasLeerJson==1)
                        principal.CargarJson();

                }
                break;
            case TRANSLATEDPHRASE:
                if (principal != null) {
                    principal.setOracion((String) msg.obj);
                    principal.speak();
                }
                break;
            case SHAREACTION:
                principal.setOracion((String) msg.obj);
                principal.shareText();
                break;
            case WHISPERPHRASE:
                if (principal != null) {
                    principal.confirmWhisperPhrase((String) msg.obj);
                }
                break;
            case TEXTNOTTRANSLATED:
                if (principal != null) {
                    principal.mostrarMensajeSinConexionInternet();
                }
                break;
            case CHATGPTERROR:{
                if(principal!=null)
                    principal.chatGptError();
            }
            break;
            case TRYAGAIN:
                if (principal != null) {
                    principal.intentarDeNuevo();
                }
                break;
            default:
                super.handleMessage(msg);
        }
    }

    public void setFallasLeerJson(int fallasLeerJson){
        this.fallasLeerJson=fallasLeerJson;
    }
}