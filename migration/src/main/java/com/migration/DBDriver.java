package com.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBDriver {
	Connection dbDriverConn(String url, String id, String pw) {
		Connection conn = null;
		try {
			if(url.contains("oracle")) {
				Class.forName("oracle.jdbc.driver.OracleDriver");
			} else if(url.contains("postgresql")) {
				Class.forName("org.postgresql.Driver");
			}
			System.out.println("SourceDB정상연결");
			try {
				conn = DriverManager.getConnection(url, id, pw);
				System.out.println("SourceDB계정일치");
			} catch (SQLException e) {
				System.out.println("SourceDB계정불일치");
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			System.out.println("SourceDB연결실패");
			e.printStackTrace();
		}
		return conn;
	}
}
