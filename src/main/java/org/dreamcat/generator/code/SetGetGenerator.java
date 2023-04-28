package org.dreamcat.generator.code;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ReflectUtil;

/**
 * self                        :-:      self
 * subclass                     -:      superclass
 * box                         :-:      primitive
 * byte/short/int/long/float    -:      type promotion
 * flat                        :-:      String
 *
 * @author Jerry Will
 * @version 2021-11-25
 */
@With
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetGetGenerator {

    private static final String template_all =
            "public static $target $method($source $sourceName) {\n$body}";
    private static final String template_new = "    $target $targetName = new $target();\n";
    private static final String template_set_get = "    $targetName.$setter($wrap);\n";
    private static final String template_return = "    return $targetName;\n";

    private String sourceName = "src";
    private String targetName = "res";
    private String method; // to$target
    private boolean typeConstraint;
    private LinkedList<BiFunction<Class<?>, Class<?>, String>> typeFormatters =
            new LinkedList<>(Collections.singleton(default_type_formatter));

    public SetGetGenerator addTypeFormat(BiFunction<Class<?>, Class<?>, String> typeFormatter) {
        typeFormatters.addFirst(typeFormatter);
        return this;
    }

    public String generate(Class<?> source, Class<?> target) {
        StringBuilder body = new StringBuilder();
        // new
        body.append(InterpolationUtil.format(template_new,
                MapUtil.of("target", target.getSimpleName(),
                        "targetName", targetName)));
        // set-get
        Map<String, Field> sourceFields = ReflectUtil.retrieveNoStaticFieldMap(source);
        Map<String, Field> targetFields = ReflectUtil.retrieveNoStaticFieldMap(target);
        targetFields.forEach((fieldName, targetField) -> {
            Field sourceField = sourceFields.get(fieldName);
            if (sourceField == null) return;

            String setter = ReflectUtil.getSetter(target, targetField).getName();
            String getter = ReflectUtil.getGetter(source, sourceField).getName();

            String wrap = sourceName + "." + getter + "()";
            Class<?> srcType = sourceField.getType(), destType = targetField.getType();
            String format = getTypeFormat(srcType, destType);
            if (format != null) {
                wrap = String.format(format, wrap);
            } else if (typeConstraint) return;

            body.append(InterpolationUtil.format(template_set_get, MapUtil.of(
                    "targetName", targetName,
                    "setter", setter, "wrap", wrap)));
        });
        // return
        body.append(InterpolationUtil.format(template_return,
                MapUtil.of("targetName", targetName)));

        String methodStr = method != null ? method : "to" + target.getSimpleName();
        return InterpolationUtil.format(template_all,
                MapUtil.of("target", target.getSimpleName(),
                        "method", methodStr,
                        "source", source.getSimpleName(),
                        "sourceName", sourceName,
                        "body", body.toString()));
    }

    private String getTypeFormat(Class<?> sfType, Class<?> tfType) {
        for (BiFunction<Class<?>, Class<?>, String> formatter : typeFormatters) {
            String format = formatter.apply(sfType, tfType);
            if (format != null) return format;
        }
        return null;
    }

    private final static BiFunction<Class<?>, Class<?>, String>
            default_type_formatter = (src, dest) -> {
        // destObj = srcObj;
        if (ReflectUtil.isSubOrEqOrBox(src, dest)) return "%s";
        //  * flat                        :-:      String
        if (ReflectUtil.isBoxedClass(src) || src.isPrimitive()) {
            if (ReflectUtil.isBoxedClass(dest) || dest.isPrimitive()) {
                if (ReflectUtil.isTypePromotion(src, dest)) {
                    if (src.isPrimitive() && dest.isPrimitive()) {
                        return "%s"; // type promotion
                    } else {
                        // primitive-box: cast and auto-box
                        // box-primitive: auto-unbox and cast
                        // box-box: auto-unbox and cast and auto-box
                        return "(" + dest + ")%s";
                    }
                }
            } else if (dest.equals(String.class)) {
                if (ReflectUtil.isBoxedClass(src)) {
                    return "%s.toString()";
                } else {
                    return "String.valueOf(%s)";
                }
            }
        } else if (src.equals(String.class)) {
            if (ReflectUtil.isSub(dest, Enum.class)) {
                return dest.getSimpleName() + ".valueOf(%s)";
            } else if (dest.equals(int.class)) {
                return "Integer.parseInt(%s)";
            } else if (dest.equals(Integer.class)) {
                return "Integer.valueOf(%s)";
            } else if (dest.equals(long.class)) {
                return "Long.parseLong(%s)";
            } else if (dest.equals(Long.class)) {
                return "Long.valueOf(%s)";
            } else if (dest.equals(double.class)) {
                return "Double.parseDouble(%s)";
            } else if (dest.equals(Double.class)) {
                return "Double.valueOf(%s)";
            } else if (dest.equals(boolean.class)) {
                return "Boolean.parseInt(%s)";
            } else if (dest.equals(Boolean.class)) {
                return "Boolean.valueOf(%s)";
            } else if (dest.equals(short.class)) {
                return "Short.parseShort(%s)";
            } else if (dest.equals(Short.class)) {
                return "Short.valueOf(%s)";
            } else if (dest.equals(byte.class)) {
                return "Byte.parseByte(%s)";
            } else if (dest.equals(Byte.class)) {
                return "Byte.valueOf(%s)";
            } else if (dest.equals(float.class)) {
                return "Float.parseFloat(%s)";
            } else if (dest.equals(Float.class)) {
                return "Float.valueOf(%s)";
            }
        } else if (ReflectUtil.isSub(src, Enum.class)) {
            if (dest.equals(String.class)) {
                return "%s.name()";
            }
        }
        return null;
    };
}
