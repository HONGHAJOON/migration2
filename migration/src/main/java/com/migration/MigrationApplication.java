package com.migration;

import java.sql.*;
import java.util.LinkedList;
import java.util.Queue;

public class MigrationApplication {
	
	private static final String SRC_DB_URL = "jdbc:oracle:thin:@localhost:1521:orcl"; // 소스 DB URL
    private static final String SRC_DB_USER = "system";
    private static final String SRC_DB_PASSWORD = "7564";

    private static final String TARGET_DB_URL = "jdbc:postgresql://localhost:5432/postgres"; // 타겟 DB URL
    private static final String TARGET_DB_USER = "postgres";
    private static final String TARGET_DB_PASSWORD = "7564";

    private static final String TABLE_NAME = "TEST.EMPLOYEE";
    private static final int BUFFER_SIZE = 5; // 최대 버퍼 크기

    private static final Queue<Object[]> buffer = new LinkedList(); // 데이터를 저장하는 버퍼
    private static boolean isDataFetchingComplete = false; // 데이터 가져오기 완료 여부 플래그
    private static final Object lock = new Object(); // 동기화를 위한 잠금 객체

    public static void main(String[] args) {
        Thread producer = new Thread(new DataFetcher());
        Thread consumer = new Thread(new DataInserter());

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
        }

        System.out.println("Data migration completed.");
    }

    // 데이터를 소스 데이터베이스에서 가져오는 스레드
    static class DataFetcher implements Runnable {
        @Override
        public void run() {
            try (Connection sourceConnection = DriverManager.getConnection(SRC_DB_URL, SRC_DB_USER, SRC_DB_PASSWORD)) {
                System.out.println("Connected to source database.");

                String selectQuery = "SELECT * FROM " + TABLE_NAME;
                try (PreparedStatement selectStmt = sourceConnection.prepareStatement(selectQuery);
                     ResultSet resultSet = selectStmt.executeQuery()) {

                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    System.out.println(columnCount);
                    while (resultSet.next()) {
                        synchronized (lock) {
                            while (buffer.size() >= BUFFER_SIZE) {
                                lock.wait(); // 버퍼가 가득 찼을 경우 대기
                            }

                            Object[] row = new Object[columnCount];
                            for (int i = 1; i <= columnCount; i++) {
                                row[i - 1] = resultSet.getObject(i);
                            }
                            buffer.add(row);
                            lock.notifyAll(); // 소비자 스레드에 신호 보내기
                        }
                    }
                }

                synchronized (lock) {
                    isDataFetchingComplete = true; // 데이터 가져오기 완료 플래그 설정
                    lock.notifyAll(); // 소비자 스레드 깨우기
                }

                System.out.println("Data fetching completed.");
            } catch (SQLException | InterruptedException e) {
                System.err.println("Error in DataFetcher: " + e.getMessage());
            }
        }
    }

    // 데이터를 타겟 데이터베이스에 삽입하는 스레드
    static class DataInserter implements Runnable {
        @Override
        public void run() {
            try (Connection targetConnection = DriverManager.getConnection(TARGET_DB_URL, TARGET_DB_USER, TARGET_DB_PASSWORD)) {
                System.out.println("Connected to target database.");

                String insertQuery = createInsertQuery("employee", targetConnection);
                try (PreparedStatement insertStmt = targetConnection.prepareStatement(insertQuery)) {
                    while (true) {
                        Object[] row;
                        synchronized (lock) {
                            while (buffer.isEmpty() && !isDataFetchingComplete) {
                                lock.wait(); // 버퍼가 비어있을 경우 대기
                            }

                            if (buffer.isEmpty() && isDataFetchingComplete) {
                                break; // 데이터 가져오기와 버퍼 처리가 모두 끝났으면 종료
                            }

                            row = buffer.poll();
                            lock.notifyAll(); // 생산자 스레드에 신호 보내기
                        }

                        // 데이터를 삽입
                        if (row != null) {
                            for (int i = 0; i < row.length; i++) {
                                insertStmt.setObject(i + 1, row[i]);
                            }
                            insertStmt.executeUpdate();
                        }
                    }
                }

                System.out.println("Data insertion completed.");
            } catch (SQLException | InterruptedException e) {
                System.err.println("Error in DataInserter: " + e.getMessage());
            }
        }

        private String createInsertQuery(String tableName, Connection connection) throws SQLException {
            String query = "SELECT * FROM " + tableName + " WHERE 1=0"; // 테이블 구조만 가져오기 위한 쿼리
            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet resultSet = stmt.executeQuery()) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();

                for (int i = 1; i <= columnCount; i++) {
                    columns.append(metaData.getColumnName(i));
                    values.append("?");
                    if (i < columnCount) {
                        columns.append(", ");
                        values.append(", ");
                    }
                }

                return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
            }
        }
    }
//	
//	static long startTime = 0;
//	
//	
//
//	public static void main(String[] args) {
//		SpringApplication.run(MigrationApplication.class, args);
//		// 1. Source, Target Database 연결확인 : 필요한 파라미터(DB주소, id, pw)
//		// -Target DB 연결 : 실패시 예외처리(??동시에 두개의 DB연결 가능한지)
//		// -Target DB 연결해제
//		// -Source DB 연결 : 실패시 예외처리
////		Connection sConn = app.dbDriver("jdbc:oracle:thin:@localhost:1521:orcl", "system", "7564");; // 2개씩
////		Connection tConn = app.dbDriver("jdbc:postgresql://localhost:5432/postgres", "postgres", "7564");;
//		ResultSet rst = null;
//		PreparedStatement sPsmt = null; // 2개씩
//		PreparedStatement tPsmt = null;
//		
//		
//		// 2. Table 생성 : 필요한 파라미터(Table명)
//		// -추출 테이블 컬럼명, 컬럼타입, key값 (나머지 속성들?) 조회
//		// -Target DB에 테이블 생성(세부 속성 설정)
//
//		// 3. Data 삽입
//		// -테이블이 하나일때 : SourceDB top-n 쿼리조회 및 TargetDB삽입 방식반복(속도 개선)
//		// -키가 연결된 테이블이 두개 이상일 때 : join후 groupby시켜 top-n 쿼리조회 및 TargetDB 삽입 방식반복
//		//
//
//		// 4. 성공 여부 확인
//		// ?데이터 하나하나 비교?
//		// 항시 예외시 롤백, 최종성공시 커밋
//		
//		startTime = System.currentTimeMillis();
//		
//		ReadThread readT=new ReadThread();
////		WriteThread writeT=new WriteThread();
//		
//		
//		readT.start();
////		writeT.start();
//		
//		System.out.println("소요시간 : "+(System.currentTimeMillis() - startTime));
////		try {
////			String sql = "SELECT * FROM TEST.EMPLOYEE";
////			sPsmt = sConn.prepareStatement(sql);
////			rst = sPsmt.executeQuery();
////			ResultSetMetaData rsmd = rst.getMetaData();
////			int columnCount = rsmd.getColumnCount();
////			System.out.println(rst.getType());
////			
////			//Prepare Insert Statement
////			StringBuilder insertSql = new StringBuilder("INSERT INTO employee (");
////            for (int i = 1; i <= columnCount; i++) {
////                insertSql.append(rsmd.getColumnName(i));
////                if (i < columnCount) insertSql.append(", ");
////            }
////            insertSql.append(") VALUES (");
////            for (int i = 1; i <= columnCount; i++) {
////                insertSql.append("?");
////                if (i < columnCount) insertSql.append(", ");
////            }
////            insertSql.append(")");
////            tPsmt = tConn.prepareStatement(insertSql.toString());
////
////            // Insert Data into TargetDB
////            while (rst.next()) {
////                for (int i = 1; i <= columnCount; i++) {
////                    int columnType = rsmd.getColumnType(i);
////                    if (columnType == Types.NUMERIC && rsmd.getScale(i) == 0) {
////                        tPsmt.setInt(i, rst.getInt(i));
////                    } else if (columnType == Types.NUMERIC) {
////                        tPsmt.setDouble(i, rst.getDouble(i));
////                    } else if (columnType == Types.VARCHAR || columnType == Types.CLOB) {
////                        tPsmt.setString(i, rst.getString(i));
////                    } else if (columnType == Types.DATE || columnType == Types.TIMESTAMP) {
////                        tPsmt.setDate(i, rst.getDate(i));
////                    }
////                }
////                tPsmt.executeUpdate(); // Insert row into target table
////            }
////            System.out.println("Data migration completed successfully.");
////		} catch (Exception e) {
////			e.printStackTrace();
////			System.out.println("쿼리실패");
////		} finally {
////            try {
////                if (rst != null) rst.close();
////                if (sPsmt != null) sPsmt.close();
////                if (tPsmt != null) tPsmt.close();
////                if (sConn != null) sConn.close();
////                if (tConn != null) tConn.close();
////            } catch (SQLException e) {
////                e.printStackTrace();
////            }
////        }
//	}
}
