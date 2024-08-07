/*
 * This file is generated by jOOQ.
 */
package io.heapy.komok.database.sequences


import io.heapy.komok.database.Public

import org.jooq.Sequence
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType



/**
 * The sequence <code>public.entity_id_seq</code>
 */
val ENTITY_ID_SEQ: Sequence<Long> = Internal.createSequence("entity_id_seq", Public.PUBLIC, SQLDataType.BIGINT.nullable(false), null, null, null, null, false, null)

/**
 * The sequence <code>public.entity_id_seq1</code>
 */
val ENTITY_ID_SEQ1: Sequence<Long> = Internal.createSequence("entity_id_seq1", Public.PUBLIC, SQLDataType.BIGINT.nullable(false), null, null, null, null, false, null)
