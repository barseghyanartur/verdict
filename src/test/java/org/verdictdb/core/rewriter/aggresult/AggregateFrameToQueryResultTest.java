package org.verdictdb.core.rewriter.aggresult;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verdictdb.connection.JdbcQueryResult;
import org.verdictdb.core.aggresult.AggregateFrame;
import org.verdictdb.core.aggresult.AggregateFrameQueryResult;
import org.verdictdb.exception.VerdictDBValueException;

public class AggregateFrameToQueryResultTest {
  static Connection conn;

  @BeforeClass
  public static void setupH2Database() throws SQLException {
    final String DB_CONNECTION = "jdbc:h2:mem:~/test;DB_CLOSE_DELAY=-1";
    final String DB_USER = "";
    final String DB_PASSWORD = "";
    conn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
  }

  @AfterClass
  public static void closeH2Connection() throws SQLException {
    conn.close();
  }

  @Test
  public void testCountQueryToAggregateFrame() throws SQLException, VerdictDBValueException {
    List<List<Object>> contents = new ArrayList<>();
    contents.add(Arrays.<Object>asList(1, "Anju", "female"));
    contents.add(Arrays.<Object>asList(2, "Sonia", "female"));
    contents.add(Arrays.<Object>asList(3, "Asha", "male"));
    contents.add(Arrays.<Object>asList(3, "Joe", "male"));
    contents.add(Arrays.<Object>asList(3, "JoJo", "male"));
    Statement stmt = conn.createStatement();
    stmt.execute("DROP TABLE PEOPLE IF EXISTS");
    stmt.execute("CREATE TABLE PEOPLE(id int, name varchar(255), gender varchar(8))");
    for (List<Object> row : contents) {
      String id = row.get(0).toString();
      String name = row.get(1).toString();
      String gender = row.get(2).toString();
      stmt.execute(String.format("INSERT INTO PEOPLE(id, name, gender) VALUES(%s, '%s', '%s')", id, name, gender));
    }
    ResultSet rs = stmt.executeQuery("SELECT gender, count(*) as cnt FROM PEOPLE GROUP BY gender");
    JdbcQueryResult queryResult = new JdbcQueryResult(rs);
    List<String> nonAgg = new ArrayList<>();
    List<AggNameAndType> agg = new ArrayList<>();
    nonAgg.add("GENDER");
    agg.add(new AggNameAndType("CNT", "COUNT"));
    AggregateFrame aggregateFrame = AggregateFrame.fromDmbsQueryResult(queryResult, nonAgg, agg);

    AggregateFrameQueryResult aggregateFrameQueryResult = (AggregateFrameQueryResult) aggregateFrame.toDbmsQueryResult();
    assertEquals(2, aggregateFrameQueryResult.getColumnCount());
    assertEquals("gender", aggregateFrameQueryResult.getColumnName(0));
    assertEquals("cnt", aggregateFrameQueryResult.getColumnName(1));
    while (aggregateFrameQueryResult.next()){
      if (aggregateFrameQueryResult.getValue(0).equals("male")){
        assertEquals(new Long(3), aggregateFrameQueryResult.getValue(1));
      }
      else assertEquals(new Long(2), aggregateFrameQueryResult.getValue(1));
    }
  }

  @Test
  public void testSumQueryToAggregateFrame() throws SQLException, VerdictDBValueException {
    List<List<Object>> contents = new ArrayList<>();
    contents.add(Arrays.<Object>asList(1, "Anju", "female"));
    contents.add(Arrays.<Object>asList(2, "Sonia", "female"));
    contents.add(Arrays.<Object>asList(3, "Asha", "male"));
    contents.add(Arrays.<Object>asList(3, "Joe", "male"));
    contents.add(Arrays.<Object>asList(3, "JoJo", "male"));
    Statement stmt = conn.createStatement();
    stmt.execute("DROP TABLE PEOPLE IF EXISTS");
    stmt.execute("CREATE TABLE PEOPLE(id int, name varchar(255), gender varchar(8))");
    for (List<Object> row : contents) {
      String id = row.get(0).toString();
      String name = row.get(1).toString();
      String gender = row.get(2).toString();
      stmt.execute(String.format("INSERT INTO PEOPLE(id, name, gender) VALUES(%s, '%s', '%s')", id, name, gender));
    }
    ResultSet rs = stmt.executeQuery("SELECT gender, sum(id) as s FROM PEOPLE GROUP BY gender");
    JdbcQueryResult queryResult = new JdbcQueryResult(rs);
    List<String> nonAgg = new ArrayList<>();
    List<AggNameAndType> agg = new ArrayList<>();
    nonAgg.add("GENDER");
    agg.add(new AggNameAndType("S", "COUNT"));
    AggregateFrame aggregateFrame = AggregateFrame.fromDmbsQueryResult(queryResult, nonAgg, agg);

    AggregateFrameQueryResult aggregateFrameQueryResult = (AggregateFrameQueryResult) aggregateFrame.toDbmsQueryResult();
    assertEquals(2, aggregateFrameQueryResult.getColumnCount());
    assertEquals("gender", aggregateFrameQueryResult.getColumnName(0));
    assertEquals("s", aggregateFrameQueryResult.getColumnName(1));
    while (aggregateFrameQueryResult.next()){
      if (aggregateFrameQueryResult.getValue(0).equals("male")){
        assertEquals(new Long(9), aggregateFrameQueryResult.getValue(1));
      }
      else assertEquals(new Long(3), aggregateFrameQueryResult.getValue(1));
    }
  }

  @Test
  public void testCountQueryToAggregateFrame2() throws SQLException, VerdictDBValueException {
    List<List<Object>> contents = new ArrayList<>();
    contents.add(Arrays.<Object>asList(1, "Anju", "female", 15, 170, "USA"));
    contents.add(Arrays.<Object>asList(2, "Sonia", "female", 17, 156, "USA"));
    contents.add(Arrays.<Object>asList(3, "Asha", "male", 23, 168, "CHN"));
    contents.add(Arrays.<Object>asList(3, "Joe", "male", 14, 178, "USA"));
    contents.add(Arrays.<Object>asList(3, "JoJo", "male", 18, 190, "CHN"));
    contents.add(Arrays.<Object>asList(3, "Sam", "male", 18, 190, "USA"));
    contents.add(Arrays.<Object>asList(3, "Alice", "female", 18, 190, "CHN"));
    contents.add(Arrays.<Object>asList(3, "Bob", "male", 18, 190, "CHN"));
    Statement stmt = conn.createStatement();
    stmt.execute("DROP TABLE PEOPLE IF EXISTS");
    stmt.execute("CREATE TABLE PEOPLE(id int, name varchar(255), gender varchar(8), age int, height int, nation varchar(8))");
    for (List<Object> row : contents) {
      String id = row.get(0).toString();
      String name = row.get(1).toString();
      String gender = row.get(2).toString();
      String age = row.get(3).toString();
      String height = row.get(4).toString();
      String nation = row.get(5).toString();
      stmt.execute(String.format("INSERT INTO PEOPLE(id, name, gender, age, height, nation) VALUES(%s, '%s', '%s', %s, %s, '%s')", id, name, gender, age, height, nation));
    }
    ResultSet rs = stmt.executeQuery("SELECT gender, count(*) as cnt, sum(age) as agesum FROM PEOPLE GROUP BY gender");
    JdbcQueryResult queryResult = new JdbcQueryResult(rs);
    List<String> nonAgg = new ArrayList<>();
    List<AggNameAndType> agg = new ArrayList<>();
    nonAgg.add("GENDER");
    agg.add(new AggNameAndType("CNT", "COUNT"));
    agg.add(new AggNameAndType("AGESUM", "SUM"));
    AggregateFrame aggregateFrame = AggregateFrame.fromDmbsQueryResult(queryResult, nonAgg, agg);

    AggregateFrameQueryResult aggregateFrameQueryResult = (AggregateFrameQueryResult) aggregateFrame.toDbmsQueryResult();
    assertEquals(3, aggregateFrameQueryResult.getColumnCount());
    assertEquals("gender", aggregateFrameQueryResult.getColumnName(0));
    assertEquals("cnt", aggregateFrameQueryResult.getColumnName(1));
    assertEquals("agesum", aggregateFrameQueryResult.getColumnName(2));
    while (aggregateFrameQueryResult.next()){
      if (aggregateFrameQueryResult.getValue(0).equals("male")){
        assertEquals(new Long(5), aggregateFrameQueryResult.getValue(1));
        assertEquals(new Long(91), aggregateFrameQueryResult.getValue(2));
      }
      else {
        assertEquals(new Long(3), aggregateFrameQueryResult.getValue(1));
        assertEquals(new Long(50), aggregateFrameQueryResult.getValue(2));
      }
    }
  }

  @Test
  public void testCountQueryToAggregateFrame3() throws SQLException, VerdictDBValueException {
    List<List<Object>> contents = new ArrayList<>();
    contents.add(Arrays.<Object>asList(1, "Anju", "female", 15, 170, "USA"));
    contents.add(Arrays.<Object>asList(2, "Sonia", "female", 17, 156, "USA"));
    contents.add(Arrays.<Object>asList(3, "Asha", "male", 23, 168, "CHN"));
    contents.add(Arrays.<Object>asList(3, "Joe", "male", 14, 178, "USA"));
    contents.add(Arrays.<Object>asList(3, "JoJo", "male", 18, 190, "CHN"));
    contents.add(Arrays.<Object>asList(3, "Sam", "male", 18, 190, "USA"));
    contents.add(Arrays.<Object>asList(3, "Alice", "female", 18, 190, "CHN"));
    contents.add(Arrays.<Object>asList(3, "Bob", "male", 18, 190, "CHN"));
    Statement stmt = conn.createStatement();
    stmt.execute("DROP TABLE PEOPLE IF EXISTS");
    stmt.execute("CREATE TABLE PEOPLE(id int, name varchar(255), gender varchar(8), age double, height int, nation varchar(8))");
    for (List<Object> row : contents) {
      String id = row.get(0).toString();
      String name = row.get(1).toString();
      String gender = row.get(2).toString();
      String age = row.get(3).toString();
      String height = row.get(4).toString();
      String nation = row.get(5).toString();
      stmt.execute(String.format("INSERT INTO PEOPLE(id, name, gender, age, height, nation) VALUES(%s, '%s', '%s', %s, %s, '%s')", id, name, gender, age, height, nation));
    }
    ResultSet rs = stmt.executeQuery("SELECT sum(age) as agesum, gender, count(*) as cnt, nation FROM PEOPLE GROUP BY gender, nation");
    while (rs.next()){
     rs.getString(1);
    }

    JdbcQueryResult queryResult = new JdbcQueryResult(rs);
    List<String> nonAgg = new ArrayList<>();
    List<AggNameAndType> agg = new ArrayList<>();
    nonAgg.add("GENDER");
    nonAgg.add("NATION");
    agg.add(new AggNameAndType("CNT", "COUNT"));
    agg.add(new AggNameAndType("AGESUM", "SUM"));
    AggregateFrame aggregateFrame = AggregateFrame.fromDmbsQueryResult(queryResult, nonAgg, agg);

    AggregateFrameQueryResult aggregateFrameQueryResult = (AggregateFrameQueryResult) aggregateFrame.toDbmsQueryResult();
    assertEquals(4, aggregateFrameQueryResult.getColumnCount());
    assertEquals("agesum", aggregateFrameQueryResult.getColumnName(0));
    assertEquals("gender", aggregateFrameQueryResult.getColumnName(1));
    assertEquals("cnt", aggregateFrameQueryResult.getColumnName(2));
    assertEquals("nation", aggregateFrameQueryResult.getColumnName(3));
    while (aggregateFrameQueryResult.next()) {
      if (aggregateFrameQueryResult.getValue(1).equals("male") &&
          aggregateFrameQueryResult.getValue(3).equals("CHN")) {
        assertEquals(new Long(59), aggregateFrameQueryResult.getValue(0));
        assertEquals(new Long(3), aggregateFrameQueryResult.getValue(2));
      }
      else if (aggregateFrameQueryResult.getValue(1).equals("female") &&
          aggregateFrameQueryResult.getValue(3).equals("CHN")) {
        assertEquals(new Long(18), aggregateFrameQueryResult.getValue(0));
        assertEquals(new Long(1), aggregateFrameQueryResult.getValue(2));
      }
      else if (aggregateFrameQueryResult.getValue(1).equals("female") &&
          aggregateFrameQueryResult.getValue(3).equals("USA")) {
        assertEquals(new Long(32), aggregateFrameQueryResult.getValue(0));
        assertEquals(new Long(2), aggregateFrameQueryResult.getValue(2));
      }
      else {
        assertEquals(new Long(32), aggregateFrameQueryResult.getValue(0));
        assertEquals(new Long(2), aggregateFrameQueryResult.getValue(2));
      }
    }
  }
}
