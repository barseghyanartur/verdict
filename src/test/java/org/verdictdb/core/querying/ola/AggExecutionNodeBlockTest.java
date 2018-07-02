package org.verdictdb.core.querying.ola;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verdictdb.core.connection.JdbcConnection;
import org.verdictdb.core.execution.ExecutablePlanRunner;
import org.verdictdb.core.querying.AggExecutionNode;
import org.verdictdb.core.querying.BaseQueryNode;
import org.verdictdb.core.querying.QueryExecutionPlan;
import org.verdictdb.core.querying.SimpleTreePlan;
import org.verdictdb.core.scrambling.ScrambleMeta;
import org.verdictdb.core.scrambling.ScrambleMetaForTable;
import org.verdictdb.core.scrambling.UniformScrambler;
import org.verdictdb.core.sqlobject.AliasedColumn;
import org.verdictdb.core.sqlobject.BaseTable;
import org.verdictdb.core.sqlobject.ColumnOp;
import org.verdictdb.core.sqlobject.CreateTableAsSelectQuery;
import org.verdictdb.core.sqlobject.SelectQuery;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.exception.VerdictDBValueException;
import org.verdictdb.sqlreader.QueryToSql;
import org.verdictdb.sqlsyntax.H2Syntax;

public class AggExecutionNodeBlockTest {
  
  static Connection conn;
  
  static String originalSchema = "originalschema";

  static String originalTable = "originalschema";

  static String newSchema = "newschema";

  static String newTable = "newtable";
  
  static ScrambleMeta scrambleMeta = new ScrambleMeta();
  
  static int aggBlockCount = 3;
  
  @BeforeClass
  public static void setupH2Database() throws SQLException, VerdictDBException {
    final String DB_CONNECTION = "jdbc:h2:mem:aggexecnodeblocktest;DB_CLOSE_DELAY=-1";
    final String DB_USER = "";
    final String DB_PASSWORD = "";
    conn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
    conn.createStatement().execute(String.format("CREATE SCHEMA \"%s\"", originalSchema));
    conn.createStatement().execute(String.format("CREATE SCHEMA \"%s\"", newSchema));
    populateRandomData(conn, originalSchema, originalTable);
    
    // create scrambled table
    UniformScrambler scrambler =
        new UniformScrambler(originalSchema, originalTable, newSchema, newTable, aggBlockCount);
    CreateTableAsSelectQuery createQuery = scrambler.createQuery();
    String scrambleSql = QueryToSql.convert(new H2Syntax(), createQuery);
    conn.createStatement().execute(scrambleSql);
    ScrambleMetaForTable metaEntry = scrambler.generateMeta();
    scrambleMeta.insertScrambleMetaEntry(metaEntry);
  }
  
  @AfterClass
  public static void closeH2Connection() throws SQLException {
    conn.close();
  }

  @Test
  public void testConvertFlatToProgressiveAgg() throws VerdictDBException {
//    System.out.println("test case starts");
    SelectQuery aggQuery = SelectQuery.create(
        new AliasedColumn(ColumnOp.count(), "agg"),
        new BaseTable(newSchema, newTable, "t"));
    QueryExecutionPlan plan = new QueryExecutionPlan(newSchema);
    plan.setScrambleMeta(scrambleMeta);
    
    AggExecutionNode aggnode = AggExecutionNode.create(plan, aggQuery);
    AggExecutionNodeBlock block = new AggExecutionNodeBlock(plan, aggnode);
    BaseQueryNode converted = block.convertToProgressiveAgg(plan.getScrambleMeta());   // AsyncAggregation
    converted.print();
    assertTrue(converted instanceof AsyncAggExecutionNode);
    
    assertTrue(converted.getDependent(0) instanceof AggExecutionNode);
    for (int i = 1; i < aggBlockCount; i++) {
      assertTrue(converted.getDependent(i) instanceof AggCombinerExecutionNode);
      if (i == 1) {
        assertTrue(converted.getDependent(i).getDependent(0) instanceof AggExecutionNode);
        assertTrue(converted.getDependent(i).getDependent(1) instanceof AggExecutionNode);
      } else {
        assertTrue(converted.getDependent(i).getDependent(0) instanceof AggCombinerExecutionNode);
        assertTrue(converted.getDependent(i).getDependent(1) instanceof AggExecutionNode);
      }
    }
//    assertEquals("initialized", converted.getStatus());
//    assertEquals("initialized", converted.getDependent(0).getStatus());
//    for (int i = 1; i < aggBlockCount; i++) {
//      assertEquals("initialized", converted.getDependent(i).getStatus());
//      assertEquals("initialized", converted.getDependent(i).getDependent(0).getStatus());
//      assertEquals("initialized", converted.getDependent(i).getDependent(1).getStatus());
//    }
    ExecutablePlanRunner.runTillEnd(
        new JdbcConnection(conn, new H2Syntax()), 
        new SimpleTreePlan(converted));
//    converted.executeAndWaitForTermination(new JdbcConnection(conn, new H2Syntax()));
    
//    assertEquals("success", converted.getStatus());
//    assertEquals("success", converted.getDependent(0).getStatus());
//    for (int i = 1; i < aggBlockCount; i++) {
//      assertEquals("success", converted.getDependent(i).getStatus());
//      assertEquals("success", converted.getDependent(i).getDependent(0).getStatus());
//      assertEquals("success", converted.getDependent(i).getDependent(1).getStatus());
//    }
  }
  
  // the origiinal query does not include any scrambled tables; thus, it must not be converted to any other
  // form.
  @Test
  public void testConvertNonBigFlatToProgressiveAgg() throws VerdictDBValueException {
    
  }
  
  static void populateRandomData(Connection conn, String schemaName, String tableName) throws SQLException {
    Statement stmt = conn.createStatement();
    stmt.execute(String.format("CREATE TABLE \"%s\".\"%s\"(\"id\" int, \"value\" double)", schemaName, tableName));
    Random r = new Random();
    for (int i = 0; i < 10; i++) {
      stmt.execute(String.format("INSERT INTO \"%s\".\"%s\"(\"id\", \"value\") VALUES(%s, %f)",
          schemaName, tableName, i, r.nextDouble()));
    }
    stmt.close();
  }

}
