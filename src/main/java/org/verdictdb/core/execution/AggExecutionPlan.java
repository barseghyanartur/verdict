/*
 * Copyright 2018 University of Michigan
 * 
 * You must contact Barzan Mozafari (mozafari@umich.edu) or Yongjoo Park (pyongjoo@umich.edu) to discuss
 * how you could use, modify, or distribute this code. By default, this code is not open-sourced and we do
 * not license this code.
 */

package org.verdictdb.core.execution;

import org.verdictdb.connection.DbmsConnection;
import org.verdictdb.core.DbmsMetaDataCache;
import org.verdictdb.core.query.SelectQueryOp;
import org.verdictdb.core.rewriter.ScrambleMeta;
import org.verdictdb.core.sql.NonValidatingSQLParser;
import org.verdictdb.exception.UnexpectedTypeException;
import org.verdictdb.exception.ValueException;
import org.verdictdb.exception.VerdictDbException;
import org.verdictdb.sql.syntax.SyntaxAbstract;

public class AggExecutionPlan {
  
  DbmsConnection conn;
  
  SelectQueryOp query;
  
  AggExecutionNode root;
  
  DbmsMetaDataCache meta;
  
  /**
   * 
   * @param queryString A select query
   * @throws UnexpectedTypeException 
   */
  public AggExecutionPlan(DbmsConnection conn, SyntaxAbstract syntax, String queryString) throws VerdictDbException {
    this(conn, syntax, (SelectQueryOp) new NonValidatingSQLParser().toRelation(queryString));
  }
  
  /**
   * 
   * @param query  A well-formed select query object
   * @throws ValueException 
   * @throws VerdictDbException 
   */
  public AggExecutionPlan(DbmsConnection conn, SyntaxAbstract syntax, SelectQueryOp query) throws VerdictDbException {
    this.conn = conn;
    this.meta = new DbmsMetaDataCache(conn);
    if (!query.isAggregateQuery()) {
      throw new UnexpectedTypeException(query);
    }
    this.query = query;
    this.root = plan(conn, query);
  }
  
  /** 
   * Creates a tree in which each node is AggExecutionNode
   * 
   * @param conn
   * @param query
   * @return The root of the tree.
   * @throws ValueException 
   * @throws UnexpectedTypeException 
   */
  AggExecutionNode plan(DbmsConnection conn, SelectQueryOp query) throws VerdictDbException {
    ScrambleMeta meta = new ScrambleMeta();
    return new AggExecutionNode(conn, meta, query);
  }
  
  public void execute(DbmsConnection conn) {
    
  }

}
