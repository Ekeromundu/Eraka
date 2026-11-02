package na.eraka;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.recyclerview.widget.RecyclerView;

import na.eraka.Adapters.TagsAdapter;
import na.eraka.Interfaces.DialogInterfaceTags;
import na.eraka.Interfaces.TagJsonListener;
import na.eraka.JSONutils.Json;
import na.eraka.Prediction.AgeGroup;
import na.eraka.Prediction.TimeOfDay;
import na.eraka.Prediction.Position;
import na.eraka.Prediction.Gender;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.JSONutils;
import na.eraka.utils.exceptions.FiveMbException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
// Todo modularizar esta clase en partes
public class TagAssigner {

    private static final String TAG = "AsignTags";
    private Json json;
    private JSONObject picto;
    private boolean esGrupo = false;
    private final boolean esGrupo2 = false;
    private Context mContext;
    private TagsAdapter adapter;
    private String pictoGrupoJson;
    private ArrayList<JSONObject> mArrayListSelectedTAGS;
    private ArrayList<JSONObject> mArrayListTagsPorTipo;
    private DialogInterfaceTags mDialogInterface;
    private TagJsonListener mTagInterface;
    private ArrayList<JSONObject> arrayListTodosLosTags;
    private ArrayList<JSONObject> noselectedTags;

    public TagAssigner() {
    }

    public TagAssigner(Context mContext) {
        this.mContext = mContext;

        Json.getInstance().setmContext(mContext);
        this.json = Json.getInstance();

        this.mArrayListSelectedTAGS = new ArrayList<>();
        this.mArrayListTagsPorTipo = new ArrayList<>();
        this.arrayListTodosLosTags = new ArrayList<>();

        String tags = "[{\"id\":379,\"texto\":{\"en\":\"Morning\",\"es\":\"" + TimeOfDay.MANANA + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"ic_buenos_dias\"}}," +
                "{\"id\":818,\"texto\":{\"en\":\"Noon\",\"es\":\"" + TimeOfDay.MEDIODIA + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"verano\"}}," +
                "{\"id\":380,\"texto\":{\"en\":\"Afternoon\",\"es\":\"" + TimeOfDay.TARDE + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"ic_buenas_tardes\"}}," +
                "{\"id\":381,\"texto\":{\"en\":\"Night\",\"es\":\"" + TimeOfDay.NOCHE + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"ic_buenas_noches\"}}," +
                "{\"id\":614,\"texto\":{\"en\":\"Grandpa\",\"es\":\"" + AgeGroup.ADULTO + "\"},\"tipo\":\"" + Constants.EDAD + "\",\"imagen\":{\"picto\":\"ic_abuelo\"}}," +
                "{\"id\":195,\"texto\":{\"en\":\"Young\",\"es\":\"" + AgeGroup.JOVEN + "\"},\"tipo\":\"" + Constants.EDAD + "\",\"imagen\":{\"picto\":\"joven\"}}," +
                "{\"id\":630,\"texto\":{\"en\":\"Boy\",\"es\":\"" + AgeGroup.NINO + "\"},\"tipo\":\"" + Constants.EDAD + "\",\"imagen\":{\"picto\":\"nino\"}}," +
                "{\"id\":651,\"texto\":{\"en\":\"Field\",\"es\":\"" + Position.ESTADIO + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"cancha\"}}," +
                "{\"id\":653,\"texto\":{\"en\":\"Square\",\"es\":\"" + Position.PARQUE + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"plaza\"}}," +
                "{\"id\":1013,\"texto\":{\"en\":\"Movie theater\",\"es\":\"" + Position.CINE + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"cine\"}}," +
                "{\"id\":654,\"texto\":{\"en\":\"Computer store\",\"es\":\"" + Position.TIENDA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"tienda_de_informatica\"}}," +
                "{\"id\":687,\"texto\":{\"en\":\"Bar\",\"es\":\"" + Position.BAR + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"bar\"}}," +
                "{\"id\":496,\"texto\":{\"en\":\"Coffee\",\"es\":\"" + Position.CAFE + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"cafe\"}}," +
                "{\"id\":685,\"texto\":{\"en\":\"Restaurant\",\"es\":\"" + Position.RESTAURANT + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"restaurante\"}}," +
                "{\"id\":672,\"texto\":{\"en\":\"Bakery\",\"es\":\"" + Position.PANADERIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"pan_blanco\"}}," +
                "{\"id\":652,\"texto\":{\"en\":\"Butchery\",\"es\":\"" + Position.CARNICERIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"carniceria\"}}," +
                "{\"id\":698,\"texto\":{\"en\":\"Greengrocery\",\"es\":\"" + Position.VERDULERIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"verduleria\"}}," +
                "{\"id\":655,\"texto\":{\"en\":\"Farmacy\",\"es\":\"" + Position.FARMACIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"ic_farmacia\"}}," +
                "{\"id\":668,\"texto\":{\"en\":\"Hospital\",\"es\":\"" + Position.HOSPITAL + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"hospital\"}}," +
                "{\"id\":662,\"texto\":{\"en\":\"School\",\"es\":\"" + Position.ESCUELA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"ic_escuela\"}}," +
                "{\"id\":611,\"texto\":{\"en\":\"Transportation\",\"es\":\"" + Position.ESTACIONDEBUS + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"transportes\"}}," +
                "{\"id\":1033,\"texto\":{\"en\":\"Woman\",\"es\":\"" + Gender.FEMENINO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"mujer\"}}," +
                 "{\"id\":1035,\"texto\":{\"en\":\"Man\",\"es\":\"" + Gender.MASCULINO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}," +
                 "{\"id\":1036,\"texto\":{\"en\":\"Binary\",\"es\":\"" + Gender.BINARIO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}," +
                 "{\"id\":1037,\"texto\":{\"en\":\"Fluid\",\"es\":\"" + Gender.FLUIDO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}," +
                "{\"id\":1038,\"texto\":{\"en\":\"Others\",\"es\":\"" + Gender.OTRO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}]\n";

        arrayListTodosLosTags = JSONutils.stringToArrayList(tags);


    }


    // Setter para el listener de la clase TagAssigner
    public void setInterfaz(DialogInterfaceTags interfaz) {
        this.mDialogInterface = interfaz;
    }

    public DialogInterfaceTags getmDialogInterface() {
        return mDialogInterface;
    }

    public TagJsonListener getmTagInterface() {
        return mTagInterface;
    }

    public void setInterfazTag(TagJsonListener interfaz) {
        this.mTagInterface = interfaz;
    }

    public void setAdapter(RecyclerView recyclerView) {

        adapter = new TagsAdapter(mArrayListTagsPorTipo, R.layout.grid_item_layout, mArrayListSelectedTAGS, mContext);
        recyclerView.setAdapter(adapter);

    }

    public void setExtras(JSONObject pictoExtra, boolean esGrupoExtra) {
        this.picto = pictoExtra;
        this.esGrupo = esGrupoExtra;

    }

    public JSONObject getPicto() {
        return picto;
    }

    public TagsAdapter getTagsAdapter() {
        return adapter = new TagsAdapter(mArrayListTagsPorTipo, R.layout.grid_item_layout, mArrayListSelectedTAGS, mContext);
    }

    public void getTagsYaAsignados() {
        if (esGrupo) {

            try {
                JSONArray tagDeGrupo = picto.getJSONArray("tags");
                for (int i = 0; i < tagDeGrupo.length(); i++) {
                    //agrego todos los tags guardados en grupo
                    mArrayListSelectedTAGS.add(json.getJsonObjectFromTexto(arrayListTodosLosTags, tagDeGrupo.getString(i)));

                }
                cargarTagsPictos();
            } catch (JSONException e) {
                e.printStackTrace();
            }


        } else {
            cargarTagsPictos();
        }

    }


    public void cargarTags(String tipoTag) {
        try {
            mArrayListTagsPorTipo = json.getArrayListFromTipo(tipoTag, arrayListTodosLosTags);
            getTagsYaAsignados();
        } catch (Exception ex) {

        }
    }

    //Funciona para que cuando apreto el boton ok del dialog asigne a los grupos o pictos los tags
    public void asignarTags(Dialog dialog) {
        if (esGrupo) {
            //Set a todos los hijos del grupo
            ModificarTags modificarTags = new ModificarTags(mContext, false, dialog);
            modificarTags.execute();
        } else {
            //set a un solo picto
            try {
                setTagsToPicto(picto);
                dialog.dismiss();
                //((DialogInterfaceTags) mContext).onTagAsignado(picto.toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }





    public void setTagToGrupo(JSONObject grupo, Context context, boolean esAgenda) {

        ModificarTags modificarTags = new ModificarTags(context, esAgenda);
        modificarTags.execute();
    }

    private void setTagsToGrupo(JSONObject grupo, boolean esAgenda) throws FiveMbException {
        if(grupo!=null&&grupo.has("tags"))
            grupo.remove("tags");
        JSONArray todosLosPictos = null;
        try {
            todosLosPictos = json.readJSONArrayFromFile(Constants.ARCHIVO_PICTOS);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {

                JSONArray array = grupo.getJSONArray("relacion");//relacion de los grupos
                for (int i = 0; i < array.length(); i++) {
                    //Cargo el json que apunta al picto a modificar
                    JSONObject jsonGrupo = array.getJSONObject(i);// picto obtenido
                    Log.d(TAG, "setTagsToGrupo: " + jsonGrupo.getInt("id"));
                    //Modifico el json desde pictos

                    JSONObject jsonPicto = json.getPictoFromId2(jsonGrupo.getInt("id"));
                    if (jsonPicto != null) {
                        setTagsToPictogram(jsonPicto);
                        JSONutils.setJsonEditado2(json.getmJSONArrayTodosLosPictos(), jsonPicto);
                    } else {
                        array.remove(i);
                        int pos = json.getPosPicto(json.getmJSONArrayTodosLosGrupos(), jsonGrupo.getInt("id"));
                        json.getmJSONArrayTodosLosGrupos().getJSONObject(pos).put("relacion", array);
                    }
                }

            if (!json.guardarJson(Constants.ARCHIVO_PICTOS))
                Log.e(TAG, "Error al guardar el json");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void guardarTags() {

        if (mArrayListSelectedTAGS.size() > 0) {
            try {
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < mArrayListSelectedTAGS.size(); i++) {
                    jsonArray.put(mArrayListSelectedTAGS.get(i).getJSONObject("texto").getString("en"));
                    Log.d(TAG, "guardarTags: " + jsonArray);
                }
                picto.put("tags", jsonArray);
                //((TagJsonListener) mContext).refrescarJsonTags(picto);

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "guardarTags: Error");
            }
        }

    }



    public class ModificarTags extends AsyncTask<Void, Void, Boolean> {

        private final String TAG = "LongTask.java";
        private final ProgressDialog p;
        private final boolean esAgenda;
        private Dialog dialog;

        /*
             Constructor
         */
        public ModificarTags(Context context, boolean esAgenda, Dialog dialogDismiss) {
            this.p = new ProgressDialog(context);
            this.esAgenda = esAgenda;
            this.dialog = dialogDismiss;
        }

        public ModificarTags(Context context, boolean esAgenda) {
            this.p = new ProgressDialog(context);
            this.esAgenda = esAgenda;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p.setMessage("Agregando TAGs");
            p.setIndeterminate(false);
            p.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                setTagsToGrupo(picto, esAgenda);
            } catch (FiveMbException e) {
                e.printStackTrace();
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (p.isShowing() && p != null) {
                p.dismiss();
            }
            guardarTags();
            ((DialogInterfaceTags) mContext).onTagAsignado(picto.toString());

            if (dialog.isShowing() && dialog != null) {
                dialog.dismiss();
            }

        }
    }
/*
*The system verify if the tag exist
*The system run into the array an question which is the selected tag and mark them
* */
    private void cargarTagsPictos() {
        try {
            JSONArray pictosTags;
            ArrayList<String> tagNames = new ArrayList<>();
            tagNames.add(Constants.HORA);
            tagNames.add(Constants.EDAD);
            tagNames.add(Constants.SEXO);
            tagNames.add(Constants.UBICACION);
            for (int i = 0; i < tagNames.size(); i++) {
                String tagPorPos = tagNames.get(i);
                if (picto.has(tagPorPos)) {
                    pictosTags = picto.getJSONArray(tagPorPos);
                    for (int j = 0; j < pictosTags.length(); j++) {
                        //agrego todos los tags guardados en grupo
                        mArrayListSelectedTAGS.add(json.getJsonObjectFromTexto(arrayListTodosLosTags, pictosTags.getString(j)));
                        Log.d(TAG, "cargarTagsPictos: " + pictosTags.getString(j));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "cargarTagsPictos: Error" + e.getMessage());
        }

    }


    public void setTagsToPicto(JSONObject picto) throws JSONException {
        if(picto!=null) {
            //Borrar los array de tags
            if (tieneTag(Constants.HORA, picto))
                picto.remove(Constants.HORA);
            if (tieneTag(Constants.EDAD, picto))
                picto.remove(Constants.EDAD);
            if (tieneTag(Constants.UBICACION, picto))
                picto.remove(Constants.UBICACION);
            if (tieneTag(Constants.SEXO, picto))
                picto.remove(Constants.SEXO);
          setTagsToPictogram(picto);
        }

    }

    private TimeOfDay getHorario(int id) {
        switch (id) {
            case 379:
                return TimeOfDay.MANANA;
            case 818:
                return TimeOfDay.MEDIODIA;
            case 380:
                return TimeOfDay.TARDE;
            case 381:
                return TimeOfDay.NOCHE;
        }
        return null;
    }

    private AgeGroup getEdad(int edad) {
        switch (edad) {
            case 614:
                return AgeGroup.ADULTO;
            case 195:
                return AgeGroup.JOVEN;
            case 630:
                return AgeGroup.NINO;
        }
        return null;
    }

    private Gender getSexo(int sexo) {
        switch (sexo) {
            case 1035:
                return Gender.MASCULINO;
            case 1033:
                return Gender.FEMENINO;
            case 1036:
                return Gender.BINARIO;
            case 1037:
                return Gender.FLUIDO;
            case 1038:
                return Gender.BINARIO;
        }
        return null;
    }

    private Position getPosicion(int posicion) {
        switch (posicion) {
            case 668:
                return Position.HOSPITAL;
            case 611:
                return Position.ESTACIONDEBUS;
            case 654:
                return Position.TIENDA;
            case 662:
                return Position.ESCUELA;
            case 685:
                return Position.RESTAURANT;
            case 652:
                return Position.CARNICERIA;
            case 698:
                return Position.VERDULERIA;
            case 1013:
                return Position.CINE;
            case 651:
                return Position.ESTADIO;
            case 496:
                return Position.CAFE;
            case 687:
                return Position.BAR;
            case 653:
                return Position.PARQUE;
            case 655:
                return Position.FARMACIA;
            case 672:
                return Position.PANADERIA;
        }
        return null;
    }

    private boolean tieneTag(String  texto,JSONObject object){
        return object.has(texto);
    }

    public ArrayList<JSONObject> getArrayListTodosLosTags() {
        return arrayListTodosLosTags;
    }

    public void setmArrayListSelectedTAGS(ArrayList<JSONObject> mArrayListSelectedTAGS) {
        this.mArrayListSelectedTAGS = mArrayListSelectedTAGS;
    }

    public ArrayList<JSONObject> getmArrayListSelectedTAGS() {
        return mArrayListSelectedTAGS;
    }

    public ArrayList<JSONObject> getmArrayListTagsPorTipo() {
        return mArrayListTagsPorTipo;
    }

    public void setTagsToPictogram(JSONObject picto) {
        if (picto != null) {
            ArrayList<TimeOfDay> tagHora = new ArrayList<>();
            ArrayList<AgeGroup> tagEdad = new ArrayList<>();
            ArrayList<Gender> tagSexo = new ArrayList<>();
            ArrayList<Position> tagPosicion = new ArrayList<>();
            getTags(tagHora,tagEdad,tagSexo,tagPosicion);
            for (int i = 0; i < tagHora.size(); i++) {
                JSONutils.setHorario(picto, tagHora.get(i));
            }
            for (int i = 0; i < tagEdad.size(); i++) {
                JSONutils.setEdad(picto, tagEdad.get(i));
            }
            for (int i = 0; i < tagSexo.size(); i++) {
                JSONutils.setSexo(picto, tagSexo.get(i));
            }
            for (int i = 0; i < tagPosicion.size(); i++) {
                JSONutils.setPosicion(picto, tagPosicion.get(i));
            }

        }
    }

    private void getTags(ArrayList<TimeOfDay> tagHora,ArrayList<AgeGroup> tagEdad,ArrayList<Gender> tagSexo,ArrayList<Position> tagPosicion){
        for (int i = 0; i < mArrayListSelectedTAGS.size(); i++) {
            try {
                if (getHorario(mArrayListSelectedTAGS.get(i).getInt("id")) != null) {
                    tagHora.add(getHorario(mArrayListSelectedTAGS.get(i).getInt("id")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (getEdad(mArrayListSelectedTAGS.get(i).getInt("id")) != null) {
                    tagEdad.add(getEdad(mArrayListSelectedTAGS.get(i).getInt("id")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (getSexo(mArrayListSelectedTAGS.get(i).getInt("id")) != null) {
                    tagSexo.add(getSexo(mArrayListSelectedTAGS.get(i).getInt("id")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                if (getPosicion(mArrayListSelectedTAGS.get(i).getInt("id")) != null) {
                    tagPosicion.add(getPosicion(mArrayListSelectedTAGS.get(i).getInt("id")));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }




}
