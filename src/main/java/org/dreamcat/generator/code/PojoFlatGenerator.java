package org.dreamcat.generator.code;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * Create by tuke on 2020/11/18
 */
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PojoFlatGenerator {

    @Builder.Default
    Function<Field, String> nameMapper = Field::getName;
    // valueMapper
    BiFunction<String, Field, String> valueMapper;
    BiFunction<String, Field, String> primitiveValueMapper;
    BiFunction<String, Field, String> numberValueMapper;
    BiFunction<String, Field, String> boolValueMapper;
    BiFunction<String, Field, String> charValueMapper;
    BiFunction<String, Field, String> stringValueMapper;
    BiFunction<String, Field, String> dateValueMapper;
    BiFunction<String, Field, String> localDateValueMapper;
    BiFunction<String, Field, String> localTimeValueMapper;
    BiFunction<String, Field, String> localDateTimeValueMapper;
    BiFunction<String, Field, String> enumValueMapper;

    private static final String convert_method =
            "public static Map<String, Object> flat($class bean) {\n"
                    + "Map<String, Object> map = new HashMap<>();\n"
                    + "$assignment\n"
                    + "return map;\n"
                    + "}";
    private static final String get_object =
            "$object.$getter()";
    private static final String get_object_assign =
            "$type $variable = $object.$getter();\n";
    private static final String put_value =
            "map.put(\"$name\", $value);\n";

    // flat a bean to map
    public String generate(Class<?> pojoClass) {
        Map<String, String> context = new HashMap<>();
        context.put("class", pojoClass.getSimpleName());
        StringBuilder assignment = new StringBuilder();
        Set<Class<?>> cacheClasses = new HashSet<>(Collections.singleton(pojoClass));
        Set<String> putKeys = new HashSet<>(32);
        appendRecursively("bean", pojoClass, assignment, cacheClasses, putKeys);
        context.put("assignment", assignment.toString());
        return InterpolationUtil.format(convert_method, context);
    }

    private void appendRecursively(
            String object, Class<?> pojoClass, StringBuilder assignment,
            Set<Class<?>> cacheClasses, Set<String> putKeys) {
        List<Field> fields = ReflectUtil.retrieveBeanFields(pojoClass);
        for (Field field : fields) {
            String fieldName = field.getName();
            String name = nameMapper.apply(field);
            if (putKeys.contains(name)) {
                continue;
            } else {
                putKeys.add(name);
            }

            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType) ||
                    Map.class.isAssignableFrom(fieldType) ||
                    fieldType.isArray()) continue;

            if (ReflectUtil.isFlatOrContainer(fieldType)) {
                // get from object
                Map<String, String> getMap = new HashMap<>();
                getMap.put("object", object);
                getMap.put("getter", "get" + StringUtil.toCapitalCase(fieldName));
                String value = InterpolationUtil.format(get_object, getMap);
                // put into map
                String mappedValue = valueMap(value, field);
                Map<String, String> putMap = new HashMap<>();
                putMap.put("name", name);
                putMap.put("value", mappedValue);
                String putValue = InterpolationUtil.format(put_value, putMap);
                assignment.append(putValue);
            } else {
                if (cacheClasses.contains(fieldType)) continue;

                Set<Class<?>> newCacheClasses = new HashSet<>(cacheClasses);
                newCacheClasses.add(fieldType);

                Map<String, String> map = new HashMap<>();
                map.put("type", fieldType.getSimpleName());
                map.put("variable", name);
                map.put("object", object);
                map.put("getter", "get" + StringUtil.toCapitalCase(fieldName));
                String getObject = InterpolationUtil.format(get_object_assign, map);
                assignment.append('\n').append(getObject);
                appendRecursively(name, fieldType, assignment, newCacheClasses, putKeys);
            }
        }
    }

    private String valueMap(String value, Field field) {
        Class<?> cls = field.getType();

        if (cls.isPrimitive() && primitiveValueMapper != null) {
            return primitiveValueMapper.apply(value, field);
        } else if (Number.class.isAssignableFrom(cls) && numberValueMapper != null) {
            return numberValueMapper.apply(value, field);
        } else if (Boolean.class.equals(cls) && boolValueMapper != null) {
            return boolValueMapper.apply(value, field);
        } else if (Character.class.equals(cls) && charValueMapper != null) {
            return charValueMapper.apply(value, field);
        } else if (String.class.equals(cls) && stringValueMapper != null) {
            return stringValueMapper.apply(value, field);
        } else if (Date.class.equals(cls) && dateValueMapper != null) {
            return dateValueMapper.apply(value, field);
        } else if (LocalDate.class.isAssignableFrom(cls) && localDateValueMapper != null) {
            return localDateValueMapper.apply(value, field);
        } else if (LocalTime.class.isAssignableFrom(cls) && localTimeValueMapper != null) {
            return localTimeValueMapper.apply(value, field);
        } else if (LocalDateTime.class.isAssignableFrom(cls) && localDateTimeValueMapper != null) {
            return localDateTimeValueMapper.apply(value, field);
        } else if (Enum.class.isAssignableFrom(cls) && enumValueMapper != null) {
            return enumValueMapper.apply(value, field);
        } else if (valueMapper != null) {
            return valueMapper.apply(value, field);
        }
        return value;
    }
}
