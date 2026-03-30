# Requirements Analysis: GID-589 - Add New Indexes to Insight

**Jira Ticket**: GID-589
**Title**: Add new indexes to Insight
**Requested By**: Roy Genung
**Analysis Date**: March 30, 2026
**Database**: insight_test / Insight

---

## Executive Summary

This ticket requests the creation of 3 new database indexes to improve query performance in the Insight database, specifically for the "OrderLineInstances.sql" script. The indexes target views related to order line attributes and instances.

---

## Requirements Detail

### Recommendation from Roy Genung

The following indexes are recommended for performance optimization:

### 1. Index on `dbo.vwOrderLineAttributeValues`

**Target Object**: View - `dbo.vwOrderLineAttributeValues`
**Index Column**: `olnIDolnAv`

```sql
-- Recommended index
CREATE INDEX ON olnIDolnAv
```

**Purpose**:
- Optimize queries filtering or joining by order line ID
- Improve performance when querying the pivoted attribute values view
- Support faster lookups in the OrderLineInstances script

**Notes**:
- This is the view previously analyzed (version 6, auto-generated)
- The column `olnIDolnAv` represents the order line ID from the underlying `OrderLineAttributeValues` table

---

### 2. Index on `dbo.vwOrderLineOptions`

**Target Object**: View - `dbo.vwOrderLineOptions`
**Index Column**: `olnID`

```sql
-- Recommended index
CREATE INDEX ON olnID
```

**Purpose**:
- Optimize queries filtering or joining by order line ID
- Improve performance for option-related queries
- Support OrderLineInstances script execution

**Notes**:
- View definition needs to be reviewed
- Likely follows similar pivoting pattern as vwOrderLineAttributeValues

---

### 3. Index on `Sales.vwOrderLineInstanceAttributeValuesOlin`

**Target Object**: View - `Sales.vwOrderLineInstanceAttributeValuesOlin`
**Index Columns**: Composite index on two columns
- `olnIDolInav`
- `olnIDInstanceolinav`

```sql
-- Recommended index
CREATE INDEX ON olnIDolInav, olnIDInstanceolinav
```

**Purpose**:
- Optimize queries filtering/joining on both order line ID and instance ID
- Support complex queries involving order line instances with their attributes
- Improve performance for multi-column filter conditions

**Notes**:
- This is in the Sales schema (different from dbo schema)
- Composite index suggests queries frequently filter on both columns together
- Order of columns in composite index matters for query optimization

---

## Technical Considerations

### Indexed Views vs Base Table Indexes

**Critical Issue**: The requirements specify creating indexes on **views**, not base tables.

In SQL Server, to create indexes on views (indexed/materialized views), the following requirements must be met:

#### Prerequisites for Indexed Views

1. **SCHEMABINDING** - View must be created with `WITH SCHEMABINDING` option
2. **Unique Clustered Index** - First index must be a unique clustered index
3. **Deterministic Functions** - Only deterministic functions allowed
4. **No Outer Joins** - Cannot use LEFT/RIGHT/FULL OUTER JOIN (with exceptions)
5. **ANSI Settings** - Specific ANSI settings required at creation time

#### Current Status

**vwOrderLineAttributeValues**:
- ✗ NOT created with SCHEMABINDING
- ✗ Contains LEFT JOIN (line 23)
- ✗ Cannot have indexes in current form

**Impact**: The view would need to be recreated to support indexing, which may:
- Break existing dependencies
- Require testing of all consuming queries
- Need coordination with the auto-generation process

---

## Implementation Approaches

### Option A: Create Indexed Views (Materialized Views)

**Steps**:
1. Modify view definitions to add `WITH SCHEMABINDING`
2. Create unique clustered index on each view
3. Create non-clustered indexes as specified
4. Test all consuming queries

**Pros**:
- Query performance improvement (pre-aggregated data)
- Indexes exactly as specified in requirements
- Automatic maintenance by SQL Server

**Cons**:
- Views need modification (may break auto-generation)
- Requires unique clustered index first
- Storage overhead for materialized data
- Maintenance overhead on base table DML operations
- May not be possible with LEFT JOIN in current view

**Complexity**: High

---

### Option B: Create Indexes on Underlying Base Tables

**Steps**:
1. Identify base tables used by each view
2. Create covering indexes on base tables
3. Include columns used in view output
4. Test performance improvement

**Example for vwOrderLineAttributeValues**:
```sql
-- Index on base table instead of view
CREATE NONCLUSTERED INDEX IX_OrderLineAttributeValues_atbID_olnID
ON dbo.OrderLineAttributeValues (atbID, olnID)
INCLUDE (olnavValue);
```

**Pros**:
- No view modification required
- Simpler implementation
- Benefits all queries on base tables
- No auto-generation conflicts

**Cons**:
- May not provide same performance as indexed view
- Requires understanding of underlying table structure
- May need multiple indexes across different tables

**Complexity**: Medium

---

### Option C: Hybrid Approach

Create indexed views for simple views and base table indexes for complex views.

**Complexity**: Medium-High

---

## Missing Information & Questions

### Clarifications Needed

1. **Index Names**: What naming convention should be used?
   - Standard pattern: `IX_TableName_Columns` or `IDX_TableName_Columns`?

2. **Implementation Method**: Which approach is preferred?
   - Indexed views (materialized)?
   - Base table indexes?
   - Specific guidance from Roy Genung?

3. **Index Options**: Are any special index options required?
   - `FILLFACTOR` setting?
   - `INCLUDE` columns for covering indexes?
   - `ONLINE` creation for production?
   - Compression settings?

4. **View Modifications**: If indexed views are required:
   - Is modifying auto-generated views acceptable?
   - How to handle the auto-generation process?
   - Who manages view versioning?

5. **Testing Requirements**:
   - What queries should be tested?
   - Performance baseline metrics needed?
   - Acceptance criteria for performance improvement?

6. **Environment**:
   - Which environment(s) should receive these indexes?
   - insight_test only, or also production?
   - Deployment process and approval required?

---

## Impact Analysis

### Affected Objects

1. **dbo.vwOrderLineAttributeValues**
   - Current consumers unknown
   - Auto-generated (version 6)
   - Managed by Attribute Associations form

2. **dbo.vwOrderLineOptions**
   - View definition needs review
   - Consumers unknown

3. **Sales.vwOrderLineInstanceAttributeValuesOlin**
   - Different schema (Sales vs dbo)
   - Likely more complex with instance relationships
   - Consumers unknown

### Dependencies to Check

- OrderLineInstances.sql script
- Reports or applications using these views
- ETL processes
- Other views that reference these views
- Stored procedures

---

## Recommended Action Plan

### Phase 1: Discovery & Analysis
- [ ] Review view definitions for `vwOrderLineOptions` and `Sales.vwOrderLineInstanceAttributeValuesOlin`
- [ ] Identify all consumers of the three views
- [ ] Analyze current execution plans for OrderLineInstances.sql
- [ ] Document current performance baseline
- [ ] Verify auto-generation process and impact

### Phase 2: Clarification
- [ ] Confirm implementation approach with Roy Genung
- [ ] Determine acceptable view modifications (if any)
- [ ] Agree on index naming conventions
- [ ] Define success criteria and acceptance tests

### Phase 3: Implementation
- [ ] Create implementation scripts based on approved approach
- [ ] Test in development environment
- [ ] Validate performance improvements
- [ ] Document rollback procedures

### Phase 4: Deployment
- [ ] Code review and approval
- [ ] Deploy to test environment
- [ ] Validation testing
- [ ] Deploy to production (if approved)
- [ ] Monitor performance post-deployment

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Breaking auto-generation process | High | Coordinate with form owners; document manual changes |
| Performance degradation on writes | Medium | Test DML performance; consider filtered indexes |
| View dependencies break | High | Identify all consumers; thorough testing |
| Insufficient storage for indexed views | Medium | Calculate storage requirements; monitor space |
| Index maintenance overhead | Low | Monitor index fragmentation; plan maintenance |
| Deployment rollback complexity | Medium | Prepare tested rollback scripts |

---

## Estimated Effort

| Phase | Effort (Hours) | Notes |
|-------|----------------|-------|
| Discovery & Analysis | 4-6 | View analysis, dependency mapping |
| Clarification | 1-2 | Stakeholder communication |
| Implementation (Option A) | 8-12 | If indexed views required |
| Implementation (Option B) | 4-6 | If base table indexes |
| Testing & Validation | 4-6 | Performance testing, regression tests |
| Documentation | 2-3 | Scripts, runbooks, knowledge transfer |
| **Total (Option A)** | **19-29 hours** | Indexed views approach |
| **Total (Option B)** | **15-23 hours** | Base table indexes approach |

---

## References

- **Jira Ticket**: GID-589
- **Related Analysis**: View_Analysis.md (vwOrderLineAttributeValues)
- **SQL Server Documentation**: [Indexed Views](https://docs.microsoft.com/en-us/sql/relational-databases/views/create-indexed-views)
- **Script Context**: OrderLineInstances.sql

---

## Approval & Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Requestor | Roy Genung | | |
| DBA Review | | | |
| Technical Lead | | | |
| Deployment Approval | | | |

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-03-30 | Analysis | Initial requirements analysis |
