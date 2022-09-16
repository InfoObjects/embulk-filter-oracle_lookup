package org.embulk.filter.oracle_lookup;

import java.sql.Connection;
import java.sql.DriverManager;

public class OracleConnection {

    private static Connection connection=null;

    private OracleConnection(OracleLookupFilterPlugin.PluginTask task) throws Exception {
        try{
            Class.forName("oracle.jdbc.driver.OracleDriver");
            String url = "jdbc:oracle:thin:@" + task.getHost() + ":"+task.getPort()+":"+task.getDatabase();
            connection= DriverManager.getConnection(url, task.getUserName(), task.getPassword());
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }

    }

    public static Connection getConnection(OracleLookupFilterPlugin.PluginTask task){
        if(connection==null){
            try {
                new OracleConnection(task);
                return connection;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException();
            }
        }
        return connection;
    }
}
