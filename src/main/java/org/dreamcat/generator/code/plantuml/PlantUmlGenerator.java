package org.dreamcat.generator.code.plantuml;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.dreamcat.common.sql.SqlUtil;
import org.dreamcat.common.sql.TableCommonDef;

/**
 * @author Jerry Will
 * @version 2022-07-17
 */
@With
@Setter
// @NoArgsConstructor
@AllArgsConstructor
public class PlantUmlGenerator {

    public String generateObject(String sql) {
        List<TableCommonDef> tables = SqlUtil.parseCreateTable(sql);
        // todo
        return null;
    }
}
