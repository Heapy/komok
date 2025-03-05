# Kotlin HTMX Integration for Ktor

A Kotlin library that provides seamless integration between [Ktor](https://ktor.io/) and [HTMX](https://htmx.org/), enabling hypermedia-driven applications with minimal JavaScript.

## Overview

This library offers a comprehensive set of Kotlin extensions for working with HTMX in Ktor applications. It provides strongly-typed attributes, utility functions, and convenience builders to simplify HTMX integration while maintaining type safety.

## Features

- Complete set of HTMX attributes as Kotlin constants
- Extension functions for kotlinx.html to easily add HTMX attributes
- Type-safe builders for complex HTMX attributes like `hx-swap` and `hx-trigger`
- Ktor request/response extensions for HTMX headers
- Helper functions for HTML templating with HTMX
- Easy script inclusion for HTMX

## Installation

Add the library to your project's dependencies:

```kotlin
implementation("io.heapy.komok:komok-tech-ktor-htmx:1.0.12")
```

## Usage

### Including HTMX script

```kotlin
import io.heapy.komok.tech.ktor.htmx.htmxScriptLatest
import kotlinx.html.*

fun HTML.page() {
    head {
        title("HTMX Example")
        htmxScriptLatest() // Adds the latest HTMX script to your page
    }
    body {
        // Page content
    }
}
```

### Basic Attributes

```kotlin
import io.heapy.komok.tech.ktor.htmx.*
import kotlinx.html.*

fun BODY.userForm() {
    div {
        form {
            input(type = InputType.text, name = "username") {
                hxPost("/users") // Add hx-post attribute
                hxTarget("#user-list") // Target the response
                hxSwap(HtmxAttributes.SwapValues.OUTER_HTML) // Set swap method
            }
            button {
                +"Add User"
            }
        }
        div {
            id = "user-list"
            // User list will be loaded here
        }
    }
}
```

### Using Type-Safe Builders

```kotlin
import io.heapy.komok.tech.ktor.htmx.*
import kotlinx.html.*

fun BODY.searchForm() {
    input(type = InputType.search, name = "query") {
        hxGet("/search")
        hxTarget("#results")

        // Using the TriggerBuilder for complex triggers
        hxTrigger {
            on("keyup")
            delay("keyup", "500ms")
            changed("keyup")
        }

        // Using the SwapBuilder for complex swaps
        hxSwap {
            value(HtmxAttributes.SwapValues.INNER_HTML)
            scroll("top")
            transition(true)
        }
    }
    div {
        id = "results"
    }
}
```

### Detecting HTMX Requests in Ktor

```kotlin
import io.heapy.komok.tech.ktor.htmx.isHtmxRequest
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/users") {
            if (call.isHtmxRequest) {
                // Return just the user list HTML fragment
                call.respondText(renderUserList(), contentType = ContentType.Text.Html)
            } else {
                // Return the full page
                call.respondText(renderFullPage(), contentType = ContentType.Text.Html)
            }
        }
    }
}
```

### Working with HTMX Headers

```kotlin
import io.heapy.komok.tech.ktor.htmx.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        post("/users") {
            // Process form submission

            // Get the HTMX target from the request
            val target = call.request.getHtmxTarget()

            // Set HTMX-specific response headers
            call.response.setHtmxTrigger(mapOf(
                "userCreated" to """{"id": 123, "name": "New User"}"""
            ))

            // Redirect only for non-HTMX requests
            if (!call.isHtmxRequest) {
                call.response.setHtmxRedirect("/users")
            }

            call.respondText(renderUserList(), contentType = ContentType.Text.Html)
        }
    }
}
```

### HTML Templating

```kotlin
import io.heapy.komok.tech.ktor.htmx.*
import kotlinx.html.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/component") {
            // Render just the body content (for HTMX partial updates)
            val html = renderBodyComponent {
                div {
                    +"This is a partial update"
                    button {
                        hxGet("/another-component")
                        hxSwap(HtmxAttributes.SwapValues.OUTER_HTML)
                        +"Load Another"
                    }
                }
            }
            call.respondText(html, contentType = ContentType.Text.Html)
        }

        get("/page") {
            // Render a complete HTML page
            val html = renderHtmlPage {
                head {
                    title("Complete Page")
                    htmxScriptLatest()
                }
                body {
                    h1 {
                        +"Complete Page Example"
                    }
                    div {
                        id = "content"
                        button {
                            hxGet("/component")
                            hxTarget("#content")
                            +"Load Component"
                        }
                    }
                }
            }
            call.respondText(html, contentType = ContentType.Text.Html)
        }
    }
}
```

## Complete Example: Todo Application

Here's a more complete example of a simple Todo application using Ktor and HTMX:

```kotlin
import io.heapy.komok.tech.ktor.htmx.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.html.*

data class Todo(val id: Int, var text: String, var completed: Boolean = false)

// In-memory todo storage
val todos = mutableListOf(
    Todo(1, "Learn HTMX", true),
    Todo(2, "Build Ktor application", false)
)

fun main() {
    embeddedServer(Netty, port = 8080) {
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
                                hxOn("htmx:afterRequest", "this.reset()")

                                input(type = InputType.text, name = "todoText") {
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
                    ContentType.Text.Html
                )
            }

            // Create a new todo
            post("/todos") {
                val formParameters = call.receiveParameters()
                val todoText = formParameters["todoText"] ?: ""

                if (todoText.isNotBlank()) {
                    val newId = (todos.maxOfOrNull { it.id } ?: 0) + 1
                    val newTodo = Todo(newId, todoText)
                    todos.add(newTodo)

                    call.respondText(
                        renderBodyComponent {
                            renderTodoItem(newTodo)
                        },
                        ContentType.Text.Html
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
                            renderTodoItem(todo)
                        },
                        ContentType.Text.Html
                    )
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            // Delete a todo
            delete("/todos/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                val removed = id?.let { todoId -> todos.removeIf { it.id == todoId } } ?: false

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
fun BODY.renderTodoItem(todo: Todo) {
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
```

## Core HTMX Attributes

The library provides constants for all HTMX attributes, organized into logical groups:

```kotlin
// Core attributes
HtmxAttributes.HX_GET
HtmxAttributes.HX_POST
HtmxAttributes.HX_PUT
HtmxAttributes.HX_PATCH
HtmxAttributes.HX_DELETE
HtmxAttributes.HX_TRIGGER
HtmxAttributes.HX_TARGET
HtmxAttributes.HX_SWAP

// Swap values
HtmxAttributes.SwapValues.INNER_HTML
HtmxAttributes.SwapValues.OUTER_HTML
HtmxAttributes.SwapValues.BEFORE_BEGIN
HtmxAttributes.SwapValues.AFTER_BEGIN
HtmxAttributes.SwapValues.BEFORE_END
HtmxAttributes.SwapValues.AFTER_END

// Trigger modifiers
HtmxAttributes.TriggerModifiers.DELAY
HtmxAttributes.TriggerModifiers.THROTTLE
HtmxAttributes.TriggerModifiers.CHANGED
HtmxAttributes.TriggerModifiers.ONCE

// Request headers
HtmxAttributes.RequestHeaders.HX_REQUEST
HtmxAttributes.RequestHeaders.HX_TRIGGER
HtmxAttributes.RequestHeaders.HX_TARGET

// Response headers
HtmxAttributes.ResponseHeaders.HX_TRIGGER
HtmxAttributes.ResponseHeaders.HX_REDIRECT
HtmxAttributes.ResponseHeaders.HX_REFRESH
```

## Best Practices

1. **Use the builders for complex attributes**: The `SwapBuilder` and `TriggerBuilder` classes provide type-safe ways to construct complex HTMX attributes.

2. **Check for HTMX requests**: Use the `isHtmxRequest` extension property to determine if a request is coming from HTMX, allowing you to return only the necessary HTML fragments.

3. **Leverage the response headers**: Use response headers like `HX-Trigger` to trigger client-side events after a request is complete.

4. **Component-based approach**: Use the templating functions to create reusable components that can be rendered either as part of a full page or as standalone fragments.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 – see the [LICENSE-Apache2](../LICENSE-Apache2) file for details.
