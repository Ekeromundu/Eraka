package na.eraka.JSONutils;


import na.eraka.Prediction.Weather;
import na.eraka.Prediction.AgeGroup;
import na.eraka.Prediction.TimeOfDay;
import na.eraka.Prediction.Position;
import na.eraka.Prediction.Gender;
import na.eraka.utils.JSONutils;
import na.eraka.utils.constants.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;


public class JsonTest {
    private final int size = 10000;

    @Test
    public void getInstance() {
        Json json = Json.getInstance();
        Assert.assertNotNull(json);
    }



    @Test
    public void initJsonArrays() {
    }

    @Test
    public void refreshJsonArrays() {
    }

    @Test
    public void getmJSONArrayTodosLosPictos() {
        JSONObject jsonObject = createPictogramJSONArray();
        JSONArray array = null;
        try {
            array = jsonObject.getJSONArray("array");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Json json = Json.getInstance();
        json.setmJSONArrayTodosLosPictos(array);
        Assert.assertEquals(array.toString(),json.getmJSONArrayTodosLosPictos().toString());
    }

    @Test
    public void setmJSONArrayTodosLosPictos() {
        JSONObject jsonObject = createPictogramJSONArray();
        JSONArray array = null;
        try {
            array = jsonObject.getJSONArray("array");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Json json = Json.getInstance();
        json.setmJSONArrayTodosLosPictos(array);
        int value = array.length();
        int result = json.getmJSONArrayTodosLosPictos().length();
        System.out.println("Expected value:"+value+" Result value:"+result);
        Assert.assertTrue(array.length() == json.getmJSONArrayTodosLosPictos().length());
    }

    @Test
    public void getmJSONArrayPictosSugeridos() {
        Json json = Json.getInstance();
        JSONObject object = createPictogramJSONArray();
        try {
            json.setmJSONArrayPictosSugeridos(object.getJSONArray("array"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(json.getmJSONArrayPictosSugeridos());
    }

    @Test
    public void setmJSONArrayPictosSugeridos() {
        Json json = Json.getInstance();
        JSONObject object = createPictogramJSONArray();
        try {
            json.setmJSONArrayPictosSugeridos(object.getJSONArray("array"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(json.getmJSONArrayPictosSugeridos());
    }

    @Test
    public void getmJSONArrayTodasLasFotosBackup() {
    }

    @Test
    public void setmJSONArrayTodasLasFotosBackup() {
    }

    @Test
    public void getmJSONArrayTodosLosGrupos() {
        JSONArray array = null;
        JSONObject object = createGroupsJSONArray();
        Json json = Json.getInstance();
        try {
            array = createGroupsJSONArray().getJSONArray("array");
            json.setmJSONArrayTodosLosGrupos(array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(array.toString(),json.getmJSONArrayTodosLosGrupos().toString());

    }

    @Test
    public void setmJSONArrayTodosLosGrupos() {
        JSONArray array = null;
        JSONObject object = createGroupsJSONArray();
        Json json = Json.getInstance();
        try {
            array = createGroupsJSONArray().getJSONArray("array");
            json.setmJSONArrayTodosLosGrupos(array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(array.toString(),json.getmJSONArrayTodosLosGrupos().toString());

    }

    @Test
    public void getmJSONArrayTodasLasFrases() {
        Json json =Json.getInstance();
        JSONArray phrases = Phrases();
        json.setmJSONArrayTodasLasFrases(phrases);
        Assert.assertEquals(phrases.toString(),json.getmJSONArrayTodasLasFrases().toString());
    }

    @Test
    public void setmJSONArrayTodasLasFrases() {
        Json json =Json.getInstance();
        JSONArray phrases = Phrases();
        json.setmJSONArrayTodasLasFrases(phrases);
        Assert.assertEquals(phrases,json.getmJSONArrayTodasLasFrases());
    }

    @Test
    public void getmJSonArrayJuegos() {
    }

    @Test
    public void setmJSonArrayJuegos() {
    }

    @Test
    public void initSharedPrefs() {
    }

    @Test
    public void getmArrayListTodasLasFotosBackup() {

    }




    @Test
    public void abrirBitmap() {
    }

    @Test
    public void getUrlBitmap() {
    }

    @Test
    public void testAbrirBitmap() {
    }

    @Test
    public void setAgenda() {
    }

    @Test
    public void getPictoFromId2() {
        Json json = Json.getInstance();
        JSONObject object = createPictogramJSONArray();
        JSONArray array = null;
        int max = 0;
        try {
            array = object.getJSONArray("array");
            json.setmJSONArrayTodosLosPictos(array);
            max = object.getInt("max");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject lastObject = null;
        JSONObject value = null;
        try {
            lastObject = json.getmJSONArrayTodosLosPictos().getJSONObject(json.getmJSONArrayTodosLosPictos().length()-1);
            value= json.getPictoFromId2(max);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(lastObject,value);
    }

    @Test
    public void getHijosGrupo2() {
    }

    @Test
    public void tieneHora() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        Assert.assertTrue(json.tieneTag(object,Constants.HORA));
    }

    @Test
    public void tieneSexo() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        Assert.assertTrue(json.tieneTag(object,Constants.SEXO));
    }
    @Test
    public void tienePosicion() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        Assert.assertTrue(json.tieneTag(object,Constants.UBICACION));
    }
    @Test
    public void tieneEdad() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        Assert.assertTrue(json.tieneTag(object,Constants.EDAD));
    }

    @Test
    public void tieneClima() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        Assert.assertTrue(json.tieneTag(object,Constants.CLIMA));
    }
    @Test
    public void tagEmptyOption() {
        Json json = Json.getInstance();
        JSONObject object = new JSONObject();
        boolean exist = true;
        try {
            object.put("tags",new JSONArray());
            exist = json.tieneTag(object,"testing");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertFalse(exist);
    }
    @Test
    public void tagNullOption() {
        Json json = Json.getInstance();
        JSONObject object = new JSONObject();
        boolean exist = true;
        try {
            object.put("tags",null);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        exist = json.tieneTag(object,"testing");
        Assert.assertFalse(exist);
    }


    @Test
    public void setPlaceName() {
        Json json = Json.getInstance();
        json.setPlaceName("testing");
        Assert.assertEquals("TYPE_testing",json.getCantidadDePlaces());
    }

    @Test
    public void getCantidadDePlaces() {
        Json json = Json.getInstance();
        json.setPlaceName("testing");

    }

    @Test
    public void getRestaurantPosition(){
        Json json = Json.getInstance();
        json.setPlaceName("RESTAURANT");
        Assert.assertTrue(Position.RESTAURANT == json.calcularPosicion());
    }

    @Test
    public void getBakery(){
        Json json = Json.getInstance();
        json.setPlaceName("BAKERY");
        Assert.assertTrue(Position.PANADERIA == json.calcularPosicion());
    }

    @Test
    public void getPark(){
        Json json = Json.getInstance();
        json.setPlaceName("PARK");
        Assert.assertTrue(Position.PARQUE == json.calcularPosicion());
    }
    @Test
    public void getStore(){
        Json json = Json.getInstance();
        json.setPlaceName("STORE");
        Assert.assertTrue(Position.TIENDA == json.calcularPosicion());
    }
    @Test
    public void getShoppingMall(){
        Json json = Json.getInstance();
        json.setPlaceName("SHOPPING_MALL");
        Assert.assertTrue(Position.SHOPPING == json.calcularPosicion());
    }

    @Test
    public void getAirport(){
        Json json = Json.getInstance();
        json.setPlaceName("AIRPORT");
        Assert.assertTrue(Position.AEROPUERTO == json.calcularPosicion());
    }
    @Test
    public void getATM(){
        Json json = Json.getInstance();
        json.setPlaceName("ATM");
        Assert.assertTrue(Position.CAJERO == json.calcularPosicion());
    }
    @Test
    public void getBank(){
        Json json = Json.getInstance();
        json.setPlaceName("BANK");
        Assert.assertTrue(Position.BANCO == json.calcularPosicion());
    }
    @Test
    public void getBar(){
        Json json = Json.getInstance();
        json.setPlaceName("BAR");
        Assert.assertTrue(Position.BAR == json.calcularPosicion());
    }
    @Test
    public void getInsuranceAgency(){
        Json json = Json.getInstance();
        json.setPlaceName("INSURANCE_AGENCY");
        Assert.assertTrue(Position.AGENCIADESEGUROS == json.calcularPosicion());
    }
    @Test
    public void getSubwayStation(){
        Json json = Json.getInstance();
        json.setPlaceName("SUBWAY_STATION");
        Assert.assertTrue(Position.SUBTE == json.calcularPosicion());
    }
    @Test
    public void getJewerlyStore(){
        Json json = Json.getInstance();
        json.setPlaceName("JEWELRY_STORE");
        Assert.assertTrue(Position.JOLLERIA == json.calcularPosicion());
    }
    @Test
    public void getBusStation(){
        Json json = Json.getInstance();
        json.setPlaceName("BUS_STATION");
        Assert.assertTrue(Position.ESTACIONDEBUS == json.calcularPosicion());
    }

    @Test
    public void getTaxiStand(){
        Json json = Json.getInstance();
        json.setPlaceName("TAXI_STAND");
        Assert.assertTrue(Position.PARADATAXI == json.calcularPosicion());
    }
    @Test
    public void getStadium(){
        Json json = Json.getInstance();
        json.setPlaceName("STADIUM");
        Assert.assertTrue(Position.ESTADIO == json.calcularPosicion());
    }
    @Test
    public void getHospital(){
        Json json = Json.getInstance();
        json.setPlaceName("HOSPITAL");
        Assert.assertTrue(Position.HOSPITAL == json.calcularPosicion());
    }

    @Test
    public void getMealDelivery(){
        Json json = Json.getInstance();
        json.setPlaceName("MEAL_DELIVERY");
        Assert.assertTrue(Position.DELIVERY == json.calcularPosicion());
    }
    @Test
    public void getHairCare(){
        Json json = Json.getInstance();
        json.setPlaceName("HAIR_CARE");
        Assert.assertTrue(Position.PELUQUERIA == json.calcularPosicion());
    }
    @Test
    public void getMuseum(){
        Json json = Json.getInstance();
        json.setPlaceName("MUSEUM");
        Assert.assertTrue(Position.MUSEO == json.calcularPosicion());
    }
    @Test
    public void getTrainStation(){
        Json json = Json.getInstance();
        json.setPlaceName("TRAIN_STATION");
        Assert.assertTrue(Position.ESTACIONDETREN == json.calcularPosicion());
    }
    @Test
    public void getMovieTheater(){
        Json json = Json.getInstance();
        json.setPlaceName("MOVIE_THEATER");
        Assert.assertTrue(Position.CINE == json.calcularPosicion());
    }

    @Test
    public void getCoffe(){
        Json json = Json.getInstance();
        json.setPlaceName("CAFE");
        Assert.assertTrue(Position.CAFE == json.calcularPosicion());
    }
    @Test
    public void getSchool(){
        Json json = Json.getInstance();
        json.setPlaceName("SCHOOL");
        Assert.assertTrue(Position.ESCUELA == json.calcularPosicion());
    }
    @Test
    public void getZoo(){
        Json json = Json.getInstance();
        json.setPlaceName("ZOO");
        Assert.assertTrue(Position.ZOOLOGICO == json.calcularPosicion());
    }
    @Test
    public void getLaundry(){
        Json json = Json.getInstance();
        json.setPlaceName("LAUNDRY");
        Assert.assertTrue(Position.LAVANDERIA == json.calcularPosicion());
    }

    @Test
    public void getLodging(){
        Json json = Json.getInstance();
        json.setPlaceName("LODGING");
        Assert.assertTrue(Position.ALOJAMIENTO == json.calcularPosicion());
    }

    @Test
    public void getPetStore(){
        Json json = Json.getInstance();
        json.setPlaceName("PET_STORE");
        Assert.assertTrue(Position.VETERINARIA == json.calcularPosicion());
    }

    @Test
    public void getPharmacy(){
        Json json = Json.getInstance();
        json.setPlaceName("PHARMACY");
        Assert.assertTrue(Position.FARMACIA == json.calcularPosicion());
    }

    @Test
    public void getDentist(){
        Json json = Json.getInstance();
        json.setPlaceName("DENTIST");
        Assert.assertTrue(Position.DENTISTA == json.calcularPosicion());
    }
    @Test
    public void getNightClub(){
        Json json = Json.getInstance();
        json.setPlaceName("NIGHT_CLUB");
        Assert.assertTrue(Position.BOLICHE == json.calcularPosicion());
    }
    @Test
    public void getPhysiotherapist(){
        Json json = Json.getInstance();
        json.setPlaceName("PHYSIOTHERAPIST");
        Assert.assertTrue(Position.FISIOTERAPIA == json.calcularPosicion());
    }
    @Test
    public void getAmusementPark(){
        Json json = Json.getInstance();
        json.setPlaceName("AMUSEMENT_PARK");
        Assert.assertTrue(Position.PARQUE == json.calcularPosicion());
    }

    @Test
    public void getClothingStore(){
        Json json = Json.getInstance();
        json.setPlaceName("CLOTHING_STORE");
        Assert.assertTrue(Position.TIENDADEROPA == json.calcularPosicion());
    }
    @Test
    public void getChurh(){
        Json json = Json.getInstance();
        json.setPlaceName("CHURCH");
        Assert.assertTrue(Position.IGLESIA == json.calcularPosicion());
    }
    @Test
    public void getConvenienceStore(){
        Json json = Json.getInstance();
        json.setPlaceName("CONVENIENCE_STORE");
        Assert.assertTrue(Position.TIENDADECONVENIENCIA == json.calcularPosicion());
    }

    @Test
    public void testNothingPosition(){
        Json json = Json.getInstance();
        json.setPlaceName("testing");
        Assert.assertTrue(Position.NADA == json.calcularPosicion());
    }


    @Test
    public void compareToEqualsValue() {
        Json json = Json.getInstance();
        int value = json.compareTo(25f,25f);
        Assert.assertTrue(value ==0);
    }
    @Test
    public void compareToABiggerThanB() {
        Json json = Json.getInstance();
        int value = json.compareTo(35f,25f);
        Assert.assertEquals(value , -1);
    }
    @Test
    public void compareToBBiggerThanA() {
        Json json = Json.getInstance();
        int value = json.compareTo(25f,35f);
        Assert.assertEquals(value, 1);
    }

    @Test
    public void upgrateIndexValue(){
        Json json = Json.getInstance();
        json.upgradeIndexOfLoadOptions(7);
        Assert.assertTrue(Constants.VUELTAS_CARRETE == 7);
    }

    @Test
    public void valueBiggerThanCero(){
        Json json = Json.getInstance();
        Assert.assertTrue(json.getValueBiggerOrEquals0(7));
    }

    @Test
    public void valueLessCero(){
        Json json = Json.getInstance();
        Assert.assertFalse(json.getValueBiggerOrEquals0(-7));
    }

    @Test
    public void getGrupoFromId() {
        Json json = Json.getInstance();
        JSONObject object = createGroupsJSONArray();
        JSONObject value = null;
        try {
            json.setmJSONArrayTodosLosGrupos(object.getJSONArray("array"));
            value = json.getGrupoFromId(object.getInt("max"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(value);
    }

    @Test
    public void getPosPictoFirstValue() {
        Json json = Json.getInstance();
        int position = 0;
        JSONObject object = createPictogramJSONArray();
        try {
            json.setmJSONArrayTodosLosPictos(object.getJSONArray("array"));
            position = json.getPosPicto(json.getmJSONArrayTodosLosPictos(),object.getInt("min"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(position != -1);
    }

    @Test
    public void getPosPictoLastValue() {
        Json json = Json.getInstance();
        int position = 0;
        JSONObject object = createPictogramJSONArray();
        try {
            json.setmJSONArrayTodosLosPictos(object.getJSONArray("array"));
            position = json.getPosPicto(json.getmJSONArrayTodosLosPictos(),object.getInt("max"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(position != -1);
    }

    @Test
    public void getPosPictoNullValue() {
        Json json = Json.getInstance();
        int position = 0;
        JSONObject object = createPictogramJSONArray();
        try {
            json.setmJSONArrayTodosLosPictos(null);
            position = json.getPosPicto(json.getmJSONArrayTodosLosPictos(),object.getInt("max"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(position == -1);
    }


    @Test
    public void getArrayListFromTipo() {
    }

    @Test
    public void getPictoFromCustomArrayById() {
    }

    @Test
    public void getId() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        int id = 0;
        try {
            id= json.getId(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(id == 2);
    }

    @Test
    public void generarCheckSum() {
    }

    @Test
    public void ApplicationIsFailing() {
        Json json = Json.getInstance();
        json.sumarFallas();
        json.sumarFallas();
        json.sumarFallas();
        Assert.assertTrue(json.getCantFallas() == 3);
    }

    @Test
    public void getJsonObjectFromTexto() {
        Json json = Json.getInstance();
        ArrayList<JSONObject> array;
        JSONObject object = null;
        array = createTags();
        object = json.getJsonObjectFromTexto(array,Gender.MASCULINO.name());
        Assert.assertNotNull(object);
    }

    @Test
    public void readJSONArrayFromFile() {
    }

    @Test
    public void getJsonObjectFromTextoEnIngles() {
    }

    @Test
    public void compareValues() {
    }

    @Test
    public void getPictoFromCustomArrayById2() {
    }

    @Test
    public void cargarOpciones() {
    }

    @Test
    public void addFoto2BackUp() {
    }

    @Test
    public void arrayContains() {

    }

    @Test
    public void cargarPictosSugeridosJson() {
    }

    @Test
    public void crearFrase() {
    }

    @Test
    public void getThePhraseLastId() {
    }

    @Test
    public void resetearError() {
    }

    @Test
    public void sumarFallas() {
    }

    @Test
    public void getCantFallas() {
    }

    @Test
    public void ordenarSugerenciasPorTipo() {
    }

    @Test
    public void borrarSugerenciasPictosPadres() {
    }


    @Test
    public void estaEditadoFalse() {
        Json json = Json.getInstance();
        JSONObject object = createPictogramJSONArray();
        boolean value = true;
        try {
            JSONArray array = object.getJSONArray("array");
            value = json.estaEditado(array.getJSONObject(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertFalse(value);
    }

    @Test
    public void estaEditadoTrue() {
        Json json = Json.getInstance();
        JSONObject object = createPictogramJSONArray();
        boolean value = false;
        try {
            JSONArray array = object.getJSONArray("array");
             JSONObject child = array.getJSONObject(0);
             child.getJSONObject("imagen").put("pictoEditado","idealpath");
            value = json.estaEditado(child);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(value);
    }

    @Test
    public void devolverComplejidad() {
    }

    @Test
    public void agregarJuego() {
    }

    @Test
    public void getGame() {
    }

    @Test
    public void getObjectPuntaje() {
    }

    @Test
    public void devolverCantidadGruposUsados() {
    }


    @Test
    public void getScoreFail() {
        Json json = Json.getInstance();
        JSONObject object = createGroupWithTag();
        JSONArray array = new JSONArray();
        array.put(object);
        json.setmJSONArrayTodosLosPictos(array);
        int score =json.getScore(object,false);
        Assert.assertTrue(score == 0);
    }

    @Test
    public void getPhrasesByLanguage(){
        Json json = Json.getInstance();
        json.setmJSONArrayTodasLasFrases(Phrases());
        JSONArray result = json.getPhrasesByLanguage();
        Assert.assertNotNull(result);
    }
    @Test
    public void getPhrasesByLanguageError(){
        Json json = Json.getInstance();
        json.setmJSONArrayTodasLasFrases(PhrasesWithoutLocale());
        JSONArray result = json.getPhrasesByLanguage();
        Assert.assertNotNull(result);
    }

    @Test
    public void getmJSonArrayFrasesFavoritas() {
    }

    @Test
    public void setmJSonArrayFrasesFavoritas() {
    }

    @Test
    public void addAraasacPictogramFromInternet() {
        Json json = Json.getInstance();
        JSONObject pictogram = createPictograms(0,"es","perro","dog",0);
        json.setmJSONArrayTodosLosPictos(new JSONArray());
        json.addAraasacPictogramFromInternet(pictogram);
        try {
            Assert.assertEquals(pictogram,json.getmJSONArrayTodosLosPictos().getJSONObject(0));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void addPictogramToAll() {
    }



    private JSONObject createPictogramJSONArray() {
        JSONArray jsonArray = new JSONArray();
        JSONObject object = new JSONObject();
        int max = 0;
        int min = -1;
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            int aux = random.nextInt(size);
            int tipoRandom = random.nextInt(6);
            if(max == aux)
                aux = random.nextInt(size);
            if(min == -1)
                min = aux;
            max = setUpMaxValue(aux,max);
            min = setUpMinValue(aux,min);
            jsonArray.put(createPictograms(aux, "es", "Yo", "I",tipoRandom ));
        }
        try {
            object.put("array",jsonArray);
            object.put("max",max);
            object.put("min",min);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    private JSONObject createGroupsJSONArray() {
        JSONArray jsonArray = new JSONArray();
        JSONObject object = new JSONObject();
        int max = 0;
        int min = -1;
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            int aux = random.nextInt(size);
            if(max == aux)
                aux = random.nextInt(size);
            if(min == -1)
                min = aux;
            max = setUpMaxValue(aux,max);
            min = setUpMinValue(aux,min);
            jsonArray.put(createGroups(aux, "es", "Yo", "I"));
        }
        try {
            object.put("array",jsonArray);
            object.put("max",max);
            object.put("min",min);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }


    private JSONObject createPictograms(int id, String locale, String localeName, String englishName, int tipo) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("tipo", tipo);
            JSONObject texto = new JSONObject();
            texto.put("en", englishName);
            texto.put(locale, localeName);
            jsonObject.put("texto", texto);
            JSONObject imagen = new JSONObject();
            imagen.put("picto", "ic_action_previous");
            jsonObject.put("imagen", imagen);
            JSONArray jsonArray = new JSONArray();
            JSONObject relatedId1 = new JSONObject();
            relatedId1.put("id", getRandomValue(size)).put("frec", 10);
            JSONObject relatedId2 = new JSONObject();
            relatedId2.put("id",  getRandomValue(size));
            JSONObject relatedId3 = new JSONObject();
            relatedId3.put("id",  getRandomValue(size));
            jsonArray.put(relatedId1).put(relatedId2).put(relatedId3);
            jsonObject.put("relacion", jsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private JSONObject createGroups(int id, String locale, String localeName, String englishName) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            JSONObject texto = new JSONObject();
            texto.put("en", englishName);
            texto.put(locale, localeName);
            jsonObject.put("texto", texto);
            JSONObject imagen = new JSONObject();
            imagen.put("picto", "ic_action_previous");
            jsonObject.put("imagen", imagen);
            JSONArray jsonArray = new JSONArray();
            JSONObject relatedId1 = new JSONObject();
            relatedId1.put("id", getRandomValue(size)).put("frec", 10);
            JSONObject relatedId2 = new JSONObject();
            relatedId2.put("id",  getRandomValue(size));
            JSONObject relatedId3 = new JSONObject();
            relatedId3.put("id",  getRandomValue(size));
            jsonArray.put(relatedId1).put(relatedId2).put(relatedId3);
            jsonObject.put("relacion", jsonArray);
            jsonObject.put("frecuencia",getRandomValue(100));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private int getRandomValue(int max){
        Random random = new Random();
        return random.nextInt(max);
    }

    public JSONObject createGroupWithTag(){
        JSONObject object = createPictograms(2,"es","hola","hello",1);
        JSONArray array = new JSONArray();
        array.put(TimeOfDay.NOCHE);
        array.put(TimeOfDay.MANANA);
        array.put(Gender.MASCULINO);
        array.put(Gender.FLUIDO);
        array.put(AgeGroup.JOVEN);
        array.put(Position.AEROPUERTO);
        array.put(Weather.TORMENTA);
        try {
            object.put("tags",array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public JSONArray Phrases() {
        try {
            return new JSONArray("[{\"frase\":\"yo quiero ir a casa \",\"frecuencia\":1,\"complejidad\":{\"valor\":0,\"pictos componentes\":[{\"id\":643,\"esSugerencia\":false},{\"id\":44,\"esSugerencia\":false},{\"id\":30,\"esSugerencia\":false},{\"id\":653,\"esSugerencia\":false}]},\"fecha\":1620650383499,\"locale\":\"es\",\"id\":0},{\"frase\":\"Hola ¿Cómo estás? estoy aburrido quiero \",\"frecuencia\":1,\"complejidad\":{\"valor\":0,\"pictos componentes\":[{\"id\":377,\"esSugerencia\":false},{\"id\":378,\"esSugerencia\":false},{\"id\":22,\"esSugerencia\":false},{\"id\":119,\"esSugerencia\":false},{\"id\":44,\"esSugerencia\":false}]},\"fecha\":1620652671707,\"locale\":\"es\",\"id\":2},{\"frase\":\"acompañar \",\"frecuencia\":4,\"complejidad\":{\"valor\":0,\"pictos componentes\":[{\"id\":1,\"hora\":[\"TARDE\",\"MEDIODIA\"],\"edad\":[\"JOVEN\"],\"sexo\":[\"MASCULINO\"],\"ubicacion\":[\"CAFE\",\"BAR\"]}]},\"fecha\":[1620848827861,1620848833313,1620848876219,1620848936363],\"locale\":\"es\",\"id\":5},{\"frase\":\"Hola Buenas tardes \",\"frecuencia\":3,\"complejidad\":{\"valor\":0,\"pictos componentes\":[{\"id\":377,\"esSugerencia\":false},{\"id\":380,\"horario\":[\"MEDIODIA\",\"TARDE\"],\"esSugerencia\":false,\"hora\":[\"TARDE\"]}]},\"fecha\":[1620673767927,1621017780404,1621017791290],\"locale\":\"es\",\"id\":4}]");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    public JSONArray PhrasesWithoutLocale() {
        try {
            return new JSONArray("[{\"frase\":\"null\",\"locale\":1,},{\"frase\":\"null \",\"locale\":1,}]");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }




   private int setUpMaxValue(int value,int compareValue){
        if(value>compareValue)
            return value;
        else
            return compareValue;
   }

    private int setUpMinValue(int value,int compareValue){
        if(value<compareValue)
            return value;
        else
            return compareValue;
    }

    private ArrayList<JSONObject> createTags() {
        String tags = "[{\"id\":379,\"texto\":{\"en\":\"Good morning\",\"es\":\"" + TimeOfDay.MANANA + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"ic_buenos_dias\"}}," +
                "{\"id\":818,\"texto\":{\"en\":\"summer\",\"es\":\"" + TimeOfDay.MEDIODIA + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"verano\"}}," +
                "{\"id\":380,\"texto\":{\"en\":\"Good afternoon\",\"es\":\"" + TimeOfDay.TARDE + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"ic_buenas_tardes\"}}," +
                "{\"id\":381,\"texto\":{\"en\":\"Good night\",\"es\":\"" + TimeOfDay.NOCHE + "\"},\"tipo\":\"" + Constants.HORA + "\",\"imagen\":{\"picto\":\"ic_buenas_noches\"}}," +
                "{\"id\":614,\"texto\":{\"en\":\"grandpa\",\"es\":\"" + AgeGroup.ADULTO + "\"},\"tipo\":\"" + Constants.EDAD + "\",\"imagen\":{\"picto\":\"ic_abuelo\"}}," +
                "{\"id\":195,\"texto\":{\"en\":\"young\",\"es\":\"" + AgeGroup.JOVEN + "\"},\"tipo\":\"" + Constants.EDAD + "\",\"imagen\":{\"picto\":\"joven\"}}," +
                "{\"id\":630,\"texto\":{\"en\":\"boy\",\"es\":\"" + AgeGroup.NINO + "\"},\"tipo\":\"" + Constants.EDAD + "\",\"imagen\":{\"picto\":\"nino\"}}," +
                "{\"id\":651,\"texto\":{\"en\":\"field\",\"es\":\"" + Position.ESTADIO + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"cancha\"}}," +
                "{\"id\":653,\"texto\":{\"en\":\"square\",\"es\":\"" + Position.PARQUE + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"plaza\"}}," +
                "{\"id\":1013,\"texto\":{\"en\":\"movie theater\",\"es\":\"" + Position.CINE + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"cine\"}}," +
                "{\"id\":654,\"texto\":{\"en\":\"computer store\",\"es\":\"" + Position.TIENDA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"tienda_de_informatica\"}}," +
                "{\"id\":687,\"texto\":{\"en\":\"Bar\",\"es\":\"" + Position.BAR + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"bar\"}}," +
                "{\"id\":496,\"texto\":{\"en\":\"coffee\",\"es\":\"" + Position.CAFE + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"cafe\"}}," +
                "{\"id\":685,\"texto\":{\"en\":\"Restaurant\",\"es\":\"" + Position.RESTAURANT + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"restaurante\"}}," +
                "{\"id\":672,\"texto\":{\"en\":\"bakery\",\"es\":\"" + Position.PANADERIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"pan_blanco\"}}," +
                "{\"id\":652,\"texto\":{\"en\":\"butchery\",\"es\":\"" + Position.CARNICERIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"carniceria\"}}," +
                "{\"id\":698,\"texto\":{\"en\":\"greengrocery\",\"es\":\"" + Position.VERDULERIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"verduleria\"}}," +
                "{\"id\":655,\"texto\":{\"en\":\"farmacy\",\"es\":\"" + Position.FARMACIA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"ic_farmacia\"}}," +
                "{\"id\":668,\"texto\":{\"en\":\"hospital\",\"es\":\"" + Position.HOSPITAL + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"hospital\"}}," +
                "{\"id\":662,\"texto\":{\"en\":\"school\",\"es\":\"" + Position.ESCUELA + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"ic_escuela\"}}," +
                "{\"id\":611,\"texto\":{\"en\":\"transportation\",\"es\":\"" + Position.ESTACIONDEBUS + "\"},\"tipo\":\"" + Constants.UBICACION + "\",\"imagen\":{\"picto\":\"transportes\"}}," +
                "{\"id\":1033,\"texto\":{\"en\":\"woman\",\"es\":\"" + Gender.FEMENINO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"mujer\"}}," +
                "{\"id\":1035,\"texto\":{\"en\":\"man\",\"es\":\"" + Gender.MASCULINO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}," +
                "{\"id\":1036,\"texto\":{\"en\":\"binary\",\"es\":\"" + Gender.BINARIO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}," +
                "{\"id\":1037,\"texto\":{\"en\":\"fluid\",\"es\":\"" + Gender.FLUIDO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}," +
                "{\"id\":1038,\"texto\":{\"en\":\"others\",\"es\":\"" + Gender.OTRO + "\"},\"tipo\":\"" + Constants.SEXO + "\",\"imagen\":{\"picto\":\"hombre\"}}]\n";
        return JSONutils.stringToArrayList(tags);
    }
}