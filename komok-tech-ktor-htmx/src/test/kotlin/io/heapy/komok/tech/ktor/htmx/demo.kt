package io.heapy.komok.tech.ktor.htmx

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.html.ButtonType
import kotlinx.html.InputType
import kotlinx.html.UL
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.li
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.ul

data class Todo(
    val id: Int,
    var text: String,
    var completed: Boolean = false,
)

// In-memory storage
val todos = mutableListOf(
    Todo(
        id = 1,
        text = "Learn HTMX",
        completed = true,
    ),
    Todo(
        id = 2,
        text = "Build Ktor application",
        completed = false,
    ),
)

fun main() {
    embeddedServer(
        CIO,
        port = 8080,
    ) {
        routing {
            get("/") {
                call.respondText(
                    renderHtmlPage {
                        head {
                            title("Todo App")
                            htmxScriptLatest()
                        }
                        body {
                            h1 { +"Todo List" }

                            // Form to add new todos
                            form {
                                id = "new-todo-form"
                                hxPost("/todos")
                                hxTarget("#todo-list")
                                hxSwap(HtmxAttributes.SwapValues.BEFORE_END)
                                hxOn(
                                    "after-request",
                                    "this.reset()",
                                )

                                input(
                                    type = InputType.text,
                                    name = "todoText",
                                ) {
                                    placeholder = "Add a new todo"
                                    required = true
                                }

                                button(type = ButtonType.submit) {
                                    +"Add"
                                }
                            }

                            // Todo list container
                            ul {
                                id = "todo-list"
                                todos.forEach { todo ->
                                    renderTodoItem(todo)
                                }
                            }
                        }
                    },
                    ContentType.Text.Html,
                )
            }

            // Create a new todo
            post("/todos") {
                val formParameters = call.receiveParameters()
                val todoText = formParameters["todoText"]
                    ?: ""

                if (todoText.isNotBlank()) {
                    val newId = (todos.maxOfOrNull { it.id } ?: 0) + 1
                    val newTodo = Todo(
                        newId,
                        todoText,
                    )
                    todos.add(newTodo)

                    call.respondText(
                        renderBodyComponent {
                            ul {
                                doNotRenderCurrentTag()
                                renderTodoItem(newTodo)
                            }
                        },
                        ContentType.Text.Html,
                    )
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            // Toggle todo completion status
            post("/todos/{id}/toggle") {
                val id = call.parameters["id"]?.toIntOrNull()
                val todo = id?.let { todoId -> todos.find { it.id == todoId } }

                if (todo != null) {
                    todo.completed = !todo.completed

                    call.respondText(
                        renderBodyComponent {
                            ul {
                                doNotRenderCurrentTag()
                                renderTodoItem(todo)
                            }
                        },
                        ContentType.Text.Html,
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Delete a todo
            delete("/todos/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val removed = id?.let { todoId -> todos.removeIf { it.id == todoId } }
                    ?: false

                if (removed) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }.start(wait = true)
}

// Helper function to render a todo item
fun UL.renderTodoItem(todo: Todo) {
    li {
        id = "todo-${todo.id}"

        // Checkbox to toggle completion
        input(type = InputType.checkBox) {
            checked = todo.completed
            hxPost("/todos/${todo.id}/toggle")
            hxTarget("#todo-${todo.id}")
            hxSwap(HtmxAttributes.SwapValues.OUTER_HTML)
        }

        // Todo text with strikethrough if completed
        span {
            if (todo.completed) {
                style = "text-decoration: line-through"
            }
            +todo.text
        }

        // Delete button
        button {
            hxDelete("/todos/${todo.id}")
            hxTarget("#todo-${todo.id}")
            hxSwap(HtmxAttributes.SwapValues.OUTER_HTML)
            +"Delete"
        }
    }
}
