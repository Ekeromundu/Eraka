package na.eraka.idioma;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class AppLanguageTest {
    private final AppLanguage configurarIdioma = new AppLanguage();
    @Test
    public void testSetLanguage() {
        AppLanguage.setLanguage("en");
        Assert.assertTrue(AppLanguage.getLanguaje().equals("en"));
    }
    @Test
    public void testGetLanguaje() {
        Assert.assertTrue(AppLanguage.getLanguaje().equals("en"));
    }

    @Test
    public void testGetLanguajeEn() {
        AppLanguage.setLanguage("en");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("english"));
    }
    @Test
    public void testGetLanguajeCa() {
        AppLanguage.setLanguage("ca");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("catalan"));
    }
    @Test
    public void testGetLanguajePT() {
        AppLanguage.setLanguage("pt");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("portuguese"));
    }
    @Test
    public void testGetLanguajeEs() {
        AppLanguage.setLanguage("es");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("spanish"));
    }
    @Test
    public void testGetLanguajeFr() {
        AppLanguage.setLanguage("fr");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("french"));
    }
    @Test
    public void testGetLanguajeIt() {
        AppLanguage.setLanguage("it");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("italian"));
    }
    @Test
    public void testGetLanguajeAr() {
        AppLanguage.setLanguage("ar");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("arabic"));
    }
    @Test
    public void testGetLanguajeDa() {
        AppLanguage.setLanguage("da");
        Assert.assertTrue(AppLanguage.getNormalLanguage().equals("danish"));
    }
    @Test
    public void testGetLanguajeIso6393En() {
        String result = AppLanguage.getLanguageIso6393("eb");
        Assert.assertTrue(result.equals("eng"));
    }
    @Test
    public void testGetLanguajeIso6393Es() {
        String result = AppLanguage.getLanguageIso6393("es");
        Assert.assertTrue(result.equals("spa"));
    }
    @Test
    public void testGetLanguajeIso6393Pt() {

        String result = AppLanguage.getLanguageIso6393("pt");
        Assert.assertTrue(result.equals("por"));
    }
    @Test
    public void testGetLanguajeIso6393Ca() {
        String result = AppLanguage.getLanguageIso6393("ca");
        Assert.assertTrue(result.equals("cat"));
    }
    @Test
    public void testGetLanguajeIso6393Fr() {
        String result = AppLanguage.getLanguageIso6393("fr");
        Assert.assertTrue(result.equals("fra"));
    }
    @Test
    public void testGetLanguajeIso6393Ita() {
        String result = AppLanguage.getLanguageIso6393("it");
        Assert.assertTrue(result.equals("ita"));
    }
    @Test
    public void testGetLanguajeIso6393Ara() {
        String result = AppLanguage.getLanguageIso6393("ar");
        Assert.assertTrue(result.equals("ara"));
    }
    @Test
    public void testGetLanguajeIso6393Da() {
        String result = AppLanguage.getLanguageIso6393("da");
        Assert.assertTrue(result.equals("dan"));
    }



}