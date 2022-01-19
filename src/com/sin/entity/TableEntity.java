package com.sin.entity;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;

import java.util.*;

public class TableEntity {
    public List<String> tableDataPath;

    // 表格属性
    public String name;
    public int columnLen;
    public List<String> columns;
    public Map<String, ColumnDefinition> columnDefinitionMap;
    public CreateTable createTable;
    public Set<Integer> keySet;
    // public Set<Integer> floatIndexSet;
    // have key index? (not primary key index)
    public boolean isKey = false;
    // have Key? if we have key we should use the key to find the data
    public boolean hasKey = false;
    // have column in the key which the type is float
     public int[] floatValueIndex;
    // public int[] floatKeyName;
    public boolean[] colIsFloat;

    // may be key / primary key index
    // which columns in the data is the key
    public int[] keyIndex;
    // get the type of corresponding columns in the keyIndex
    public String[] keyType;
    // index of update in the columns
    public int updatedatIndex = 3;

    // PreparedStatement build string
    public StringBuilder updateSB, insertSB, selectSB;

    public TableEntity(String createTableStatement) {
        this.tableDataPath = new LinkedList<>();
        this.columns = new LinkedList<>();
        this.columnDefinitionMap = new HashMap<>();
        keySet = new HashSet<>();
        // floatIndexSet = new HashSet<>();
        colIsFloat = new boolean[5];
        createTBDefine(createTableStatement);
    }

    public void createTBDefine(String statement) {
        // System.out.println(statement);
        // n order to get rid of the row change character
        // statement = statement.replaceAll("\\n", "");
        // statement = statement.replaceAll("`","");
        // because the JSQLParser have problem in parsing the sql statement
        // with the key index and not with the primary key index
        if (statement.contains("KEY") && !statement.contains("PRIMARY")) {
            // It is hard to type the chinese in the ubuntu so only english...
            // JSQLParser has a error in parsing the statement with KEY
            statement = statement.replaceFirst("KEY", "PRIMARY KEY");
            isKey = true;
        }
        try {
            this.createTable = (CreateTable) CCJSqlParserUtil.parse(statement);
            this.name = createTable.getTable().getName();
//            List<Integer> floatIndex = new LinkedList<>();
            int columnCnt = 0;
            for (ColumnDefinition col : this.createTable.getColumnDefinitions()) {
                columns.add(col.getColumnName());
                columnDefinitionMap.put(col.getColumnName(), col);

                if (col.getColDataType().getDataType().toLowerCase(Locale.ROOT).equals("float")) {
                    // floatIndexSet.add(columnCnt);
                    colIsFloat[columnCnt] = true;
                }
                columnCnt++;
            }
            this.columnLen = columnCnt;
//            floatValueIndex = new int[floatIndex.size()];
//            for (int i = 0; i < floatValueIndex.length; i++)
//                floatValueIndex[i] = floatIndex.get(i);

            // get the index of the table
            List<Index> indexList = this.createTable.getIndexes();
            // TODO: test whether the key is added in order
            List<String> keyNameList = new LinkedList<>();
            if (indexList != null) {
                for (Index index : indexList) {
                    String ins = index.getType().toLowerCase(Locale.ROOT);
                    if (ins.contains("key")) {
                        hasKey = true;
                        keyNameList.addAll(index.getColumnsNames());
                    }
                }
            }
            // 有primark key的情况下，除了primary key的所在列全部更新
            // 无primary key的情况下，只更新updated_at参数
            if (hasKey) {
                // initialize the array which store the index data
                keyIndex = new int[keyNameList.size()];
                keyType = new String[keyNameList.size()];

                // construct insert, build, update string for prepared statement
                updateSB = new StringBuilder("update " + name + " set ");
                insertSB = new StringBuilder("insert into " + name + " values (");
                selectSB = new StringBuilder("select updated_at from " + name + " where ");
                columnCnt = 0;
                boolean flag;
                for (String col : columns) {
                    flag = true;
                    for (String s : keyNameList)
                        if (s.equals(col)) {
                            flag = false;
                            break;
                        }
                    if (flag)
                        updateSB.append(col).append("=?, ");
                    insertSB.append("?,");
                    if (name.toLowerCase(Locale.ROOT).contains("updated_at")) {
                        this.updatedatIndex = columnCnt;
                    }
                    columnCnt++;
                }
                // 把updateSB的最后一个逗号删除掉，然后添加where 语句
                updateSB.deleteCharAt(updateSB.length() - 1);
                updateSB.append(" where ");
                // updateSB.append(" where ").append(primaryStat).append(";");
                // the last ; would like to lead error in the batch execute
                // 把insertSB的最后一个逗号删除掉，然后添加) 语句
                insertSB.deleteCharAt(insertSB.length() - 1);
                insertSB.append(")");

                int keyIndextmp = 0;
                for (String s : keyNameList) {
                    if (keyIndextmp == 0) {
                        selectSB.append(s).append(" =?");
                        updateSB.append(s).append(" =?");
                    } else {
                        selectSB.append(" and ").append(s).append(" =?");
                        updateSB.append(" and ").append(s).append(" =?");
                    }
                    int index = 0;
                    for (ColumnDefinition col : this.createTable.getColumnDefinitions()) {
                        if (s.equals(col.getColumnName())) {
                            keySet.add(index);
                            keyIndex[keyIndextmp] = index;
                            keyType[keyIndextmp++] = col.getColDataType().getDataType();
                        }
                        index++;
                    }
                }
            } else {
                // do not have the key
                selectSB = new StringBuilder("select updated_at from " + this.name + " where ");
                updateSB = new StringBuilder("update " + this.name + " set updated_at = ? where ");
                insertSB = new StringBuilder("insert into " + this.name + " values (");
                int cnt = 0;
                for (String name : this.columns) {
                    insertSB.append("?,");
                    if (name.toLowerCase(Locale.ROOT).contains("updated_at")) {
                        this.updatedatIndex = cnt;
                    } else {
                        selectSB.append(name).append("=? and");
                        updateSB.append(name).append("=? and");
                    }
                    cnt++;
                }
                // 把insertSB的最后一个逗号删除掉，然后添加)
                insertSB.deleteCharAt(insertSB.length() - 1);
                insertSB.append(")");
                // 把selectSB delete final `and`
                // selectSB.deleteCharAt(selectSB.length() - 1);
                selectSB.delete(selectSB.length() - 3, selectSB.length());
                // 把updateSB delete final `and`
                updateSB.delete(updateSB.length() - 3, updateSB.length());
                // updateSB.deleteCharAt(updateSB.length() - 1);
            }
        } catch (JSQLParserException e) {
            System.out.println(statement);
            e.printStackTrace();
        }
    }

    public void addTBDefine(String createStatement) {
        if (createStatement.contains("KEY") && !createStatement.contains("PRIMARY")) {
            // It is hard to type the chinese in the ubuntu so only english...
            // JSQLParser has a error in parsing the statement with KEY
            createStatement = createStatement.replaceFirst("KEY", "PRIMARY KEY");
            isKey = true;
        }
        // System.out.println(createStatement);
        try {
            CreateTable otherCreateTable = (CreateTable) CCJSqlParserUtil.parse(createStatement);
            for (ColumnDefinition col : otherCreateTable.getColumnDefinitions()) {
                String colName = col.getColumnName();
                if (columnDefinitionMap.containsKey(colName) && compareColumnType(columnDefinitionMap.get(colName), col)) {
                    columnDefinitionMap.put(colName, col);
                }
            }
        } catch (JSQLParserException e) {
            System.out.println(createStatement);
            e.printStackTrace();
        }
    }

    public boolean compareColumnType(ColumnDefinition orig, ColumnDefinition newO) {
        // getDataType 是直接获得Data的Type没有后缀，既是没有后面的关于字符类似char长度的字段， 比较的时候需要注意
        // 获取类型的前缀
        String origS = orig.getColDataType().getDataType().toUpperCase(), newOS = newO.getColDataType().getDataType().toUpperCase();
        if ("INTEGER".equals(origS))
            origS = "INT";
        if ("INTEGER".equals(newOS))
            newOS = "INT";
        // 个人愚见，Date类型的数据差距太大，压根就不是精度问题了，如果采用了不同的Date类型，这两个类型的数据一定是无法成功合并的
        // 肯定不存在相同表下面的列数据类型不同的情况，那样肯定是错的
        if (SQLNumberType.contains(origS) && SQLNumberType.contains(newOS)) {
            SQLNumberType origT = SQLNumberType.valueOf(origS);
            SQLNumberType newOT = SQLNumberType.valueOf(newOS);
            if (origT.ordinal() > newOT.ordinal()) {
                return false;
            } else if (origT.ordinal() < newOT.ordinal()) {
                return true;
            } else if (origT == SQLNumberType.DECIMAL) {
                // 比较他们的参数，优先选择参数值大的 DECIMAL 含有两个参数 M N， 选择M大的, DECIMAL一定会有参数么？不一定，还得判断下
                // 是给定参数的取值范围大，还是未给定的参数范围大？
                // 两者中都取最大的
                List<String> origPar = orig.getColDataType().getArgumentsStringList();
                List<String> newOPar = newO.getColDataType().getArgumentsStringList();
                if (origPar == null && newOPar != null)
                    return true;
                else if (origPar != null && newOPar == null)
                    return false;
                else if (origPar == null)
                    return false;
                int[] o = new int[2], n = new int[2];
                int cnt = 0;
                for (String s : origPar) {
                    o[cnt++] = Integer.parseInt(s);
                }
                cnt = 0;
                for (String s : newOPar) {
                    n[cnt++] = Integer.parseInt(s);
                }
                o[0] = Math.max(o[0], n[0]);
                o[1] = Math.max(o[1], n[1]);
                List<String> newPar = new LinkedList<>();
                newPar.add(String.valueOf(o[0]));
                newPar.add(String.valueOf(o[1]));
                orig.getColDataType().setArgumentsStringList(newPar);
            }
            return false;
        } else if (SQLStringType.contains(origS) && SQLStringType.contains(newOS)) {
            // 如果存在参数列表就用参数列表中值大的，如果没有参数列表，就按表中的顺序来， String类型的参数要么只有一个，要么没有
            List<String> origPar = orig.getColDataType().getArgumentsStringList();
            List<String> newOPar = newO.getColDataType().getArgumentsStringList();
            SQLStringType origT = SQLStringType.valueOf(origS);
            SQLStringType newOT = SQLStringType.valueOf(newOS);
            if (origPar != null && newOPar != null) {
                // 如果都有长度要求，直接设置长度为最长得到那一个
                int o = Integer.parseInt(origPar.get(0)), n = Integer.parseInt(newOPar.get(0));
                return o < n;
            } else if (origPar != null) {
                // 如果只有original存在长度要求
                int o = Integer.parseInt(origPar.get(0));
                return true;
            } else if (newOPar != null) {
                // 如果只有新来的由长度要求
                return false;
            } else {
                // 如果没有下标要求，直接比较他们在index的下标位置
                return origT.ordinal() < newOT.ordinal();
            }
        }
        return false;
    }

    // precision of mysql float type is a big problem
    // try to use the long type as the hash value to low down the time cost in the selecting statement
    // MAX VALUE of long is 9223372036854775807 19 digits   64 bits 8 Bytes -> 16 Bytes
    // MAX VALUE of Integer is 2147483647 10 digits         32 bits 4 Bytes -> 16 Bytes
    // the memory of the long and integer is the same
    // use the 18 digits as the hash value in order to avoid overflowing
    // TODO: memory limit!!! use the long value server as the hash value may lead to OOM!
    public Long getHashValue(String[] data) {
        int a = Integer.MAX_VALUE;
        long b = Long.MAX_VALUE;
        StringBuilder sb = new StringBuilder();
        if (hasKey) {
            for (int i : keyIndex) {
                for (Character ch : data[i].toCharArray()) {
                    if (ch >= '0' && ch <= '9') {
                        sb.append(ch);
                        if (sb.length() == 18)
                            return Long.valueOf(sb.toString());
                    }
                }
            }
        } else {
            for (int i = 0; i < data.length; i++) {
                if (i != this.updatedatIndex) {
                    for(Character ch : data[i].toCharArray()){
                        if(ch >= '0' && ch <= '9'){
                            sb.append(ch);
                            if (sb.length() == 18)
                                return Long.valueOf(sb.toString());
                        }
                    }
                }
            }
        }
        while (sb.length() < 18)
            sb.append("0");
        return Long.valueOf(sb.toString());
    }


    public boolean getHashValueBool(String[] data){
        return true;
    }
}
