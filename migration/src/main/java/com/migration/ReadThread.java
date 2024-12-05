package com.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReadThread extends Thread {
	DBDriver dbDriver = new DBDriver();

	Connection conn = dbDriver.dbDriverConn("jdbc:oracle:thin:@localhost:1521:orcl", "system", "7564");
	PreparedStatement sPsmt = null;
	ResultSet rst = null;
	@Override
	public void run() {

		try {
			String sql = "SELECT COUNT(*) FROM TEST.EMPLOYEE";
			sPsmt = conn.prepareStatement(sql);
			rst = sPsmt.executeQuery();
			if(rst.next()) {
				System.out.println(rst.getInt(1));
			}
			WriteThread writeT = new WriteThread();
			writeT.setDaemon(true);
			writeT.start();
			for (int i = 0; i < rst.getInt(1); i++) {
				System.out.print("R");
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}

	}
}
