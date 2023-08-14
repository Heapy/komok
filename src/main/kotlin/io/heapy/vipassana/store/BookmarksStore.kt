package io.heapy.vipassana.store

import java.time.ZonedDateTime

@JvmInline
value class BookmarkId(val value: String)
@JvmInline
value class ChatId(val value: String)

/**
 * Ask to create full-text index for field
 * Looks like we can do this by default for all String fields,
 * and maybe just negate annotation (DisableSearch) when search is not required
 */
annotation class Searchable

class Attachment

// Split into entity and entity wrapper,
// most of functionality is actually shared between all types
sealed interface Bookmark {
    // entity put to event stream to populate extra fields or review, processed
    // flag reset after internal change (for example status change)
    val processed: Boolean
    val id: BookmarkId
    val user: UserId
    val sharedWith: List<GroupId>
    @Searchable
    val url: String
    @Searchable
    val title: String
    @Searchable
    val description: String
    @Searchable
    val content: String // for better search, and for archive
    @Searchable
    val created: ZonedDateTime
    val favourite: Boolean
    @Searchable
    val tags: List<String> // for better search
    val favicon: FileId
    val attachments: List<Attachment> // anything in system
    val chat: ChatId // multi-user tree chat for bookmark
    val parent: BookmarkId? // for bookmarks tree
    val status: String? // 404, 301, 302, etc
    val lastStatusCheck: ZonedDateTime
}

interface BookmarksStore {
    suspend fun save()
}

data class Contact(
    var id: String,
    var name: String,
    var favourite: Boolean,
)

/**
 * Executes all ops or none
 */
data class AtomicOp(
    val ops: List<ContactOp>
)

/**
 * Just number of ops on single item which will be processed together
 */
data class BatchOp(
    val ops: List<ContactOp>
)

sealed interface ContactOp {
    val id: String
    val clock: Long
    val node: String
}

data class CreatContactOp(
    override val id: String,
    val name: String,
    val favourite: Boolean,
    override val clock: Long,
    override val node: String,
) : ContactOp

data class ChangeContactNameOp(
    override val id: String,
    val name: String,
    override val clock: Long,
    override val node: String,
) : ContactOp

data class ChangeFavouriteOp(
    override val id: String,
    val favourite: Boolean,
    override val clock: Long,
    override val node: String,
) : ContactOp

// db tables:
// - contact_ops
// - contact_snapshot

fun snapshot(op: ContactOp, ops: List<ContactOp>): Contact {
    val currentContactsOps = ops.filter { it.id == op.id }
        .sortedBy { it.clock }

    val create = currentContactsOps.first()
    if (create !is CreatContactOp) error("Invalid state")

    val contact = Contact(
        id = create.id,
        name = create.name,
        favourite = create.favourite,
    )

    currentContactsOps.forEach { opCurrent ->
        when (opCurrent) {
            is CreatContactOp -> Unit
            is ChangeFavouriteOp -> contact.favourite = opCurrent.favourite
            is ChangeContactNameOp -> contact.name = opCurrent.name
        }
    }

    return contact
}

fun main() {
    val contact_ops = mutableListOf<ContactOp>()
    val contact_snapshots = mutableMapOf<String, Contact>()

    val op1 = CreatContactOp(
        id = "1",
        name = "Ruslan Ibragimov",
        favourite = false,
        clock = 1,
        node = "A",
    )

    contact_ops.add(op1)

    val contact1 = snapshot(op1, contact_ops)
    contact_snapshots[op1.id] = contact1
    println(contact_snapshots[op1.id]) // Contact(id=1, name=Ruslan Ibragimov, favourite=false)

    val op2 = ChangeContactNameOp(
        id = "1",
        name = "Ruslan Ibrahimau",
        clock = 100,
        node = "A",
    )

    contact_ops.add(op2)

    val contact2 = snapshot(op2, contact_ops)
    contact_snapshots[op1.id] = contact2
    println(contact_snapshots[op1.id]) // Contact(id=1, name=Ruslan Ibrahimau, favourite=false)

    val op3 = ChangeFavouriteOp(
        id = "1",
        favourite = true,
        clock = 200,
        node = "A",
    )

    contact_ops.add(op3)

    val contact3 = snapshot(op3, contact_ops)
    contact_snapshots[op1.id] = contact3
    println(contact_snapshots[op1.id]) // Contact(id=1, name=Ruslan Ibrahimau, favourite=true)
}
