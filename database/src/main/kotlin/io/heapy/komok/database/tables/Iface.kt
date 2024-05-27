/*
 * This file is generated by jOOQ.
 */
package io.heapy.komok.database.tables


import io.heapy.komok.database.Public
import io.heapy.komok.database.indexes.IDX_INTERFACE_ID
import io.heapy.komok.database.keys.IFACE_PKEY
import io.heapy.komok.database.tables.records.IfaceRecord

import kotlin.collections.Collection
import kotlin.collections.List

import org.jooq.Condition
import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Identity
import org.jooq.Index
import org.jooq.InverseForeignKey
import org.jooq.JSONB
import org.jooq.Name
import org.jooq.PlainSQL
import org.jooq.QueryPart
import org.jooq.Record
import org.jooq.SQL
import org.jooq.Schema
import org.jooq.Select
import org.jooq.Stringly
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class Iface(
    alias: Name,
    path: Table<out Record>?,
    childPath: ForeignKey<out Record, IfaceRecord>?,
    parentPath: InverseForeignKey<out Record, IfaceRecord>?,
    aliased: Table<IfaceRecord>?,
    parameters: Array<Field<*>?>?,
    where: Condition?
): TableImpl<IfaceRecord>(
    alias,
    Public.PUBLIC,
    path,
    childPath,
    parentPath,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table(),
    where,
) {
    companion object {

        /**
         * The reference instance of <code>public.iface</code>
         */
        val IFACE: Iface = Iface()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<IfaceRecord> = IfaceRecord::class.java

    /**
     * The column <code>public.iface.id</code>.
     */
    val ID: TableField<IfaceRecord, Long?> = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false).identity(true), this, "")

    /**
     * The column <code>public.iface.name</code>.
     */
    val NAME: TableField<IfaceRecord, String?> = createField(DSL.name("name"), SQLDataType.VARCHAR(100).nullable(false), this, "")

    /**
     * The column <code>public.iface.schema</code>.
     */
    val SCHEMA: TableField<IfaceRecord, JSONB?> = createField(DSL.name("schema"), SQLDataType.JSONB, this, "")

    private constructor(alias: Name, aliased: Table<IfaceRecord>?): this(alias, null, null, null, aliased, null, null)
    private constructor(alias: Name, aliased: Table<IfaceRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, null, aliased, parameters, null)
    private constructor(alias: Name, aliased: Table<IfaceRecord>?, where: Condition?): this(alias, null, null, null, aliased, null, where)

    /**
     * Create an aliased <code>public.iface</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>public.iface</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>public.iface</code> table reference
     */
    constructor(): this(DSL.name("iface"), null)
    override fun getSchema(): Schema? = if (aliased()) null else Public.PUBLIC
    override fun getIndexes(): List<Index> = listOf(IDX_INTERFACE_ID)
    override fun getIdentity(): Identity<IfaceRecord, Long?> = super.getIdentity() as Identity<IfaceRecord, Long?>
    override fun getPrimaryKey(): UniqueKey<IfaceRecord> = IFACE_PKEY
    override fun `as`(alias: String): Iface = Iface(DSL.name(alias), this)
    override fun `as`(alias: Name): Iface = Iface(alias, this)
    override fun `as`(alias: Table<*>): Iface = Iface(alias.qualifiedName, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): Iface = Iface(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): Iface = Iface(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): Iface = Iface(name.qualifiedName, null)

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Condition?): Iface = Iface(qualifiedName, if (aliased()) this else null, condition)

    /**
     * Create an inline derived table from this table
     */
    override fun where(conditions: Collection<Condition>): Iface = where(DSL.and(conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(vararg conditions: Condition?): Iface = where(DSL.and(*conditions))

    /**
     * Create an inline derived table from this table
     */
    override fun where(condition: Field<Boolean?>?): Iface = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(condition: SQL): Iface = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String): Iface = where(DSL.condition(condition))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg binds: Any?): Iface = where(DSL.condition(condition, *binds))

    /**
     * Create an inline derived table from this table
     */
    @PlainSQL override fun where(@Stringly.SQL condition: String, vararg parts: QueryPart): Iface = where(DSL.condition(condition, *parts))

    /**
     * Create an inline derived table from this table
     */
    override fun whereExists(select: Select<*>): Iface = where(DSL.exists(select))

    /**
     * Create an inline derived table from this table
     */
    override fun whereNotExists(select: Select<*>): Iface = where(DSL.notExists(select))
}