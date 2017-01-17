/*
 * Copyright (c) 2016 SnappyData, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package org.apache.spark.sql

import java.io.{FileOutputStream, File, PrintWriter}
import java.sql.{ResultSet, Statement}

import io.snappydata.cluster.ClusterManagerTestBase
import io.snappydata.test.dunit.AvailablePortHelper
import org.apache.spark.sql.catalyst.encoders.RowEncoder

import org.apache.spark.sql.collection.Utils
import org.apache.spark.sql.execution.columnar.ColumnTableScan
import org.apache.spark.sql.execution.joins._
import org.apache.spark.sql.execution.{FilterExec, ProjectExec, RowTableScan}

import scala.io.Source

class NorthWindDUnitTest(s: String) extends ClusterManagerTestBase(s) {

  def testReplicatedTableQueries(): Unit = {
    val snc = SnappyContext(sc)
    val sqlContext = SQLContext.getOrCreate(sc)
    val pw = new PrintWriter(new FileOutputStream(new File("ValidateNWQueries_ReplicatedTable.out"), true));
    NorthWindDUnitTest.createAndLoadReplicatedTables(snc)
    NorthWindDUnitTest.createAndLoadSparkTables(sqlContext)
    validateReplicatedTableQueries(snc)
    NorthWindDUnitTest.validateQueriesFullResultSet(snc, "ReplicatedTable", pw, sqlContext)
    pw.close()
  }

  def testPartitionedRowTableQueries(): Unit = {
    val snc = SnappyContext(sc)
    val sqlContext = SQLContext.getOrCreate(sc)
    val pw = new PrintWriter(new FileOutputStream(new File("ValidateNWQueries_PartitionedRowTable.out"), true));
    createAndLoadPartitionedTables(snc)
    NorthWindDUnitTest.createAndLoadSparkTables(sqlContext)
    validatePartitionedRowTableQueries(snc)
    NorthWindDUnitTest.validateQueriesFullResultSet(snc, "PartitionedRowTable", pw, sqlContext)
    pw.close()
  }

  def testPartitionedColumnTableQueries(): Unit = {
    val snc = SnappyContext(sc)
    val sqlContext = SQLContext.getOrCreate(sc)
    val pw = new PrintWriter(new FileOutputStream(new File("ValidateNWQueries_ColumnTable.out"), true));
    NorthWindDUnitTest.createAndLoadColumnTables(snc)
    NorthWindDUnitTest.createAndLoadSparkTables(sqlContext)
    validatePartitionedColumnTableQueries(snc)
    NorthWindDUnitTest.validateQueriesFullResultSet(snc, "ColumnTable", pw, sqlContext)
    pw.close()
  }

  def testColocatedTableQueries(): Unit = {
    val snc = SnappyContext(sc)
    val sqlContext = SQLContext.getOrCreate(sc)
    val pw = new PrintWriter(new FileOutputStream(new File("ValidateNWQueries_ColocatedTable.out"), true));
    NorthWindDUnitTest.createAndLoadColocatedTables(snc)
    NorthWindDUnitTest.createAndLoadSparkTables(sqlContext)
    validateColocatedTableQueries(snc)
    NorthWindDUnitTest.validateQueriesFullResultSet(snc, "ColocatedTable", pw, sqlContext)
    pw.close()
  }

  def testInsertionOfRecordInColumnTable(): Unit = {
    val snc = SnappyContext(sc)
    val netPort = AvailablePortHelper.getRandomAvailableTCPPort
    vm2.invoke(classOf[ClusterManagerTestBase], "startNetServer", netPort)
    val conn = getANetConnection(netPort)

    val s = conn.createStatement()
    createAndLoadColumnTableUsingJDBC(s, snc)
    val rs: ResultSet = s.executeQuery(s"SELECT * from products")
    assert(rs.next())
    conn.close()
  }

  private lazy val totalProcessors = Utils.mapExecutors(sc, (_, _) =>
    Iterator(Runtime.getRuntime.availableProcessors())).collect().sum

  private def validateReplicatedTableQueries(snc: SnappyContext): Unit = {
    for (q <- NWQueries.queries) {
      q._1 match {
        case "Q1" => NWQueries.assertQuery(snc, NWQueries.Q1, "Q1", 8, 1, classOf[RowTableScan])
        case "Q2" => NWQueries.assertQuery(snc, NWQueries.Q2, "Q2", 91, 1, classOf[RowTableScan])
        case "Q3" => NWQueries.assertQuery(snc, NWQueries.Q3, "Q3", 830, 1, classOf[RowTableScan])
        case "Q4" => NWQueries.assertQuery(snc, NWQueries.Q4, "Q4", 9, 1, classOf[RowTableScan])
        case "Q5" => NWQueries.assertQuery(snc, NWQueries.Q5, "Q5", 9, 1, classOf[RowTableScan])
        case "Q6" => NWQueries.assertQuery(snc, NWQueries.Q6, "Q6", 9, 1, classOf[RowTableScan])
        case "Q7" => NWQueries.assertQuery(snc, NWQueries.Q7, "Q7", 9, 1, classOf[RowTableScan])
        case "Q8" => NWQueries.assertQuery(snc, NWQueries.Q8, "Q8", 6, 1, classOf[FilterExec])
        case "Q9" => NWQueries.assertQuery(snc, NWQueries.Q9, "Q9", 3, 1, classOf[ProjectExec])
        case "Q10" => NWQueries.assertQuery(snc, NWQueries.Q10, "Q10", 2, 1, classOf[FilterExec])
        case "Q11" => NWQueries.assertQuery(snc, NWQueries.Q11, "Q11", 4, 1, classOf[ProjectExec])
        case "Q12" => NWQueries.assertQuery(snc, NWQueries.Q12, "Q12", 2, 1, classOf[FilterExec])
        case "Q13" => NWQueries.assertQuery(snc, NWQueries.Q13, "Q13", 2, 1, classOf[FilterExec])
        case "Q14" => NWQueries.assertQuery(snc, NWQueries.Q14, "Q14", 69, 1, classOf[FilterExec])
        case "Q15" => NWQueries.assertQuery(snc, NWQueries.Q15, "Q15", 5, 1, classOf[FilterExec])
        case "Q16" => NWQueries.assertQuery(snc, NWQueries.Q16, "Q16", 8, 1, classOf[FilterExec])
        case "Q17" => NWQueries.assertQuery(snc, NWQueries.Q17, "Q17", 3, 1, classOf[FilterExec])
        case "Q18" => NWQueries.assertQuery(snc, NWQueries.Q18, "Q18", 9, 1, classOf[ProjectExec])
        case "Q19" => NWQueries.assertQuery(snc, NWQueries.Q19, "Q19", 13, 1, classOf[ProjectExec])
        case "Q20" => NWQueries.assertQuery(snc, NWQueries.Q20, "Q20", 1, 1, classOf[ProjectExec])
        case "Q21" => NWQueries.assertQuery(snc, NWQueries.Q21, "Q21", 1, 1, classOf[RowTableScan])
        case "Q22" => NWQueries.assertQuery(snc, NWQueries.Q22, "Q22", 1, 1, classOf[ProjectExec])
        case "Q23" => NWQueries.assertQuery(snc, NWQueries.Q23, "Q23", 1, 1, classOf[RowTableScan])
        case "Q24" => NWQueries.assertQuery(snc, NWQueries.Q24, "Q24", 4, 1, classOf[ProjectExec])
        case "Q25" => NWQueries.assertJoin(snc, NWQueries.Q25, "Q25", 1, 1, classOf[RowTableScan])
        case "Q26" => NWQueries.assertJoin(snc, NWQueries.Q26, "Q26", 86, 1,
          classOf[SortMergeJoinExec])
        case "Q27" => NWQueries.assertJoin(snc, NWQueries.Q27, "Q27", 9, 1,
          classOf[SortMergeJoinExec])
        case "Q28" => NWQueries.assertJoin(snc, NWQueries.Q28, "Q28", 12, 1, classOf[RowTableScan])
        case "Q29" => NWQueries.assertJoin(snc, NWQueries.Q29, "Q29", 8, 1,
          classOf[SortMergeJoinExec])
        case "Q30" => NWQueries.assertJoin(snc, NWQueries.Q30, "Q30", 8, 1,
          classOf[SortMergeJoinExec])
        case "Q31" => NWQueries.assertJoin(snc, NWQueries.Q31, "Q31", 830, 1, classOf[LocalJoin])
        case "Q32" => NWQueries.assertJoin(snc, NWQueries.Q32, "Q32", 8, 1, classOf[LocalJoin])
        case "Q33" => NWQueries.assertJoin(snc, NWQueries.Q33, "Q33", 37, 1, classOf[LocalJoin])
        case "Q34" => NWQueries.assertJoin(snc, NWQueries.Q34, "Q34", 5, 1, classOf[LocalJoin])
        case "Q35" => NWQueries.assertJoin(snc, NWQueries.Q35, "Q35", 3, 4, classOf[LocalJoin])
        case "Q36" => NWQueries.assertJoin(snc, NWQueries.Q36, "Q36", 290, 1, classOf[LocalJoin])
        case "Q37" => NWQueries.assertJoin(snc, NWQueries.Q37, "Q37", 77, totalProcessors,
          classOf[LocalJoin])
        case "Q38" => NWQueries.assertJoin(snc, NWQueries.Q38, "Q38", 2155, 1, classOf[LocalJoin])
        case "Q39" => NWQueries.assertJoin(snc, NWQueries.Q39, "Q39", 9, 1, classOf[LocalJoin])
        case "Q40" => NWQueries.assertJoin(snc, NWQueries.Q40, "Q40", 830, 1, classOf[LocalJoin])
        case "Q41" => NWQueries.assertJoin(snc, NWQueries.Q41, "Q41", 2155, 1, classOf[LocalJoin])
        case "Q42" => NWQueries.assertJoin(snc, NWQueries.Q42, "Q42", 22, 1, classOf[LocalJoin])
        case "Q43" => NWQueries.assertJoin(snc, NWQueries.Q43, "Q43", 830, 1,
          classOf[SortMergeJoinExec])
        case "Q44" => NWQueries.assertJoin(snc, NWQueries.Q44, "Q44", 830, 1,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q45" => NWQueries.assertJoin(snc, NWQueries.Q45, "Q45", 1788650, 1,
          classOf[CartesianProductExec])
        case "Q46" => NWQueries.assertJoin(snc, NWQueries.Q46, "Q46", 1788650, 1,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q47" => NWQueries.assertJoin(snc, NWQueries.Q47, "Q47", 1788650, 1,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q48" => NWQueries.assertJoin(snc, NWQueries.Q48, "Q48", 1788650,
          totalProcessors * 2 + 1, classOf[BroadcastNestedLoopJoinExec])
        case "Q49" => NWQueries.assertJoin(snc, NWQueries.Q49, "Q49", 1788650,
          totalProcessors * 2 + 1, classOf[BroadcastNestedLoopJoinExec])
        case "Q50" => NWQueries.assertJoin(snc, NWQueries.Q50, "Q50", 2155, 1, classOf[LocalJoin])
        case "Q51" => NWQueries.assertJoin(snc, NWQueries.Q51, "Q51", 2155, 1,
          classOf[SortMergeJoinExec])
        case "Q52" => NWQueries.assertJoin(snc, NWQueries.Q52, "Q52", 2155, 1,
          classOf[SortMergeJoinExec])
        case "Q53" => NWQueries.assertJoin(snc, NWQueries.Q53, "Q53", 2155, 1,
          classOf[SortMergeJoinExec])
        case "Q54" => NWQueries.assertJoin(snc, NWQueries.Q54, "Q54", 2155, 1,
          classOf[SortMergeJoinExec])
        case "Q55" => NWQueries.assertJoin(snc, NWQueries.Q55, "Q55", 21, 1, classOf[LocalJoin])
        case "Q56" => NWQueries.assertJoin(snc, NWQueries.Q56, "Q56", 8, 1, classOf[LocalJoin])
      }
    }
  }

  private def createAndLoadPartitionedTables(snc: SnappyContext): Unit = {

    snc.sql(NWQueries.regions_table)
    NWQueries.regions(snc).write.insertInto("regions")

    snc.sql(NWQueries.categories_table)
    NWQueries.categories(snc).write.insertInto("categories")

    snc.sql(NWQueries.shippers_table)
    NWQueries.shippers(snc).write.insertInto("shippers")

    snc.sql(NWQueries.employees_table)
    NWQueries.employees(snc).write.insertInto("employees")

    snc.sql(NWQueries.customers_table)
    NWQueries.customers(snc).write.insertInto("customers")

    snc.sql(NWQueries.orders_table + " using row options (" +
        "partition_by 'OrderId', buckets '13', redundancy '1')")
    NWQueries.orders(snc).write.insertInto("orders")

    snc.sql(NWQueries.order_details_table + " using row options (" +
        "partition_by 'OrderId', buckets '13', COLOCATE_WITH 'orders', " +
        "redundancy '1')")
    NWQueries.order_details(snc).write.insertInto("order_details")

    snc.sql(NWQueries.products_table +
        " using row options ( partition_by 'ProductID', buckets '17')")
    NWQueries.products(snc).write.insertInto("products")

    snc.sql(NWQueries.suppliers_table +
        " USING row options (PARTITION_BY 'SupplierID', buckets '123' )")
    NWQueries.suppliers(snc).write.insertInto("suppliers")

    snc.sql(NWQueries.territories_table +
        " using row options (partition_by 'TerritoryID', buckets '3')")
    NWQueries.territories(snc).write.insertInto("territories")

    snc.sql(NWQueries.employee_territories_table +
        " using row options(partition_by 'EmployeeID', buckets '1')")
    NWQueries.employee_territories(snc).write.insertInto("employee_territories")

  }

  private def validatePartitionedRowTableQueries(snc: SnappyContext): Unit = {
    val numDefaultPartitions = ((totalProcessors - 4) to (totalProcessors + 4)).toArray
    for (q <- NWQueries.queries) {
      q._1 match {
        case "Q1" => NWQueries.assertQuery(snc, NWQueries.Q1, "Q1", 8, 1, classOf[RowTableScan])
        case "Q2" => NWQueries.assertQuery(snc, NWQueries.Q2, "Q2", 91, 1, classOf[RowTableScan])
        case "Q3" => NWQueries.assertQuery(snc, NWQueries.Q3, "Q3", 830, numDefaultPartitions,
          classOf[RowTableScan])
        case "Q4" => NWQueries.assertQuery(snc, NWQueries.Q4, "Q4", 9, 1, classOf[RowTableScan])
        case "Q5" => NWQueries.assertQuery(snc, NWQueries.Q5, "Q5", 9, 1, classOf[RowTableScan])
        case "Q6" => NWQueries.assertQuery(snc, NWQueries.Q6, "Q6", 9, 1, classOf[RowTableScan])
        case "Q7" => NWQueries.assertQuery(snc, NWQueries.Q7, "Q7", 9, 1, classOf[RowTableScan])
        case "Q8" => NWQueries.assertQuery(snc, NWQueries.Q8, "Q8", 6, 1, classOf[FilterExec])
        case "Q9" => NWQueries.assertQuery(snc, NWQueries.Q9, "Q9", 3, 1, classOf[ProjectExec])
        case "Q10" => NWQueries.assertQuery(snc, NWQueries.Q10, "Q10", 2, 1, classOf[FilterExec])
        case "Q11" => NWQueries.assertQuery(snc, NWQueries.Q11, "Q11", 4, 1, classOf[ProjectExec])
        case "Q12" => NWQueries.assertQuery(snc, NWQueries.Q12, "Q12", 2, 1, classOf[FilterExec])
        case "Q13" => NWQueries.assertQuery(snc, NWQueries.Q13, "Q13", 2, numDefaultPartitions,
          classOf[FilterExec])
        case "Q14" => NWQueries.assertQuery(snc, NWQueries.Q14, "Q14", 69, 1, classOf[FilterExec])
        case "Q15" => NWQueries.assertQuery(snc, NWQueries.Q15, "Q15", 5, 1, classOf[FilterExec])
        case "Q16" => NWQueries.assertQuery(snc, NWQueries.Q16, "Q16", 8, 1, classOf[FilterExec])
        case "Q17" => NWQueries.assertQuery(snc, NWQueries.Q17, "Q17", 3, 1, classOf[FilterExec])
        case "Q18" => NWQueries.assertQuery(snc, NWQueries.Q18, "Q18", 9, 1, classOf[ProjectExec])
        case "Q19" => NWQueries.assertQuery(snc, NWQueries.Q19, "Q19", 13, numDefaultPartitions,
          classOf[ProjectExec])
        case "Q20" => NWQueries.assertQuery(snc, NWQueries.Q20, "Q20", 1, 1, classOf[ProjectExec])
        case "Q21" => NWQueries.assertQuery(snc, NWQueries.Q21, "Q21", 1, 1, classOf[RowTableScan])
        case "Q22" => NWQueries.assertQuery(snc, NWQueries.Q22, "Q22", 1, 1, classOf[ProjectExec])
        case "Q23" => NWQueries.assertQuery(snc, NWQueries.Q23, "Q23", 1, 1, classOf[RowTableScan])
        case "Q24" => NWQueries.assertQuery(snc, NWQueries.Q24, "Q24", 4, 5, classOf[ProjectExec])
        case "Q25" => NWQueries.assertJoin(snc, NWQueries.Q25, "Q25", 1, 1, classOf[RowTableScan])
        case "Q26" => NWQueries.assertJoin(snc, NWQueries.Q26, "Q26", 86, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q27" => NWQueries.assertJoin(snc, NWQueries.Q27, "Q27", 9, 123,
          classOf[BroadcastHashJoinExec])
        case "Q28" => NWQueries.assertJoin(snc, NWQueries.Q28, "Q28", 12, totalProcessors,
          classOf[RowTableScan])
        case "Q29" => NWQueries.assertJoin(snc, NWQueries.Q29, "Q29", 8, 123,
          classOf[BroadcastHashJoinExec])
        case "Q30" => NWQueries.assertJoin(snc, NWQueries.Q30, "Q30", 8, 123,
          classOf[BroadcastHashJoinExec])
        case "Q31" => NWQueries.assertJoin(snc, NWQueries.Q31, "Q31", 830, totalProcessors,
          classOf[LocalJoin])
        case "Q32" => NWQueries.assertJoin(snc, NWQueries.Q32, "Q32", 8, 9, classOf[LocalJoin])
        case "Q33" => NWQueries.assertJoin(snc, NWQueries.Q33, "Q33", 37, 9, classOf[LocalJoin])
        case "Q34" => NWQueries.assertJoin(snc, NWQueries.Q34, "Q34", 5, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q35" => NWQueries.assertJoin(snc, NWQueries.Q35, "Q35", 3, 4, classOf[LocalJoin])
        case "Q36" => NWQueries.assertJoin(snc, NWQueries.Q36, "Q36", 290, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q37" => NWQueries.assertJoin(snc, NWQueries.Q37, "Q37", 77, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q38" => NWQueries.assertJoin(snc, NWQueries.Q38, "Q38", 2155, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q39" => NWQueries.assertJoin(snc, NWQueries.Q39, "Q39", 9, 123,
          classOf[LocalJoin])
        case "Q40" => NWQueries.assertJoin(snc, NWQueries.Q40, "Q40", 830, totalProcessors,
          classOf[LocalJoin])
        case "Q41" => NWQueries.assertJoin(snc, NWQueries.Q41, "Q41", 2155, 13,
          classOf[LocalJoin])
        case "Q42" => NWQueries.assertJoin(snc, NWQueries.Q42, "Q42", 22, totalProcessors,
          classOf[LocalJoin])
        case "Q43" => NWQueries.assertJoin(snc, NWQueries.Q43, "Q43", 830, 13,
          classOf[SortMergeJoinExec])
        case "Q44" => NWQueries.assertJoin(snc, NWQueries.Q44, "Q44", 830, 13,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q45" => NWQueries.assertJoin(snc, NWQueries.Q45, "Q45", 1788650, 13,
          classOf[CartesianProductExec])
        case "Q46" => NWQueries.assertJoin(snc, NWQueries.Q46, "Q46", 1788650, 13,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q47" => NWQueries.assertJoin(snc, NWQueries.Q47, "Q47", 1788650, 37,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q48" => NWQueries.assertJoin(snc, NWQueries.Q48, "Q48", 1788650, 37,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q49" => NWQueries.assertJoin(snc, NWQueries.Q49, "Q49", 1788650, 37,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q50" => NWQueries.assertJoin(snc, NWQueries.Q50, "Q50", 2155, 13,
          classOf[LocalJoin])
        case "Q51" => NWQueries.assertJoin(snc, NWQueries.Q51, "Q51", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q52" => NWQueries.assertJoin(snc, NWQueries.Q52, "Q52", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q53" => NWQueries.assertJoin(snc, NWQueries.Q53, "Q53", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q54" => NWQueries.assertJoin(snc, NWQueries.Q54, "Q54", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q55" => NWQueries.assertJoin(snc, NWQueries.Q55, "Q55", 21, totalProcessors,
          classOf[LocalJoin])
        case "Q56" => NWQueries.assertJoin(snc, NWQueries.Q56, "Q56", 8, 1, classOf[LocalJoin])
      }
    }
  }

  def validatePartitionedColumnTableQueries(snc: SnappyContext): Unit = {
    val numDefaultPartitions = ((totalProcessors - 4) to (totalProcessors + 4)).toArray
    for (q <- NWQueries.queries) {
      q._1 match {
        case "Q1" => NWQueries.assertQuery(snc, NWQueries.Q1, "Q1", 8, 1, classOf[RowTableScan])
        case "Q2" => NWQueries.assertQuery(snc, NWQueries.Q2, "Q2", 91, 1, classOf[RowTableScan])
        case "Q3" => NWQueries.assertQuery(snc, NWQueries.Q3, "Q3", 830, numDefaultPartitions,
          classOf[ColumnTableScan])
        case "Q4" => NWQueries.assertQuery(snc, NWQueries.Q4, "Q4", 9, totalProcessors,
          classOf[ColumnTableScan])
        case "Q5" => NWQueries.assertQuery(snc, NWQueries.Q5, "Q5", 9, 10, classOf[ColumnTableScan])
        case "Q6" => NWQueries.assertQuery(snc, NWQueries.Q6, "Q6", 9, 10, classOf[ColumnTableScan])
        case "Q7" => NWQueries.assertQuery(snc, NWQueries.Q7, "Q7", 9, 10, classOf[ColumnTableScan])
        case "Q8" => NWQueries.assertQuery(snc, NWQueries.Q8, "Q8", 6, totalProcessors,
          classOf[FilterExec])
        case "Q9" => NWQueries.assertQuery(snc, NWQueries.Q9, "Q9", 3, totalProcessors,
          classOf[ProjectExec])
        case "Q10" => NWQueries.assertQuery(snc, NWQueries.Q10, "Q10", 2, totalProcessors,
          classOf[FilterExec])
        case "Q11" => NWQueries.assertQuery(snc, NWQueries.Q11, "Q11", 4, totalProcessors,
          classOf[ProjectExec])
        case "Q12" => NWQueries.assertQuery(snc, NWQueries.Q12, "Q12", 2, 3, classOf[FilterExec])
        case "Q13" => NWQueries.assertQuery(snc, NWQueries.Q13, "Q13", 2, numDefaultPartitions,
          classOf[FilterExec])
        case "Q14" => NWQueries.assertQuery(snc, NWQueries.Q14, "Q14", 69, 1, classOf[FilterExec])
        case "Q15" => NWQueries.assertQuery(snc, NWQueries.Q15, "Q15", 5, totalProcessors,
          classOf[FilterExec])
        case "Q16" => NWQueries.assertQuery(snc, NWQueries.Q16, "Q16", 8, totalProcessors,
          classOf[FilterExec])
        case "Q17" => NWQueries.assertQuery(snc, NWQueries.Q17, "Q17", 3, totalProcessors,
          classOf[FilterExec])
        case "Q18" => NWQueries.assertQuery(snc, NWQueries.Q18, "Q18", 9, totalProcessors,
          classOf[ProjectExec])
        case "Q19" => NWQueries.assertQuery(snc, NWQueries.Q19, "Q19", 13, numDefaultPartitions,
          classOf[ProjectExec])
        case "Q20" => NWQueries.assertQuery(snc, NWQueries.Q20, "Q20", 1, 1, classOf[ProjectExec])
        case "Q21" => NWQueries.assertQuery(snc, NWQueries.Q21, "Q21", 1, 1,
          classOf[ColumnTableScan])
        case "Q22" => NWQueries.assertQuery(snc, NWQueries.Q22, "Q22", 1, 2, classOf[ProjectExec])
        case "Q23" => NWQueries.assertQuery(snc, NWQueries.Q23, "Q23", 1, 1,
          classOf[ColumnTableScan])
        case "Q24" => NWQueries.assertQuery(snc, NWQueries.Q24, "Q24", 4, 5, classOf[ProjectExec])
        case "Q25" => NWQueries.assertJoin(snc, NWQueries.Q25, "Q25", 1, 1, classOf[RowTableScan])
        case "Q26" => NWQueries.assertJoin(snc, NWQueries.Q26, "Q26", 86, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q27" => NWQueries.assertJoin(snc, NWQueries.Q27, "Q27", 9, 123,
          classOf[BroadcastHashJoinExec])
        case "Q28" => NWQueries.assertJoin(snc, NWQueries.Q28, "Q28", 12, totalProcessors,
          classOf[ColumnTableScan])
        case "Q29" => NWQueries.assertJoin(snc, NWQueries.Q29, "Q29", 8, 123,
          classOf[SortMergeJoinExec])
        case "Q30" => NWQueries.assertJoin(snc, NWQueries.Q30, "Q30", 8, 123,
          classOf[SortMergeJoinExec])
        case "Q31" => NWQueries.assertJoin(snc, NWQueries.Q31, "Q31", 830, totalProcessors,
          classOf[LocalJoin])
        case "Q32" => NWQueries.assertJoin(snc, NWQueries.Q32, "Q32", 8, 9, classOf[LocalJoin])
        case "Q33" => NWQueries.assertJoin(snc, NWQueries.Q33, "Q33", 37, totalProcessors,
          classOf[LocalJoin])
        case "Q34" => NWQueries.assertJoin(snc, NWQueries.Q34, "Q34", 5, totalProcessors,
          classOf[LocalJoin])
        case "Q35" => NWQueries.assertJoin(snc, NWQueries.Q35, "Q35", 3, 4, classOf[LocalJoin])
        case "Q36" => NWQueries.assertJoin(snc, NWQueries.Q36, "Q36", 290, totalProcessors,
          classOf[LocalJoin])
        case "Q37" => NWQueries.assertJoin(snc, NWQueries.Q37, "Q37", 77, totalProcessors,
          classOf[LocalJoin])
        case "Q38" => NWQueries.assertJoin(snc, NWQueries.Q38, "Q38", 2155, totalProcessors,
          classOf[LocalJoin])
        case "Q39" => NWQueries.assertJoin(snc, NWQueries.Q39, "Q39", 9, 123,
          classOf[LocalJoin])
        case "Q40" => NWQueries.assertJoin(snc, NWQueries.Q40, "Q40", 830, totalProcessors,
          classOf[LocalJoin])
        case "Q41" => NWQueries.assertJoin(snc, NWQueries.Q41, "Q41", 2155, 13,
          classOf[LocalJoin])
        case "Q42" => NWQueries.assertJoin(snc, NWQueries.Q42, "Q42", 22, totalProcessors,
          classOf[LocalJoin])
        case "Q43" => NWQueries.assertJoin(snc, NWQueries.Q43, "Q43", 830, 13,
          classOf[SortMergeJoinExec])
        case "Q44" => NWQueries.assertJoin(snc, NWQueries.Q44, "Q44", 830, 13,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q45" => NWQueries.assertJoin(snc, NWQueries.Q45, "Q45", 1788650, 13,
          classOf[CartesianProductExec])
        case "Q46" => NWQueries.assertJoin(snc, NWQueries.Q46, "Q46", 1788650, 13,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q47" => NWQueries.assertJoin(snc, NWQueries.Q47, "Q47", 1788650, 13,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q48" => NWQueries.assertJoin(snc, NWQueries.Q48, "Q48", 1788650, 37,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q49" => NWQueries.assertJoin(snc, NWQueries.Q49, "Q49", 1788650, 37,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q50" => NWQueries.assertJoin(snc, NWQueries.Q50, "Q50", 2155, 13,
          classOf[LocalJoin])
        case "Q51" => NWQueries.assertJoin(snc, NWQueries.Q51, "Q51", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q52" => NWQueries.assertJoin(snc, NWQueries.Q52, "Q52", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q53" => NWQueries.assertJoin(snc, NWQueries.Q53, "Q53", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q54" => NWQueries.assertJoin(snc, NWQueries.Q54, "Q54", 2155, 13,
          classOf[SortMergeJoinExec])
        case "Q55" => NWQueries.assertJoin(snc, NWQueries.Q55, "Q55", 21, totalProcessors,
          classOf[LocalJoin])
        case "Q56" => NWQueries.assertJoin(snc, NWQueries.Q56, "Q56", 8, 1, classOf[LocalJoin])
      }
    }
  }

  private def validateColocatedTableQueries(snc: SnappyContext): Unit = {

    val numDefaultPartitions = ((totalProcessors - 4) to (totalProcessors + 4)).toArray
    for (q <- NWQueries.queries) {
      q._1 match {
        case "Q1" => NWQueries.assertQuery(snc, NWQueries.Q1, "Q1", 8, 1, classOf[RowTableScan])
        case "Q2" => NWQueries.assertQuery(snc, NWQueries.Q2, "Q2", 91, numDefaultPartitions,
          classOf[ColumnTableScan])
        case "Q3" => NWQueries.assertQuery(snc, NWQueries.Q3, "Q3", 830, numDefaultPartitions,
          classOf[RowTableScan])
        case "Q4" => NWQueries.assertQuery(snc, NWQueries.Q4, "Q4", 9, 3, classOf[RowTableScan])
        case "Q5" => NWQueries.assertQuery(snc, NWQueries.Q5, "Q5", 9, 10, classOf[RowTableScan])
        case "Q6" => NWQueries.assertQuery(snc, NWQueries.Q6, "Q6", 9, 10, classOf[RowTableScan])
        case "Q7" => NWQueries.assertQuery(snc, NWQueries.Q7, "Q7", 9, 10, classOf[RowTableScan])
        case "Q8" => NWQueries.assertQuery(snc, NWQueries.Q8, "Q8", 6, 3, classOf[FilterExec])
        case "Q9" => NWQueries.assertQuery(snc, NWQueries.Q9, "Q9", 3, 3, classOf[ProjectExec])
        case "Q10" => NWQueries.assertQuery(snc, NWQueries.Q10, "Q10", 2, 3, classOf[FilterExec])
        case "Q11" => NWQueries.assertQuery(snc, NWQueries.Q11, "Q11", 4, 3, classOf[ProjectExec])
        case "Q12" => NWQueries.assertQuery(snc, NWQueries.Q12, "Q12", 2, 3, classOf[FilterExec])
        case "Q13" => NWQueries.assertQuery(snc, NWQueries.Q13, "Q13", 2, numDefaultPartitions,
          classOf[FilterExec])
        case "Q14" => NWQueries.assertQuery(snc, NWQueries.Q14, "Q14", 69, totalProcessors,
          classOf[FilterExec])
        case "Q15" => NWQueries.assertQuery(snc, NWQueries.Q15, "Q15", 5, 3, classOf[FilterExec])
        case "Q16" => NWQueries.assertQuery(snc, NWQueries.Q16, "Q16", 8, 3, classOf[FilterExec])
        case "Q17" => NWQueries.assertQuery(snc, NWQueries.Q17, "Q17", 3, 3, classOf[FilterExec])
        case "Q18" => NWQueries.assertQuery(snc, NWQueries.Q18, "Q18", 9, 3, classOf[ProjectExec])
        case "Q19" => NWQueries.assertQuery(snc, NWQueries.Q19, "Q19", 13, numDefaultPartitions,
          classOf[ProjectExec])
        case "Q20" => NWQueries.assertQuery(snc, NWQueries.Q20, "Q20", 1, 1, classOf[ProjectExec])
        case "Q21" => NWQueries.assertQuery(snc, NWQueries.Q21, "Q21", 1, 1, classOf[RowTableScan])
        case "Q22" => NWQueries.assertQuery(snc, NWQueries.Q22, "Q22", 1, 2, classOf[ProjectExec])
        case "Q23" => NWQueries.assertQuery(snc, NWQueries.Q23, "Q23", 1, 1, classOf[RowTableScan])
        case "Q24" => NWQueries.assertQuery(snc, NWQueries.Q24, "Q24", 4, 5, classOf[ProjectExec])
        case "Q25" => NWQueries.assertJoin(snc, NWQueries.Q25, "Q25", 1, 10,
          classOf[ColumnTableScan])
        case "Q26" => NWQueries.assertJoin(snc, NWQueries.Q26, "Q26", 86, 19,
          classOf[BroadcastHashJoinExec])
        case "Q27" => NWQueries.assertJoin(snc, NWQueries.Q27, "Q27", 9, 123,
          classOf[SortMergeJoinExec])
        case "Q28" => NWQueries.assertJoin(snc, NWQueries.Q28, "Q28", 12, totalProcessors,
          classOf[ColumnTableScan])
        case "Q29" => NWQueries.assertJoin(snc, NWQueries.Q29, "Q29", 8, 123,
          classOf[BroadcastHashJoinExec])
        case "Q30" => NWQueries.assertJoin(snc, NWQueries.Q30, "Q30", 8, 123,
          classOf[BroadcastHashJoinExec])
        case "Q31" => NWQueries.assertJoin(snc, NWQueries.Q31, "Q31", 830, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q32" => NWQueries.assertJoin(snc, NWQueries.Q32, "Q32", 8, 9,
          classOf[BroadcastHashJoinExec])
        case "Q33" => NWQueries.assertJoin(snc, NWQueries.Q33, "Q33", 37, 9,
          classOf[BroadcastHashJoinExec])
        case "Q34" => NWQueries.assertJoin(snc, NWQueries.Q34, "Q34", 5, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q35" => NWQueries.assertJoin(snc, NWQueries.Q35, "Q35", 3, 4,
          classOf[BroadcastHashJoinExec])
        case "Q36" => NWQueries.assertJoin(snc, NWQueries.Q36, "Q36", 290, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q37" => NWQueries.assertJoin(snc, NWQueries.Q37, "Q37", 77, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q38" => NWQueries.assertJoin(snc, NWQueries.Q38, "Q38", 2155, totalProcessors,
          classOf[LocalJoin])
        case "Q39" => NWQueries.assertJoin(snc, NWQueries.Q39, "Q39", 9, 123,
          classOf[BroadcastHashJoinExec])
        case "Q40" => NWQueries.assertJoin(snc, NWQueries.Q40, "Q40", 830, 19,
          classOf[BroadcastHashJoinExec])
        case "Q41" => NWQueries.assertJoin(snc, NWQueries.Q41, "Q41", 2155, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q42" => NWQueries.assertJoin(snc, NWQueries.Q42, "Q42", 22, totalProcessors,
          classOf[BroadcastHashJoinExec])
        case "Q43" => NWQueries.assertJoin(snc, NWQueries.Q43, "Q43", 830, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q44" => NWQueries.assertJoin(snc, NWQueries.Q44, "Q44", 830, 19,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q45" => NWQueries.assertJoin(snc, NWQueries.Q45, "Q45", 1788650, 19,
          classOf[CartesianProductExec])
        case "Q46" => NWQueries.assertJoin(snc, NWQueries.Q46, "Q46", 1788650, 19,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q47" => NWQueries.assertJoin(snc, NWQueries.Q47, "Q47", 1788650, 329,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q48" => NWQueries.assertJoin(snc, NWQueries.Q48, "Q48", 1788650, 43,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q49" => NWQueries.assertJoin(snc, NWQueries.Q49, "Q49", 1788650, 43,
          classOf[BroadcastNestedLoopJoinExec])
        case "Q50" => NWQueries.assertJoin(snc, NWQueries.Q50, "Q50", 2155, totalProcessors,
          classOf[LocalJoin])
        case "Q51" => NWQueries.assertJoin(snc, NWQueries.Q51, "Q51", 2155, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q52" => NWQueries.assertJoin(snc, NWQueries.Q52, "Q52", 2155, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q53" => NWQueries.assertJoin(snc, NWQueries.Q53, "Q53", 2155, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q54" => NWQueries.assertJoin(snc, NWQueries.Q54, "Q54", 2155, totalProcessors,
          classOf[SortMergeJoinExec])
        case "Q55" => NWQueries.assertJoin(snc, NWQueries.Q55, "Q55", 21, totalProcessors,
          classOf[LocalJoin])
        case "Q56" => NWQueries.assertJoin(snc, NWQueries.Q56, "Q56", 8, 1, classOf[LocalJoin])
      }
    }
  }

  private def createAndLoadColumnTableUsingJDBC(stmt: Statement, snc: SnappyContext): Unit = {

    stmt.executeUpdate(NWQueries.products_table + " USING column options (" +
        "partition_by 'ProductID,SupplierID', buckets '3', redundancy '3')")
    NWQueries.products(snc).collect().foreach(row => {
      val colValues = row.toSeq
      val sqlQuery: String = s"INSERT INTO products VALUES(${colValues.head}, " +
          s"'${colValues(1).toString.replace("'", "")}',${colValues(2)}, ${colValues(3)}, " +
          s"'${colValues(4).toString.replace("'", "")}',${colValues(5)}, ${colValues(6)}, " +
          s"${colValues(7)}, ${colValues(8)},  ${colValues(9)})"
      stmt.executeUpdate(sqlQuery)
    })
  }
}

object NorthWindDUnitTest {

  def createAndLoadSparkTables(sqlContext: SQLContext): Unit = {
    NWQueries.regions(sqlContext).registerTempTable("regions")
    NWQueries.categories(sqlContext).registerTempTable("categories")
    NWQueries.shippers(sqlContext).registerTempTable("shippers")
    NWQueries.employees(sqlContext).registerTempTable("employees")
    NWQueries.customers(sqlContext).registerTempTable("customers")
    NWQueries.orders(sqlContext).registerTempTable("orders")
    NWQueries.order_details(sqlContext).registerTempTable("order_details")
    NWQueries.products(sqlContext).registerTempTable("products")
    NWQueries.suppliers(sqlContext).registerTempTable("suppliers")
    NWQueries.territories(sqlContext).registerTempTable("territories")
    NWQueries.employee_territories(sqlContext).registerTempTable("employee_territories")
  }

  def createAndLoadReplicatedTables(snc: SnappyContext): Unit = {
    snc.sql(NWQueries.regions_table)
    NWQueries.regions(snc).write.insertInto("regions")

    snc.sql(NWQueries.categories_table)
    NWQueries.categories(snc).write.insertInto("categories")

    snc.sql(NWQueries.shippers_table)
    NWQueries.shippers(snc).write.insertInto("shippers")

    snc.sql(NWQueries.employees_table)
    NWQueries.employees(snc).write.insertInto("employees")

    snc.sql(NWQueries.customers_table)
    NWQueries.customers(snc).write.insertInto("customers")

    snc.sql(NWQueries.orders_table)
    NWQueries.orders(snc).write.insertInto("orders")

    snc.sql(NWQueries.order_details_table)
    NWQueries.order_details(snc).write.insertInto("order_details")

    snc.sql(NWQueries.products_table)
    NWQueries.products(snc).write.insertInto("products")

    snc.sql(NWQueries.suppliers_table)
    NWQueries.suppliers(snc).write.insertInto("suppliers")

    snc.sql(NWQueries.territories_table)
    NWQueries.territories(snc).write.insertInto("territories")

    snc.sql(NWQueries.employee_territories_table)
    NWQueries.employee_territories(snc).write.insertInto("employee_territories")
  }

  def createAndLoadColumnTables(snc: SnappyContext): Unit = {

    snc.sql(NWQueries.regions_table)
    NWQueries.regions(snc).write.insertInto("regions")

    snc.sql(NWQueries.categories_table)
    NWQueries.categories(snc).write.insertInto("categories")

    snc.sql(NWQueries.shippers_table)
    NWQueries.shippers(snc).write.insertInto("shippers")

    snc.sql(NWQueries.employees_table + " using column options()")
    NWQueries.employees(snc).write.insertInto("employees")

    snc.sql(NWQueries.customers_table)
    NWQueries.customers(snc).write.insertInto("customers")

    snc.sql(NWQueries.orders_table + " using column options (" +
        "partition_by 'OrderId', buckets '13', redundancy '1')")
    NWQueries.orders(snc).write.insertInto("orders")

    snc.sql(NWQueries.order_details_table + " using column options (" +
        "partition_by 'OrderId', buckets '13', COLOCATE_WITH 'orders', " +
        "redundancy '1')")
    NWQueries.order_details(snc).write.insertInto("order_details")

    snc.sql(NWQueries.products_table + " USING column options (" +
        "partition_by 'ProductID,SupplierID', buckets '17', redundancy '1')")
    NWQueries.products(snc).write.insertInto("products")

    snc.sql(NWQueries.suppliers_table +
        " USING column options (PARTITION_BY 'SupplierID', buckets '123' )")
    NWQueries.suppliers(snc).write.insertInto("suppliers")

    snc.sql(NWQueries.territories_table +
        " using column options (partition_by 'TerritoryID', buckets '3')")
    NWQueries.territories(snc).write.insertInto("territories")

    snc.sql(NWQueries.employee_territories_table +
        " using row options(partition_by 'EmployeeID', buckets '1')")
    NWQueries.employee_territories(snc).write.insertInto("employee_territories")
  }

  def createAndLoadColocatedTables(snc: SnappyContext): Unit = {

    snc.sql(NWQueries.regions_table)
    NWQueries.regions(snc).write.insertInto("regions")

    snc.sql(NWQueries.categories_table)
    NWQueries.categories(snc).write.insertInto("categories")

    snc.sql(NWQueries.shippers_table)
    NWQueries.shippers(snc).write.insertInto("shippers")

    snc.sql(NWQueries.employees_table +
        " using row options( partition_by 'EmployeeID', buckets '3')")
    NWQueries.employees(snc).write.insertInto("employees")

    snc.sql(NWQueries.customers_table + " using column options(" +
        "partition_by 'CustomerID', buckets '19', redundancy '1')")
    NWQueries.customers(snc).write.insertInto("customers")

    snc.sql(NWQueries.orders_table + " using row options (" +
        "partition_by 'CustomerID', buckets '19', " +
        "colocate_with 'customers', redundancy '1')")
    NWQueries.orders(snc).write.insertInto("orders")

    snc.sql(NWQueries.order_details_table + " using row options (" +
        "partition_by 'ProductID', buckets '329', redundancy '1')")
    NWQueries.order_details(snc).write.insertInto("order_details")

    snc.sql(NWQueries.products_table +
        " USING column options ( partition_by 'ProductID', buckets '329'," +
        " colocate_with 'order_details', redundancy '1')")
    NWQueries.products(snc).write.insertInto("products")

    snc.sql(NWQueries.suppliers_table +
        " USING column options (PARTITION_BY 'SupplierID', buckets '123')")
    NWQueries.suppliers(snc).write.insertInto("suppliers")

    snc.sql(NWQueries.territories_table +
        " using column options (partition_by 'TerritoryID', buckets '3')")
    NWQueries.territories(snc).write.insertInto("territories")

    snc.sql(NWQueries.employee_territories_table + " using row options(" +
        "partition_by 'TerritoryID', buckets '3', colocate_with 'territories')")
    NWQueries.employee_territories(snc).write.insertInto("employee_territories")
  }

  protected def getTempDir(dirName: String): String = {
    val log: File = new File(".")
    var dest: String = null
    dest = log.getCanonicalPath + File.separator + dirName
    val tempDir: File = new File(dest)
    if (!tempDir.exists) tempDir.mkdir()
    return tempDir.getAbsolutePath
  }

  def assertQueryFullResultSet(snc: SnappyContext, sqlString: String, numRows: Int, queryNum: String, tableType: String, pw: PrintWriter, sqlContext: SQLContext): Any = {
    var snappyDF = snc.sql(sqlString)
    var sparkDF = sqlContext.sql(sqlString);
    val snappyQueryFileName = s"Snappy_${queryNum}.out"
    val sparkQueryFileName = s"Spark_${queryNum}.out"
    val snappyDest: String = getTempDir("snappyQueryFiles_" + tableType) + File.separator + snappyQueryFileName
    val sparkDest: String = getTempDir("sparkQueryFiles") + File.separator + sparkQueryFileName
    val sparkFile: File = new java.io.File(sparkDest)
    val snappyFile = new java.io.File(snappyDest)
    val col1 = sparkDF.schema.fieldNames(0)
    val col = sparkDF.schema.fieldNames.filter(!_.equals(col1)).toSeq
      snappyDF = snappyDF.coalesce(1).orderBy(col1, col: _*)
      writeToFile(snappyDF, snappyDest, snc)
      pw.println(s"${queryNum} Result Collected in file $snappyDest")
    if (sparkFile.listFiles() == null) {
      sparkDF = sparkDF.coalesce(1).orderBy(col1, col: _*)
      writeToFile(sparkDF, sparkDest, snc)
      pw.println(s"${queryNum} Result Collected in file $sparkDest")
    }
    val expectedFile = sparkFile.listFiles.filter(_.getName.endsWith(".csv"))
    val actualFile = snappyFile.listFiles.filter(_.getName.endsWith(".csv"))
    val expectedLineSet = Source.fromFile(expectedFile.iterator.next()).getLines()
    val actualLineSet = Source.fromFile(actualFile.iterator.next()).getLines
    while (expectedLineSet.hasNext && actualLineSet.hasNext) {
      val expectedLine = expectedLineSet.next()
      val actualLine = actualLineSet.next()
      if (!actualLine.equals(expectedLine)) {
        pw.println(s"\n** For ${queryNum} result mismatch observed**")
        pw.println(s"\nExpected Result \n: $expectedLine")
        pw.println(s"\nActual Result   \n: $actualLine")
        pw.println(s"\nQuery =" + sqlString + " Table Type : " + tableType)
        assert(false, s"\n** For ${queryNum} result mismatch observed** \nExpected Result \n: $expectedLine \nActual Result   \n: $actualLine \nQuery =" + sqlString + " Table Type : " + tableType)
      }
    }
    if (actualLineSet.hasNext || expectedLineSet.hasNext) {
      pw.println(s"\nFor ${queryNum} result count mismatch observed")
      assert(false, s"\nFor ${queryNum} result count mismatch observed")
    }
    pw.flush()
  }

  def assertJoinFullResultSet(snc: SnappyContext, sqlString: String, numRows: Int, queryNum: String, tableType: String, pw: PrintWriter, sqlContext: SQLContext): Any = {
    snc.sql("set spark.sql.crossJoin.enabled = true")
    sqlContext.sql("set spark.sql.crossJoin.enabled = true")
    assertQueryFullResultSet(snc, sqlString, numRows, queryNum, tableType, pw, sqlContext)
  }

  def dataTypeConverter(row: Row): Row = {
    val md = row.toSeq.map {
      //case d: Double => math.floor(d * 1000.0 + 0.5) // round to three digits
      case d: Double => "%18.1f".format(d).trim().toDouble
      case de: BigDecimal => {
        de.setScale(2, BigDecimal.RoundingMode.HALF_UP)
      }
      case i: Integer => {
        i
      }
      case v => v
    }
    Row.fromSeq(md)
  }

  def writeToFile(df: DataFrame, dest: String, snc: SnappyContext): Unit = {
    import snc.implicits._
    df.map(dataTypeConverter)(RowEncoder(df.schema))
      .map(row => {
        var str = ""
        row.toSeq.foreach(e => {
          if (e != null)
            str = str + e.toString + ","
          else
            str = str + "NULL" + ","
        })
        str
      }).write.format("org.apache.spark.sql.execution.datasources.csv.CSVFileFormat").option("header", false).save(dest)
  }

  def validateQueriesFullResultSet(snc: SnappyContext, tableType: String, pw: PrintWriter, sqlContext: SQLContext): Unit = {
    for (q <- NWQueries.queries) {
      q._1 match {
        case "Q1" => assertQueryFullResultSet(snc, NWQueries.Q1, 8, "Q1", tableType, pw, sqlContext)
        case "Q2" => assertQueryFullResultSet(snc, NWQueries.Q2, 91, "Q2", tableType, pw, sqlContext)
        case "Q3" => assertQueryFullResultSet(snc, NWQueries.Q3, 830, "Q3", tableType, pw, sqlContext)
        case "Q4" => assertQueryFullResultSet(snc, NWQueries.Q4, 9, "Q4", tableType, pw, sqlContext)
        case "Q5" => assertQueryFullResultSet(snc, NWQueries.Q5, 9, "Q5", tableType, pw, sqlContext)
        case "Q6" => assertQueryFullResultSet(snc, NWQueries.Q6, 9, "Q6", tableType, pw, sqlContext)
        case "Q7" => assertQueryFullResultSet(snc, NWQueries.Q7, 9, "Q7", tableType, pw, sqlContext)
        case "Q8" => assertQueryFullResultSet(snc, NWQueries.Q8, 6, "Q8", tableType, pw, sqlContext)
        case "Q9" => assertQueryFullResultSet(snc, NWQueries.Q9, 3, "Q9", tableType, pw, sqlContext)
        case "Q10" => assertQueryFullResultSet(snc, NWQueries.Q10, 2, "Q10", tableType, pw, sqlContext)
        case "Q11" => assertQueryFullResultSet(snc, NWQueries.Q11, 4, "Q11", tableType, pw, sqlContext)
        case "Q12" => assertQueryFullResultSet(snc, NWQueries.Q12, 2, "Q12", tableType, pw, sqlContext)
        case "Q13" => assertQueryFullResultSet(snc, NWQueries.Q13, 2, "Q13", tableType, pw, sqlContext)
        case "Q14" => assertQueryFullResultSet(snc, NWQueries.Q14, 69, "Q14", tableType, pw, sqlContext)
        case "Q15" => assertQueryFullResultSet(snc, NWQueries.Q15, 5, "Q15", tableType, pw, sqlContext)
        case "Q16" => assertQueryFullResultSet(snc, NWQueries.Q16, 8, "Q16", tableType, pw, sqlContext)
        case "Q17" => assertQueryFullResultSet(snc, NWQueries.Q17, 3, "Q17", tableType, pw, sqlContext)
        case "Q18" => assertQueryFullResultSet(snc, NWQueries.Q18, 9, "Q18", tableType, pw, sqlContext)
        case "Q19" => assertQueryFullResultSet(snc, NWQueries.Q19, 13, "Q19", tableType, pw, sqlContext)
        case "Q20" => assertQueryFullResultSet(snc, NWQueries.Q20, 1, "Q20", tableType, pw, sqlContext)
        case "Q21" => assertQueryFullResultSet(snc, NWQueries.Q21, 1, "Q21", tableType, pw, sqlContext)
        case "Q22" => assertQueryFullResultSet(snc, NWQueries.Q22, 1, "Q22", tableType, pw, sqlContext)
        case "Q23" => assertQueryFullResultSet(snc, NWQueries.Q23, 1, "Q23", tableType, pw, sqlContext)
        case "Q24" => assertQueryFullResultSet(snc, NWQueries.Q24, 4, "Q24", tableType, pw, sqlContext)
        case "Q25" => assertJoinFullResultSet(snc, NWQueries.Q25, 1, "Q25", tableType, pw, sqlContext)
        case "Q26" => assertJoinFullResultSet(snc, NWQueries.Q26, 86, "Q26", tableType, pw, sqlContext)
        case "Q27" => assertJoinFullResultSet(snc, NWQueries.Q27, 9, "Q27", tableType, pw, sqlContext)
        case "Q28" => assertJoinFullResultSet(snc, NWQueries.Q28, 12, "Q28", tableType, pw, sqlContext)
        case "Q29" => assertJoinFullResultSet(snc, NWQueries.Q29, 8, "Q29", tableType, pw, sqlContext)
        case "Q30" => assertJoinFullResultSet(snc, NWQueries.Q30, 8, "Q30", tableType, pw, sqlContext)
        case "Q31" => assertJoinFullResultSet(snc, NWQueries.Q31, 830, "Q31", tableType, pw, sqlContext)
        case "Q32" => assertJoinFullResultSet(snc, NWQueries.Q32, 8, "Q32", tableType, pw, sqlContext)
        case "Q33" => assertJoinFullResultSet(snc, NWQueries.Q33, 37, "Q33", tableType, pw, sqlContext)
        case "Q34" => assertJoinFullResultSet(snc, NWQueries.Q34, 5, "Q34", tableType, pw, sqlContext)
        case "Q35" => assertJoinFullResultSet(snc, NWQueries.Q35, 3, "Q35", tableType, pw, sqlContext)
        case "Q36" => assertJoinFullResultSet(snc, NWQueries.Q36, 290, "Q36", tableType, pw, sqlContext)
        case "Q37" => //assertJoinFullResultSet(snc, NWQueries.Q37, 77, "Q37", tableType, pw, sqlContext)
        case "Q38" => assertJoinFullResultSet(snc, NWQueries.Q38, 2155, "Q38", tableType, pw, sqlContext)
        case "Q39" => assertJoinFullResultSet(snc, NWQueries.Q39, 9, "Q39", tableType, pw, sqlContext)
        case "Q40" => assertJoinFullResultSet(snc, NWQueries.Q40, 830, "Q40", tableType, pw, sqlContext)
        case "Q41" => assertJoinFullResultSet(snc, NWQueries.Q41, 2155, "Q41", tableType, pw, sqlContext)
        case "Q42" => assertJoinFullResultSet(snc, NWQueries.Q42, 22, "Q42", tableType, pw, sqlContext)
        case "Q43" => assertJoinFullResultSet(snc, NWQueries.Q43, 830, "Q43", tableType, pw, sqlContext)
        case "Q44" => assertJoinFullResultSet(snc, NWQueries.Q44, 830, "Q44", tableType, pw, sqlContext)
        case "Q45" => assertJoinFullResultSet(snc, NWQueries.Q45, 1788650, "Q45", tableType, pw, sqlContext)
        case "Q46" => assertJoinFullResultSet(snc, NWQueries.Q46, 1788650, "Q46", tableType, pw, sqlContext)
        case "Q47" => assertJoinFullResultSet(snc, NWQueries.Q47, 1788650, "Q47", tableType, pw, sqlContext)
        case "Q48" => assertJoinFullResultSet(snc, NWQueries.Q48, 1788650, "Q48", tableType, pw, sqlContext)
        case "Q49" => assertJoinFullResultSet(snc, NWQueries.Q49, 1788650, "Q49", tableType, pw, sqlContext)
        case "Q50" => assertJoinFullResultSet(snc, NWQueries.Q50, 2155, "Q50", tableType, pw, sqlContext)
        case "Q51" => assertJoinFullResultSet(snc, NWQueries.Q51, 2155, "Q51", tableType, pw, sqlContext)
        case "Q52" => assertJoinFullResultSet(snc, NWQueries.Q52, 2155, "Q52", tableType, pw, sqlContext)
        case "Q53" => assertJoinFullResultSet(snc, NWQueries.Q53, 2155, "Q53", tableType, pw, sqlContext)
        case "Q54" => assertJoinFullResultSet(snc, NWQueries.Q54, 2155, "Q54", tableType, pw, sqlContext)
        case "Q55" => assertJoinFullResultSet(snc, NWQueries.Q55, 21, "Q55", tableType, pw, sqlContext)
        case "Q56" => assertJoinFullResultSet(snc, NWQueries.Q56, 8, "Q56", tableType, pw, sqlContext)
        case _ => println("OK")
      }
    }
  }

}