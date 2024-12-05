package com.migration;

import java.sql.Connection;

public class WriteThread extends Thread {
	DBDriver dbDriver = new DBDriver();

	Connection conn = dbDriver.dbDriverConn("jdbc:postgresql://localhost:5432/postgres", "postgres", "7564");

	@Override
	public void run() {
		while(true) {
			System.out.print("1");
		}
	}
}
