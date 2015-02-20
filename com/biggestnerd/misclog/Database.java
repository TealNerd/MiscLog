package com.biggestnerd.misclog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {

	String host = "localhost";
	int port = 3306;
	String db = "MiscLog";
	String user = "root";
	String password = "pass";
	private Connection connection;
	
	public boolean connect() {
		String jdbc = "jdbc:mysql://" + host + ":" + port + "/";
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception ex) {
        	System.out.println("Failed to initialize JDBC driver.");
        	ex.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(jdbc, user, password);
            System.out.println("Connected to database!");
            return true;
        } catch (SQLException ex) { //Error handling below:
        	System.out.println("Could not connnect to the database!");
        	ex.printStackTrace();
            return false;
        }
	}
	
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
        	System.out.println("An error occured while closing the connection.");
        }
    }

    public boolean isConnected() {
        try {
            return connection.isValid(5);
        } catch (SQLException ex) {
        	System.out.println("isConnected error!");
        }
        return false;
    }

    public PreparedStatement prepareStatement(String sqlStatement) {
        try {
            return connection.prepareStatement(sqlStatement);
        } catch (SQLException ex) {
        	System.out.println("Failed to prepare statement! " + sqlStatement);
        }
        return null;
    }
    
    public void execute(String sql) {
        try {
            if (isConnected()) {
            	connection.prepareStatement("USE MiscLog").executeUpdate();
                connection.prepareStatement(sql).executeUpdate();
            } else {
                connect();
                execute(sql);
            }
        } catch (SQLException ex) {
        	System.out.println("Could not execute SQL statement! " + ex);
        }
    }
    
    public void sendSnitch(String player, int x, int y, int z, String world, String name) {
    	java.util.Date javaDate = new java.util.Date();
    	java.sql.Date sqlDate = new java.sql.Date(javaDate.getTime());
    	execute("INSERT INTO Snitches (player, x, y, z, world, name) values ('" + player + "', '" + x + "', '" +
    	 y + "', '" + z + "', '" + world + "', '" + name + "');");
    }
}
