package eu.battleland.crownedbank;

import lombok.Getter;
import lombok.Setter;

import java.util.jar.JarEntry;

public class CrownedBankConstants {
    
    /**
     * Boolean indicating whether should identities be compared by name(true), or uuid(false).
     */
    @Getter @Setter
    private static boolean identityNameMajor = false;


    @Getter @Setter
    private static String sqlTablePrefix = "crownedbank";

    @Getter @Setter
    private static String sqlTableCommand = """
            create table if not exists `%s_data`
             ( `identity_name` TEXT NOT NULL , `identity_uuid` TEXT NOT NULL , `json_data` TEXT NOT NULL , UNIQUE (`identity_name`), UNIQUE (`identity_uuid`));
            """;
    @Getter @Setter
    private static String sqlUpdateCommand = """
            update `%s_data`
            set json_data='%s'
            where `identity_name`='%s' OR `identity_uuid`='%s'
            """;
    @Getter @Setter
    private static String sqlInsertCommand = """
            insert into `%s_data` (`identity_name`,`identity_uuid`,`json_data`) values('%s','%s','%s')
            """;
    @Getter @Setter
    private static String sqlQueryCommand = """
            select `json_data` from `%s_data`
            where `identity_name`='%s' OR `identity_uuid`='%s'
            """;
}
