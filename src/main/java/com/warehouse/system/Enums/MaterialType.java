package com.warehouse.system.Enums;

public enum MaterialType {
    // General & Ambient Goods
    GENERAL_DRY_GOODS,
    RAW_MATERIALS,
    FINISHED_GOODS,
    ELECTRONICS_AND_TECH,
    TEXTILES_AND_APPAREL,
    AUTOMOTIVE_PARTS,

    // Temperature-Controlled & Perishables
    PERISHABLE_FOOD,
    FROZEN_GOODS,
    PHARMACEUTICALS_MEDICAL,
    // Hazardous & Regulated (HAZMAT)

    FLAMMABLE_LIQUIDS,
    FLAMMABLE_SOLIDS,
    CORROSIVES,
    TOXIC_CHEMICALS,
    GASES_COMPRESSED,
    HAZARDOUS_MISC,

    // Heavy, Oversized & Bulk Materials
    HEAVY_MACHINERY,
    CONSTRUCTION_MATERIALS,
    BULK_LIQUIDS,
    BULK_DRY,

    // High-Value & Sensitive
    HIGH_VALUE_SECURE,
    FRAGILE_GLASS_CERAMIC,
    DOCUMENTS_ARCHIVES
}
