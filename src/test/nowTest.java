package test;


import com.sin.entity.SQLNumberType;
import com.sin.entity.TableEntity;
import com.sin.service.DBConnection;
import com.sin.service.DBManager;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sin.entity.SQLNumberType;

public class nowTest {
//    --data_path /tmp/data --dst_ip 121.41.55.205 --dst_port 3306 --dst_user root --dst_password changesql
    String DstIP = "121.41.55.205";
    String DstPort = "3306";
    String DstUser = "root";
    String DstPassword = "changesql";

    DBConnection dbconn = new DBConnection(DstIP, DstPort, DstUser, DstPassword);
    Connection conn = dbconn.connectDB();
    DBManager dbManager = new DBManager("tmp/data/");

    public nowTest() throws IOException {
//        dbManager.createDB(conn);
    }

    @Test
    public void runParse(){
        String sql = "CREATE TABLE if not exists `1`  (\n" +
                "  `id` BIGINT(299 299) unsigned NOT NULL,\n" +
                "  `a` float NOT NULL DEFAULT '0',\n" +
                "  `b` char(32) NOT NULL DEFAULT '',\n" +
                "  `updated_at` datetime NOT NULL DEFAULT '2021-12-12 00:00:00',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8";
        try {
            CreateTable createTable = (CreateTable) CCJSqlParserUtil.parse(sql);
            for (ColumnDefinition col : createTable.getColumnDefinitions()) {
//                columnTypes.put(col.getColumnName(), col.getColDataType().toString());
//                System.out.println(col.getColumnName() + "," + col.getColDataType());
//                System.out.println(col.toString());
//                List<String> list = col.getColumnSpecs();
//                System.out.println(col.getColumnSpecs());
                System.out.println(col.getColDataType().getArgumentsStringList());
            }
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void enumTest(){
        // Map<String, SQLNumberType>
        SQLNumberType intType = SQLNumberType.valueOf("int".toUpperCase());
        SQLNumberType tinyType = SQLNumberType.valueOf("tinyint".toUpperCase());
        System.out.println(intType.ordinal());
        System.out.println(tinyType.compareTo(intType));
        if(SQLNumberType.contains("iNT".toUpperCase()))
            System.out.println("YES");
    }
}
