package org.dreamcat.generator.code.amap;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import org.dreamcat.common.Pair;
import org.dreamcat.common.io.IOUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @see <a href="https://lbs.amap.com/api/webservice/guide/api/staticmaps">amap static maps</a>
 * @see <a href="https://console.amap.com/dev/key/app">amap app key</>
 * @since 2021-06-30
 */
@Data
public class AmapStaticMapImage {

    // 用户唯一标识
    String key;
    // 地图中心点, 规则：经度和纬度用","分隔 经纬度小数点后不得超过6位
    String location;
    // 地图缩放级别:[1,17]
    int zoom;
    // 地图大小=图片宽度*图片高度。最大值为1024*1024，默认400*400
    String size;
    // 普通/高清
    // 1: 返回普通图(默认)；
    // 2: 调用高清图，图片高度和宽度都增加一倍，zoom也增加一倍
    Integer scale;
    // 标注，最大数10个
    // markers=markersStyle1:location1;location2..|markersStyle2:location3;location4..|markersStyleN:locationN;locationM..
    // location为经纬度信息，经纬度之间使用","分隔，不同的点使用";"分隔。 markersStyle可以使用系统提供的样式，也可以使用自定义图片
    // 系统marersStyle：label，font ,bold, fontSize，fontColor，background
    final List<Marker> markers = new ArrayList<>();
    // 标签，最大数10个
    // labelsStyle1:location1;location2..|labelsStyle2:location3;location4..|labelsStyleN:locationN;locationM..
    final List<Label> labels = new ArrayList<>();
    // 折线和多边形，最大数4个
    final List<Path> paths = new ArrayList<>();
    // 底图是否展现实时路况。 可选值： 0，不展现(默认)；1，展现。
    Integer traffic;

    public String getImageUrl() {
        StringBuilder s = new StringBuilder(URL_PREFIX.length() << 2);
        s.append(String.format(URL_PREFIX, key, zoom));
        if (ObjectUtil.isNotBlank(location)) {
            s.append("&location=").append(location);
        }
        if (ObjectUtil.isNotBlank(size)) {
            s.append("&size=").append(size);
        }
        if (scale != null) {
            s.append("&scale=").append(scale);
        }
        if (ObjectUtil.isNotEmpty(markers)) {
            s.append("&markers=").append(StringUtil.join("|", markers));
        }
        if (ObjectUtil.isNotEmpty(labels)) {
            s.append("&labels=").append(StringUtil.join("|", labels));
        }
        if (ObjectUtil.isNotEmpty(paths)) {
            s.append("&paths=").append(StringUtil.join("|", paths));
        }
        if (traffic != null) {
            s.append("&traffic=").append(traffic);
        }
        return s.toString();
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    public byte[] fetch() {
        return fetch(url -> {
            try {
                URLConnection c = new URL(url).openConnection();
                return IOUtil.readFully(c.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public byte[] fetch(Function<String, byte[]> httpClient) {
        String imageUrl = getImageUrl();
        return httpClient.apply(imageUrl);
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    @Data
    public static class Marker {

        // 可选值： small,mid,large
        // 取-1，则表示为自定义图片
        String size;
        // 选值范围：[0x000000, 0xffffff]
        // 或PNG格式的图片的网址URL，如果size=-1
        String color;
        // [0-9]、[A-Z]、[单个中文字] 当size为small时，图片不展现标注名
        // 0，如果size=-1
        String label;
        // 经纬度小数点后不得超过6位
        // longitude1,latitude1;longitude2,latitude2
        final List<Pair<String, String>> locations = new ArrayList<>();

        public Marker(String pngUrl) {
            this("-1", pngUrl, "0");
        }

        public Marker(String size, String color, String label) {
            this.size = size;
            this.color = color;
            this.label = label;
        }

        public Marker addLocation(String longitude, String latitude) {
            this.locations.add(Pair.of(longitude, latitude));
            return this;
        }

        @Override
        public String toString() {
            // markersStyle1:location1;location2
            return String.format("%s,%s,%s:%s",
                    size, color, label,
                    locationString(locations));
        }
    }

    @Data
    public static class Label {

        // 标签内容，字符最大数目为15
        String content;
        // 0：微软雅黑；
        // 1：宋体；
        // 2：Times New Roman;
        // 3：Helvetica
        // 默认0
        int font = 0;
        // 0：非粗体；1：粗体，默认0
        int bold = 0;
        // 字体大小，可选值[1,72]，默认10
        int fontSize = 10;
        // 字体颜色，取值范围：[0x000000, 0xffffff]，默认0xFFFFFF
        String fontColor = "0xFFFFFF";
        // 背景色，取值范围：[0x000000, 0xffffff]，默认0x5288d8
        String background = "0x5288d8";
        // longitude1,latitude1;longitude2,latitude2
        final List<Pair<String, String>> locations = new ArrayList<>();

        public Label addLocation(String longitude, String latitude) {
            this.locations.add(Pair.of(longitude, latitude));
            return this;
        }

        @Override
        public String toString() {
            return String.format("%s,%d,%d,%d,%s,%s:%s",
                    content,
                    font, bold, fontSize,
                    fontColor, background,
                    locationString(locations));
        }
    }

    @Data
    public static class Path {

        // 线条粗细。可选值： [2,15]
        int weight = 5;
        // 折线颜色。 选值范围：[0x000000, 0xffffff]
        String color = "0x0000FF";
        // 透明度。可选值[0,1]，小数后最多2位，0表示完全透明，1表示完全不透明。
        String transparency = "1";
        // 多边形的填充颜色，此值不为空时折线封闭成多边形。取值规则同color
        String fillcolor = "";
        // 填充面透明度。可选值[0,1]，小数后最多2位，0表示完全透明，1表示完全不透明。
        String fillTransparency = "0.5";
        // longitude1,latitude1;longitude2,latitude2
        final List<Pair<String, String>> locations = new ArrayList<>();

        public Path(int weight, String color, String transparency, String fillcolor) {
            this.weight = weight;
            this.color = color;
            this.transparency = transparency;
            this.fillcolor = fillcolor;
        }

        public Path addLocation(String longitude, String latitude) {
            this.locations.add(Pair.of(longitude, latitude));
            return this;
        }

        @Override
        public String toString() {
            return String.format("%d,%s,%s,%s,%s:%s",
                    weight, color, transparency,
                    fillcolor, fillTransparency,
                    locationString(locations));
        }
    }

    // ==== ==== ==== ====    ==== ==== ==== ====    ==== ==== ==== ====

    private static String locationString(List<Pair<String, String>> locations) {
        return locations.stream().map(it -> it.first() + "," + it.second())
                .collect(Collectors.joining(";"));
    }

    private static String strings(List<?> list) {
        return list.stream()
                .map(Object::toString)
                .collect(Collectors.joining("|"));
    }

    private static final String URL_PREFIX = "https://restapi.amap.com/v3/staticmap"
            + "?key=%s"
            + "&zoom=%d";

    private static final String URL_FULL = "https://restapi.amap.com/v3/staticmap"
            + "?key=%s"
            + "&zoom=%d"
            + "&location=%s"
            + "&size=%s"
            + "&scale=%d"
            + "&markers=%s"
            + "&labels=%s"
            + "&paths=%s"
            + "&traffic=%d";
}
