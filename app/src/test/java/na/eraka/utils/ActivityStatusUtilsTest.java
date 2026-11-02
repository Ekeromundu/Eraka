package na.eraka.utils;

import na.eraka.MainBoardActivity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActivityStatusUtilsTest {
    ActivityStatusUtils activityUtilsEstatus = new ActivityStatusUtils();

    @Test
    public void testIsValidContext() {
        assertEquals(false,activityUtilsEstatus.isValidContext(null));
    }



}