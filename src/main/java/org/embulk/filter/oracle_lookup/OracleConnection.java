package org.embulk.filter.oracle_lookup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        try {
            if(connection==null || connection.isClosed()){
                try {
                    new OracleConnection(task);
                    return connection;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException();
                }
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        return connection;
    }
}
