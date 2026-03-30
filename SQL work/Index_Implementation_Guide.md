# Index Implementation Guide for GID-589

**Target**: `dbo.vwOrderLineAttributeValues`
**Requirement**: Improve query performance for OrderLineInstances.sql script
**Implementation Date**: 2026-03-30

---

## Recommended Approach: Base Table Indexes

Since `vwOrderLineAttributeValues` cannot be directly indexed (lacks schema binding, contains LEFT JOIN), we'll create **covering indexes on the underlying base tables**.

---

## Index #1: OrderLineAttributeValues - Primary Index

### Purpose
Optimize the main query pattern in the view: filtering by specific attribute IDs and grouping by order line ID.

### SQL Script
```sql
USE [insight_test]
GO

-- Index for the main attribute value lookups
CREATE NONCLUSTERED INDEX [IX_OrderLineAttributeValues_atbID_olnID]
ON [dbo].[OrderLineAttributeValues] ([atbID], [olnID])
INCLUDE ([olnavValue])
WITH (
    ONLINE = ON,
    FILLFACTOR = 90,
    SORT_IN_TEMPDB = ON
);
GO
```

### Details
- **Columns**: `atbID`, `olnID` (key columns from the WHERE and GROUP BY clauses)
- **Include**: `olnavValue` (covering index - avoids key lookups)
- **Rationale**:
  - The view filters on `atbID IN (75, 76, 77, 520, 521, 1160)`
  - Groups by `olnID`
  - This index allows index seeks + no table lookups

---

## Index #2: AttributeList - Lookup Index

### Purpose
Optimize the LEFT JOIN to AttributeList for quality check descriptions.

### SQL Script
```sql
USE [insight_test]
GO

-- Index for attribute description lookups
CREATE NONCLUSTERED INDEX [IX_AttributeList_atbID_atblCode_atblActive]
ON [dbo].[AttributeList] ([atbID], [atblCode], [atblActive])
INCLUDE ([atblDescription])
WITH (
    ONLINE = ON,
    FILLFACTOR = 95,
    SORT_IN_TEMPDB = ON
);
GO
```

### Details
- **Columns**: `atbID`, `atblCode`, `atblActive` (all JOIN and filter conditions)
- **Include**: `atblDescription` (covering index)
- **Rationale**:
  - Matches the LEFT JOIN condition exactly
  - Filters for `atblActive = 1`
  - Covers all needed columns

---

## Alternative: Filtered Index for Specific Attributes

If you want to optimize specifically for the 6 attributes used in the view:

### SQL Script
```sql
USE [insight_test]
GO

-- Filtered index - only for the 6 attributes used in the view
CREATE NONCLUSTERED INDEX [IX_OrderLineAttributeValues_olnID_Filtered]
ON [dbo].[OrderLineAttributeValues] ([olnID], [atbID])
INCLUDE ([olnavValue])
WHERE [atbID] IN (75, 76, 77, 520, 521, 1160)
WITH (
    ONLINE = ON,
    FILLFACTOR = 90,
    SORT_IN_TEMPDB = ON
);
GO
```

### Details
- **Filtered WHERE clause**: Only indexes rows for the 6 attributes
- **Advantages**:
  - Smaller index size (better cache utilization)
  - Faster maintenance
  - Lower storage overhead
- **Trade-off**: Only helps queries filtering on these specific atbIDs

---

## Implementation Steps

### Pre-Implementation Checklist
- [ ] Backup database or verify backup exists
- [ ] Check current index fragmentation on affected tables
- [ ] Verify sufficient disk space (estimate: 10-20% of table size per index)
- [ ] Schedule during low-activity window if possible
- [ ] Document current execution plans for comparison

### Step 1: Analyze Current State
```sql
-- Check existing indexes on OrderLineAttributeValues
EXEC sp_helpindex 'dbo.OrderLineAttributeValues';

-- Check existing indexes on AttributeList
EXEC sp_helpindex 'dbo.AttributeList';

-- Check table sizes
SELECT
    t.name AS TableName,
    SUM(p.rows) AS RowCount,
    SUM(a.total_pages) * 8 / 1024 AS TotalSpaceMB
FROM sys.tables t
INNER JOIN sys.indexes i ON t.object_id = i.object_id
INNER JOIN sys.partitions p ON i.object_id = p.object_id AND i.index_id = p.index_id
INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id
WHERE t.name IN ('OrderLineAttributeValues', 'AttributeList')
GROUP BY t.name;
```

### Step 2: Capture Baseline Performance
```sql
-- Enable actual execution plan in SSMS
SET STATISTICS IO ON;
SET STATISTICS TIME ON;

-- Run the view query
SELECT * FROM dbo.vwOrderLineAttributeValues
WHERE olnIDolnAv IN (SELECT TOP 100 olnID FROM dbo.OrderLineAttributeValues);

-- Save execution plan and statistics
```

### Step 3: Create Indexes (Choose One Approach)

**Option A: Standard Indexes (Recommended)**
```sql
-- Run Index #1 script (see above)
-- Run Index #2 script (see above)
```

**Option B: With Filtered Index**
```sql
-- Run Alternative filtered index script (see above)
-- Run Index #2 script (see above)
```

### Step 4: Update Statistics
```sql
-- Update statistics after index creation
UPDATE STATISTICS dbo.OrderLineAttributeValues WITH FULLSCAN;
UPDATE STATISTICS dbo.AttributeList WITH FULLSCAN;
```

### Step 5: Verify Index Creation
```sql
-- Verify indexes were created
SELECT
    i.name AS IndexName,
    t.name AS TableName,
    i.type_desc AS IndexType,
    i.is_unique,
    i.filter_definition,
    COL_NAME(ic.object_id, ic.column_id) AS ColumnName,
    ic.is_included_column
FROM sys.indexes i
INNER JOIN sys.tables t ON i.object_id = t.object_id
INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id
WHERE t.name IN ('OrderLineAttributeValues', 'AttributeList')
    AND i.name LIKE 'IX_%'
ORDER BY t.name, i.name, ic.key_ordinal, ic.index_column_id;
```

### Step 6: Test Performance Improvement
```sql
-- Clear plan cache (test environment only)
DBCC FREEPROCCACHE;
DBCC DROPCLEANBUFFERS; -- Use with caution

-- Enable actual execution plan
SET STATISTICS IO ON;
SET STATISTICS TIME ON;

-- Run the same test query from Step 2
SELECT * FROM dbo.vwOrderLineAttributeValues
WHERE olnIDolnAv IN (SELECT TOP 100 olnID FROM dbo.OrderLineAttributeValues);

-- Compare:
-- - Logical reads (should be significantly lower)
-- - CPU time (should be lower)
-- - Execution plan (should show index seeks instead of scans)
```

### Step 7: Monitor and Validate
```sql
-- Check index usage after running production workload
SELECT
    OBJECT_NAME(i.object_id) AS TableName,
    i.name AS IndexName,
    s.user_seeks,
    s.user_scans,
    s.user_lookups,
    s.user_updates,
    s.last_user_seek,
    s.last_user_scan
FROM sys.dm_db_index_usage_stats s
INNER JOIN sys.indexes i ON s.object_id = i.object_id AND s.index_id = i.index_id
WHERE database_id = DB_ID('insight_test')
    AND i.name LIKE 'IX_%'
    AND OBJECT_NAME(i.object_id) IN ('OrderLineAttributeValues', 'AttributeList')
ORDER BY TableName, IndexName;
```

---

## Rollback Plan

If indexes cause issues or don't improve performance:

```sql
USE [insight_test]
GO

-- Drop Index #1
DROP INDEX IF EXISTS [IX_OrderLineAttributeValues_atbID_olnID]
ON [dbo].[OrderLineAttributeValues];
GO

-- Drop Index #2
DROP INDEX IF EXISTS [IX_AttributeList_atbID_atblCode_atblActive]
ON [dbo].[AttributeList];
GO

-- Drop Alternative Filtered Index (if created)
DROP INDEX IF EXISTS [IX_OrderLineAttributeValues_olnID_Filtered]
ON [dbo].[OrderLineAttributeValues];
GO

-- Update statistics
UPDATE STATISTICS dbo.OrderLineAttributeValues WITH FULLSCAN;
UPDATE STATISTICS dbo.AttributeList WITH FULLSCAN;
GO
```

---

## Expected Performance Improvements

### Before Indexes
- **Table scans** on OrderLineAttributeValues
- High logical reads (10,000+)
- Slow execution for large datasets

### After Indexes
- **Index seeks** on OrderLineAttributeValues
- Reduced logical reads (potentially 90%+ reduction)
- Faster execution time (2-10x improvement expected)
- Better join performance on AttributeList

---

## Maintenance Considerations

### Index Fragmentation Monitoring
```sql
-- Check fragmentation periodically
SELECT
    OBJECT_NAME(ips.object_id) AS TableName,
    i.name AS IndexName,
    ips.avg_fragmentation_in_percent,
    ips.page_count
FROM sys.dm_db_index_physical_stats(
    DB_ID('insight_test'),
    NULL,
    NULL,
    NULL,
    'LIMITED'
) ips
INNER JOIN sys.indexes i ON ips.object_id = i.object_id AND ips.index_id = i.index_id
WHERE ips.avg_fragmentation_in_percent > 10
    AND ips.page_count > 1000
    AND i.name LIKE 'IX_%'
ORDER BY ips.avg_fragmentation_in_percent DESC;
```

### Rebuild Schedule
- **Fragmentation > 30%**: Rebuild index
- **Fragmentation 10-30%**: Reorganize index
- **Fragmentation < 10%**: No action needed

```sql
-- Rebuild if needed
ALTER INDEX [IX_OrderLineAttributeValues_atbID_olnID]
ON [dbo].[OrderLineAttributeValues]
REBUILD WITH (ONLINE = ON, SORT_IN_TEMPDB = ON);
```

---

## Success Criteria

- [ ] Indexes created successfully without errors
- [ ] Execution plan shows index seeks instead of table scans
- [ ] Logical reads reduced by at least 50%
- [ ] Query execution time improved by at least 30%
- [ ] No negative impact on INSERT/UPDATE/DELETE operations
- [ ] Index usage statistics show regular seeks after 1 week

---

## Notes

1. **ONLINE = ON**: Allows concurrent access during index creation (Enterprise Edition only)
   - If Standard Edition, remove this option or schedule during maintenance window

2. **FILLFACTOR**:
   - 90% for OrderLineAttributeValues (allows room for updates)
   - 95% for AttributeList (mostly read-only lookup table)

3. **SORT_IN_TEMPDB = ON**: Uses tempdb for sort operations
   - Requires sufficient tempdb space
   - Generally faster if tempdb is on separate disk

4. **Production Deployment**:
   - Test in development first
   - Deploy to test environment
   - Monitor for 2-3 days before production
   - Schedule production deployment during maintenance window

---

## Contact & Approval

- **Implemented By**: _______________
- **Reviewed By**: _______________
- **Approved By**: Roy Genung
- **Date**: _______________

---

## References

- Jira Ticket: GID-589
- View Analysis: View_Analysis.md
- Requirements: GID-589_Requirements_Analysis.md
- View Script: /Users/vaibhavsingh/Documents/Udemy Learnings/SQL work/View.sql
