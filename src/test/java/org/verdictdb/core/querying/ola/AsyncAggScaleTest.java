package org.verdictdb.core.querying.ola;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DOUBLE;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.junit.BeforeClass;
import org.junit.Test;
import org.verdictdb.core.connection.JdbcConnection;
import org.verdictdb.core.connection.StaticMetaData;
import org.verdictdb.core.execution.ExecutablePlanRunner;
import org.verdictdb.core.execution.ExecutionInfoToken;
import org.verdictdb.core.querying.AggExecutionNode;
import org.verdictdb.core.querying.QueryExecutionPlan;
import org.verdictdb.core.scrambling.ScrambleMeta;
import org.verdictdb.core.scrambling.ScrambleMetaForTable;
import org.verdictdb.core.scrambling.UniformScrambler;
import org.verdictdb.core.sqlobject.AbstractRelation;
import org.verdictdb.core.sqlobject.CreateTableAsSelectQuery;
import org.verdictdb.core.sqlobject.SelectQuery;
import org.verdictdb.core.sqlobject.SqlConvertible;
import org.verdictdb.exception.VerdictDBException;
import org.verdictdb.sqlreader.NonValidatingSQLParser;
import org.verdictdb.sqlreader.RelationStandardizer;
import org.verdictdb.sqlsyntax.H2Syntax;
import org.verdictdb.sqlwriter.CreateTableToSql;
import org.verdictdb.sqlwriter.QueryToSql;
import org.verdictdb.sqlwriter.SelectQueryToSql;

public class AsyncAggScaleTest {

  static Connection conn;

  static Statement stmt;

  static int aggBlockCount = 2;

  static ScrambleMeta meta = new ScrambleMeta();

  static StaticMetaData staticMetaData = new StaticMetaData();

  static String scrambledTable;

  String placeholderSchemaName = "placeholderSchemaName";

  String placeholderTableName = "placeholderTableName";

  static String originalSchema = "originalSchema";

  static String originalTable = "originalTable";

  static String smallTable = "smallTable";

  @BeforeClass
  public static void setupH2Database() throws SQLException, VerdictDBException {
    final String DB_CONNECTION = "jdbc:h2:mem:asyncaggscaletest;DB_CLOSE_DELAY=-1";
    final String DB_USER = "";
    final String DB_PASSWORD = "";
    conn = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);

    stmt = conn.createStatement();
    stmt.execute(String.format("CREATE SCHEMA IF NOT EXISTS\"%s\"", originalSchema));
    stmt.executeUpdate(String.format("CREATE TABLE \"%s\".\"%s\"(\"id\" int, \"value\" double)", originalSchema, originalTable));
    for (int i = 0; i < 10; i++) {
      stmt.executeUpdate(String.format("INSERT INTO \"%s\".\"%s\"(\"id\", \"value\") VALUES(%s, %f)",
          originalSchema, originalTable, i, (double) i+1));
    }
    stmt.executeUpdate(String.format("CREATE TABLE \"%s\".\"%s\"(\"s_id\" int, \"s_value\" double)", originalSchema, smallTable));
    for (int i = 0; i < 10; i++) {
      stmt.executeUpdate(String.format("INSERT INTO \"%s\".\"%s\"(\"s_id\", \"s_value\") VALUES(%s, %f)",
          originalSchema, smallTable, i, (double) i+1));
    }

    UniformScrambler scrambler =
        new UniformScrambler(originalSchema, originalTable, originalSchema, "originalTable_scrambled", aggBlockCount);
    CreateTableAsSelectQuery scramblingQuery = scrambler.createQuery();
    stmt.executeUpdate(QueryToSql.convert(new H2Syntax(), scramblingQuery));
    ScrambleMetaForTable tablemeta = scrambler.generateMeta();
    tablemeta.setNumberOfTiers(1);
    HashMap<Integer, List<Double>> distribution = new HashMap<>();
    distribution.put(0, Arrays.asList(0.5, 1.0));
    tablemeta.setCumulativeMassDistributionPerTier(distribution);
    scrambledTable = tablemeta.getTableName();
    meta.insertScrambleMetaEntry(tablemeta);

    staticMetaData.setDefaultSchema(originalSchema);
    List<Pair<String, Integer>> arr = new ArrayList<>();
    arr.addAll(Arrays.asList(new ImmutablePair<>("id", BIGINT),
        new ImmutablePair<>("value", DOUBLE)
    ));
    staticMetaData.addTableData(new StaticMetaData.TableInfo(originalSchema, "originalTable_scrambled"), arr);
    arr = new ArrayList<>();
    arr.addAll(Arrays.asList(new ImmutablePair<>("s_id", BIGINT),
        new ImmutablePair<>("s_value", DOUBLE)
    ));
    staticMetaData.addTableData(new StaticMetaData.TableInfo(originalSchema, smallTable), arr);
  }

  @Test
  public void ScrambleTableTest() throws VerdictDBException,SQLException {
    RelationStandardizer.resetItemID();
    String sql = "select sum(value) from originalTable_scrambled";
    NonValidatingSQLParser sqlToRelation = new NonValidatingSQLParser();
    AbstractRelation relation = sqlToRelation.toRelation(sql);
    RelationStandardizer gen = new RelationStandardizer(staticMetaData);
    relation = gen.standardize((SelectQuery) relation);

    QueryExecutionPlan queryExecutionPlan = new QueryExecutionPlan("verdictdb_temp", meta, (SelectQuery) relation);
    queryExecutionPlan.cleanUp();
    queryExecutionPlan = AsyncQueryExecutionPlan.create(queryExecutionPlan);
    Dimension d1 = new Dimension("originalSchema", "originalTable_scrambled", 0, 0);
//<<<<<<< HEAD:src/test/java/org/verdictdb/core/querying/AsyncAggScaleTest.java
    assertEquals(
        new HyperTableCube(Arrays.asList(d1)), 
        ((AggExecutionNode) queryExecutionPlan.getRootNode().getExecutableNodeBaseDependents().get(0).getExecutableNodeBaseDependents().get(0)).getMeta().getCubes().get(0));
    ((AsyncAggExecutionNode)queryExecutionPlan.getRoot().getExecutableNodeBaseDependents().get(0)).setScrambleMeta(meta);
//    queryExecutionPlan.setScalingNode();
//=======
//    Assert.assertEquals(new HyperTableCube(Arrays.asList(d1)), ((AggExecutionNode)queryExecutionPlan.getRootNode().getDependents().get(0).getDependents().get(0)).getCubes().get(0));
//    ((AsyncAggExecutionNode)queryExecutionPlan.getRoot().getDependents().get(0)).setScrambleMeta(meta);
    //>>>>>>> origin/joezhong-scale:src/test/java/org/verdictdb/core/querying/ola/AsyncAggScaleTest.java
    stmt.execute("create schema if not exists \"verdictdb_temp\";");
//    queryExecutionPlan.getRoot().print();
    
    ExecutablePlanRunner.runTillEnd(new JdbcConnection(conn, new H2Syntax()), queryExecutionPlan);
//    queryExecutionPlan.root.executeAndWaitForTermination();
    stmt.execute("drop schema \"verdictdb_temp\" cascade;");
  }

//<<<<<<< HEAD:src/test/java/org/verdictdb/core/querying/AsyncAggScaleTest.java
  @Test
  public void ScrambleTableCompressTest() throws VerdictDBException,SQLException {
    RelationStandardizer.resetItemID();
    String sql = "select sum(value) from originalTable_scrambled";
    NonValidatingSQLParser sqlToRelation = new NonValidatingSQLParser();
    AbstractRelation relation = sqlToRelation.toRelation(sql);
    RelationStandardizer gen = new RelationStandardizer(staticMetaData);
    relation = gen.standardize((SelectQuery) relation);

    QueryExecutionPlan queryExecutionPlan = new QueryExecutionPlan("verdictdb_temp", meta, (SelectQuery) relation);
    queryExecutionPlan.cleanUp();
    queryExecutionPlan = AsyncQueryExecutionPlan.create(queryExecutionPlan);
    ((AsyncAggExecutionNode)queryExecutionPlan.getRoot().getExecutableNodeBaseDependents().get(0)).setScrambleMeta(meta);
//    queryExecutionPlan.setScalingNode();
    queryExecutionPlan.compress();
    stmt.execute("create schema if not exists \"verdictdb_temp\";");
    ExecutablePlanRunner.runTillEnd(new JdbcConnection(conn, new H2Syntax()), queryExecutionPlan);
//    queryExecutionPlan.root.executeAndWaitForTermination(new JdbcConnection(conn, new H2Syntax()));
    stmt.execute("drop schema \"verdictdb_temp\" cascade;");
  }
//=======
//>>>>>>> origin/joezhong-scale:src/test/java/org/verdictdb/core/querying/ola/AsyncAggScaleTest.java

  @Test
  public void ScrambleTableAvgTest() throws VerdictDBException,SQLException {
    RelationStandardizer.resetItemID();
    String sql = "select (1+avg(value))*sum(value), count(*), count(value) from originalTable_scrambled";
    NonValidatingSQLParser sqlToRelation = new NonValidatingSQLParser();
    AbstractRelation relation = sqlToRelation.toRelation(sql);
    RelationStandardizer gen = new RelationStandardizer(staticMetaData);
    relation = gen.standardize((SelectQuery) relation);

    QueryExecutionPlan queryExecutionPlan = new QueryExecutionPlan("verdictdb_temp", meta, (SelectQuery) relation);
    queryExecutionPlan.cleanUp();
    queryExecutionPlan = AsyncQueryExecutionPlan.create(queryExecutionPlan);
    Dimension d1 = new Dimension("originalSchema", "originalTable_scrambled", 0, 0);
    assertEquals(
        new HyperTableCube(Arrays.asList(d1)),
        ((AggExecutionNode) queryExecutionPlan.getRootNode().getExecutableNodeBaseDependents().get(0).getExecutableNodeBaseDependents().get(0)).getMeta().getCubes().get(0));
    ((AsyncAggExecutionNode)queryExecutionPlan.getRoot().getExecutableNodeBaseDependents().get(0)).setScrambleMeta(meta);

    stmt.execute("create schema if not exists \"verdictdb_temp\";");
    ExecutablePlanRunner.runTillEnd(new JdbcConnection(conn, new H2Syntax()), queryExecutionPlan);
    stmt.execute("drop schema \"verdictdb_temp\" cascade;");
  }

  @Test
  public void toSqlTest() throws VerdictDBException,SQLException {
    String sql = "select (1+avg(value))*sum(value) from originalTable_scrambled";
    NonValidatingSQLParser sqlToRelation = new NonValidatingSQLParser();
    AbstractRelation relation = sqlToRelation.toRelation(sql);
    RelationStandardizer gen = new RelationStandardizer(staticMetaData);
    relation = gen.standardize((SelectQuery) relation);

    QueryExecutionPlan queryExecutionPlan = new QueryExecutionPlan("verdictdb_temp", meta, (SelectQuery) relation);
    queryExecutionPlan.cleanUp();
    queryExecutionPlan = AsyncQueryExecutionPlan.create(queryExecutionPlan);
    ((AsyncAggExecutionNode)queryExecutionPlan.getRoot().getExecutableNodeBaseDependents().get(0)).setScrambleMeta(meta);

    ExecutionInfoToken token = new ExecutionInfoToken();
    CreateTableAsSelectQuery query = (CreateTableAsSelectQuery) queryExecutionPlan.getRoot().getSources().get(0).getSources().get(0).createQuery(Arrays.asList(token));
    SelectQueryToSql queryToSql = new SelectQueryToSql(new H2Syntax());
    String actual = queryToSql.toSql(query.getSelect());
    String expected = "select sum(vt5.\"value\") as \"agg0\", count(*) as \"agg1\" from \"originalSchema\".\"originalTable_scrambled\" as vt5 where vt5.\"verdictdbaggblock\" = 0";
    assertEquals(expected, actual);

    ExecutionInfoToken token1 = new ExecutionInfoToken();
    token1.setKeyValue("schemaName", "verdict_temp");
    token1.setKeyValue("tableName", "table1");
    ExecutionInfoToken token2 = new ExecutionInfoToken();
    token2.setKeyValue("schemaName", "verdict_temp");
    token2.setKeyValue("tableName", "table2");
    query = (CreateTableAsSelectQuery) queryExecutionPlan.getRoot().getSources().get(0).getSources().get(1).createQuery(Arrays.asList(token1, token2));
    actual = queryToSql.toSql(query.getSelect());
    actual = actual.replaceAll("verdictdbalias_[0-9]*_[0-9]", "alias");
    expected = "select alias.\"agg0\" + alias.\"agg0\" as \"agg0\", alias.\"agg1\" + alias.\"agg1\" as \"agg1\" from \"verdict_temp\".\"table1\" as alias, \"verdict_temp\".\"table2\" as alias";
    assertEquals(expected, actual);
  }
}
