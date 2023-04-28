package org.dreamcat.generator.code;

import java.util.Date;
import java.util.List;
import lombok.Data;
import org.junit.jupiter.api.Test;

/**
 * Create by tuke on 2020/11/18
 */
class PojoFlatGeneratorTest {

    @Test
    void test() {
        System.out.println(new PojoFlatGenerator().generate(A.class));
    }

    @Data
    static class A {
        int someInt;
        boolean someBool;
        B b;
    }

    @Data
    static class B {
        String someString;
        Date someDate;
        C c;
    }

    @Data
    static class C {
        List<Long> list;
    }
}
