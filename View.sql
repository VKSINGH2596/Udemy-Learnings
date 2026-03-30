USE [insight_test]
GO

/****** Object:  View [dbo].[vwOrderLineAttributeValues]    Script Date: 3/25/2026 3:40:33 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

ALTER   VIEW [dbo].[vwOrderLineAttributeValues] AS
-- Automatic view version: [6] created [2024-09-10 08:44:46] by [HMI\GGAFNN]. If you would like to add/delete attributes from this view, please modify "add to view" field in Attribute Associations form.
SELECT 
    av.olnID AS [olnIDolnAv]
    ,MAX(CASE WHEN av.atbID = 75 THEN av.olnavValue END) AS [Product_Short_Desc]
    ,MAX(CASE WHEN av.atbID = 76 THEN av.olnavValue END) AS [Product_Line]
    ,MAX(CASE WHEN av.atbID = 77 THEN av.olnavValue END) AS [Option_String]
    ,MAX(CASE WHEN av.atbID = 520 THEN av.olnavValue END) AS [SAPPID]
    ,MAX(CASE WHEN av.atbID = 521 THEN av.olnavValue END) AS [BAANProductionOrder]
    ,MAX(CASE WHEN av.atbID = 1160 THEN av.olnavValue END) AS [QualityCheckRequired]
    ,MAX(CASE WHEN av.atbID = 1160 THEN atbl.atblDescription END) AS [QualityCheckRequired Description]
FROM dbo.OrderLineAttributeValues av
LEFT JOIN dbo.AttributeList atbl ON atbl.atbID = av.atbID AND atbl.atblCode = av.olnavValue AND atbl.atblActive = 1
WHERE av.atbID IN (75, 76, 77, 520, 521, 1160)
GROUP BY
    av.olnID
GO