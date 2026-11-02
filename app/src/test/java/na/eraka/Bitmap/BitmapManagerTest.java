package na.eraka.Bitmap;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class BitmapManagerTest  {
    private final BitmapManager gestionarBitmap = new BitmapManager(null);

    @Test
    public void setNombre(){
        gestionarBitmap.setNombre("Hello");
        Assert.assertTrue(gestionarBitmap.getNombre().equals("Hello"));
    }
    @Test
    public void setTexto(){
        gestionarBitmap.setTexto("Hello");
        Assert.assertTrue(gestionarBitmap.getTexto().equals("Hello"));
    }

    @Test
    public void setNoTemp(){
        gestionarBitmap.setNoTemp(true);
        Assert.assertTrue(gestionarBitmap.isNoTemp());
    }
}