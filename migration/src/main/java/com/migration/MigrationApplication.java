package com.migration;

import org.springframework.boot.SpringApplication;
import java.sql.*;
public class MigrationApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(MigrationApplication.class, args);
		
		Connection conn = null;
		ResultSet rst = null;
		PreparedStatement psmt = null;
		
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			String url = "jdbc:oracle:thin:@localhost:1521:orcl";
			String id = "system";
			String pw = "7564";
			System.out.println("DB정상연결");
			try {
				conn = DriverManager.getConnection(url,id,pw);
				System.out.println("DB계정일치");
			} catch (SQLException e) {
				System.out.println("DB계정불일치");
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			System.out.println("DB연결실패");
			e.printStackTrace();
		}
		
		try {
			String sql = "SELECT table_name AS tableName, tablespace_name AS tablespaceName FROM user_tables";
			
			psmt = conn.prepareStatement(sql);
			rst = psmt.executeQuery();
			while(rst.next()) {
				String a = rst.getString("tableName");
				String b = rst.getString("tablespaceName");
				System.out.println("deptno은 "+a+"deptname은 "+b);
			}
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("쿼리실패");
		}
	}

}
