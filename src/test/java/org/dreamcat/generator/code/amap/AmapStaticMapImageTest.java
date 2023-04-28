package org.dreamcat.generator.code.amap;

import org.dreamcat.common.util.Base64Util;
import org.dreamcat.common.util.SystemUtil;
import org.dreamcat.generator.code.amap.AmapStaticMapImage.Marker;
import org.dreamcat.generator.code.amap.AmapStaticMapImage.Path;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @since 2021-06-30
 */
class AmapStaticMapImageTest {

    @Test
    void fetch() {
        AmapStaticMapImage amap = new AmapStaticMapImage();

        amap.setKey(SystemUtil.getEnv("AMAP_KEY", ""));
        amap.setZoom(getZoomByDistanceForAmap(2.6));
        amap.setSize("750*300");

        amap.getMarkers().add(new Marker("large", "", "送")
                .addLocation("116.31604", "39.96491"));
        amap.getMarkers().add(new Marker("large", "", "取")
                .addLocation("116.320816", "39.966606"));
        amap.getPaths().add(new Path(3, "0x0000FF", "1", "")
                .addLocation("116.32361", "39.966957")
                .addLocation("115.32361", "40.966957"));

        System.out.println(amap.getImageUrl());
        byte[] data = amap.fetch();
        System.out.println(Base64Util.encodeAsString(data));
    }

    public int getZoomByDistanceForAmap(double distanceKm) {
        int zoom;
        if (distanceKm >= 4) {
            zoom = 11;
        } else if (distanceKm >= 2) {
            zoom = 12;
        } else if (distanceKm >= 1) {
            zoom = 13;
        } else if (distanceKm >= 0) {
            zoom = 14;
        } else {
            zoom = 13;
        }
        return zoom;
    }
}
