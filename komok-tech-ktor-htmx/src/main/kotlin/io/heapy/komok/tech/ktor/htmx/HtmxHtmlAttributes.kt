package io.heapy.komok.tech.ktor.htmx

/**
 * HTMX Attribute Constants for Kotlin
 */
object HtmxAttributes {
    // Core Attributes

    /**
     * Issues a GET request to the specified URL.
     * Example: hx-get="/api/users"
     */
    const val HX_GET = "hx-get"

    /**
     * Issues a POST request to the specified URL.
     * Example: hx-post="/api/users"
     */
    const val HX_POST = "hx-post"

    /**
     * Issues a PUT request to the specified URL.
     * Example: hx-put="/api/users/1"
     */
    const val HX_PUT = "hx-put"

    /**
     * Issues a PATCH request to the specified URL.
     * Example: hx-patch="/api/users/1"
     */
    const val HX_PATCH = "hx-patch"

    /**
     * Issues a DELETE request to the specified URL.
     * Example: hx-delete="/api/users/1"
     */
    const val HX_DELETE = "hx-delete"

    /**
     * Specifies the trigger event for the element. Default trigger is 'click' for most elements and 'change' for form elements.
     * Example: hx-trigger="click, keyup delay:500ms"
     */
    const val HX_TRIGGER = "hx-trigger"

    /**
     * Specifies which element to swap the response into. Default is 'this'.
     * Example: hx-target="#result"
     */
    const val HX_TARGET = "hx-target"

    /**
     * Specifies how the response will be swapped into the DOM. Default is 'innerHTML'.
     * Example: hx-swap="outerHTML"
     */
    const val HX_SWAP = "hx-swap"

    /**
     * Includes additional data in the request.
     * Example: hx-vals='{"name":"Joe", "email":"joe@example.com"}'
     */
    const val HX_VALS = "hx-vals"

    /**
     * Specifies which part of the response to use. Default is to use the whole response.
     * Example: hx-select="#content"
     */
    const val HX_SELECT = "hx-select"

    /**
     * Allows you to filter on a specific part of the element for the trigger.
     * Example: hx-select-oob="#notification"
     */
    const val HX_SELECT_OOB = "hx-select-oob"

    /**
     * Pushes the URL into the browser location bar, creating a new history entry.
     * Example: hx-push-url="true"
     */
    const val HX_PUSH_URL = "hx-push-url"

    /**
     * Prevents the element from executing any AJAX operations.
     * Example: hx-boost="false"
     */
    const val HX_BOOST = "hx-boost"

    /**
     * Aborts all in-flight AJAX requests issued by the element.
     * Example: hx-abort="closest form"
     */
    const val HX_ABORT = "hx-abort"

    /**
     * Adds parameters to the request's GET parameters or request body.
     * Example: hx-params="*"
     */
    const val HX_PARAMS = "hx-params"

    /**
     * Specifies to include the closest form to the element in the request.
     * Example: hx-include=".form-data"
     */
    const val HX_INCLUDE = "hx-include"

    /**
     * Indicates elements that should be updated in the UI when an element is clicked.
     * Example: hx-indicator=".spinner"
     */
    const val HX_INDICATOR = "hx-indicator"

    /**
     * Handles history restoration for the element.
     * Example: hx-history="false"
     */
    const val HX_HISTORY = "hx-history"

    /**
     * Pauses a request from being issued. Elements that have this attribute will not automatically issue an AJAX request.
     * Example: hx-request="manual"
     */
    const val HX_REQUEST = "hx-request"

    /**
     * Contains JavaScript code that is executed when the response is received.
     * Example: hx-on:htmx:after-request="console.log('Request complete')"
     */
    const val HX_ON = "hx-on"

    /**
     * Specifies the element to put the htmx-request class on during the request.
     * Example: hx-sync="#form:abort"
     */
    const val HX_SYNC = "hx-sync"

    /**
     * Disables htmx processing for an element and all of its children.
     * Example: hx-disable="true"
     */
    const val HX_DISABLE = "hx-disable"

    /**
     * Enables htmx processing for an element and all of its children.
     * Example: hx-enable="true"
     */
    const val HX_ENABLE = "hx-enable"

    /**
     * Specifies a URL or path to a JavaScript file containing extensions.
     * Example: hx-ext="debug"
     */
    const val HX_EXT = "hx-ext"

    /**
     * Adds headers to the request.
     * Example: hx-headers='{"X-Requested-With": "XMLHttpRequest"}'
     */
    const val HX_HEADERS = "hx-headers"

    /**
     * Controls if the element displays a confirmation dialog before issuing a request.
     * Example: hx-confirm="Are you sure you want to delete this item?"
     */
    const val HX_CONFIRM = "hx-confirm"

    /**
     * Makes the request directly to the server, bypassing the event handling.
     * Example: hx-disinherit="*"
     */
    const val HX_DISINHERIT = "hx-disinherit"

    /**
     * Defines the action to take if an error occurs when executing a request.
     * Example: hx-on:error="alert('Error!')"
     */
    const val HX_ON_ERROR = "hx-on:error"

    /**
     * Defines the action to take when the request is complete.
     * Example: hx-on:complete="console.log('Request complete')"
     */
    const val HX_ON_COMPLETE = "hx-on:complete"

    /**
     * Adds additional data to the request.
     * Example: hx-vars='myVar: calculateValue()'
     */
    const val HX_VARS = "hx-vars"

    /**
     * Preserves the element in place during a page transition.
     * Example: hx-preserve="true"
     */
    const val HX_PRESERVE = "hx-preserve"

    // Swap Values

    /**
     * Swap values for hx-swap attribute
     */
    object SwapValues {
        /**
         * Default. Replace the inner html of the target element.
         */
        const val INNER_HTML = "innerHTML"

        /**
         * Replace the entire target element with the response.
         */
        const val OUTER_HTML = "outerHTML"

        /**
         * Insert the response before the target element.
         */
        const val BEFORE_BEGIN = "beforebegin"

        /**
         * Insert the response before the first child of the target element.
         */
        const val AFTER_BEGIN = "afterbegin"

        /**
         * Insert the response after the last child of the target element.
         */
        const val BEFORE_END = "beforeend"

        /**
         * Insert the response after the target element.
         */
        const val AFTER_END = "afterend"

        /**
         * Deletes the target element regardless of the response.
         */
        const val DELETE = "delete"

        /**
         * Does not append content from response (out of band items will still be processed).
         */
        const val NONE = "none"
    }

    // Swap Modifiers

    /**
     * Swap modifiers for hx-swap attribute
     */
    object SwapModifiers {
        /**
         * A CSS selector that updates the browser's scroll position to the element after content is swapped.
         * Example: scroll:top
         */
        const val SCROLL = "scroll"

        /**
         * Time in milliseconds to wait before performing the swap.
         * Example: swap:100ms
         */
        const val SWAP = "swap"

        /**
         * Controls if the scroll position is reset after a swap.
         * Example: scroll:window:top
         */
        const val SHOW = "show"

        /**
         * Forces any load events on the target to be processed immediately rather than waiting for the settle delay.
         * Example: settle:100ms
         */
        const val SETTLE = "settle"

        /**
         * Time to wait between receiving a response and triggering the transition.
         * Example: transition:true
         */
        const val TRANSITION = "transition"
    }

    // Trigger Modifiers

    /**
     * Trigger modifiers for hx-trigger attribute
     */
    object TriggerModifiers {
        /**
         * Wait for the specified event before issuing the request.
         * Example: focus delay:500ms
         */
        const val DELAY = "delay"

        /**
         * Throttle the request to every given number of milliseconds.
         * Example: keyup throttle:500ms
         */
        const val THROTTLE = "throttle"

        /**
         * Wait for a "changed" value before issuing the request.
         * Example: keyup changed
         */
        const val CHANGED = "changed"

        /**
         * Enable polling of the URL.
         * Example: every 1s
         */
        const val EVERY = "every"

        /**
         * Trigger after the element is loaded.
         * Example: load
         */
        const val LOAD = "load"

        /**
         * Trigger when the element is revealed via scrolling.
         * Example: revealed
         */
        const val REVEALED = "revealed"

        /**
         * Only issue the request once.
         * Example: click once
         */
        const val ONCE = "once"

        /**
         * Filter the trigger event through a CSS selector.
         * Example: click from:#parent
         */
        const val FROM = "from"

        /**
         * Target the response at the CSS selector.
         * Example: click target:#result
         */
        const val TARGET = "target"

        /**
         * Route events through different elements.
         * Example: click consume
         */
        const val CONSUME = "consume"

        /**
         * Queue requests.
         * Example: click queue:first
         */
        const val QUEUE = "queue"
    }

    // Extended Attributes (document.body)

    /**
     * Configuration attribute for the document body
     */
    object BodyAttributes {
        /**
         * Allows you to change the global timeout for htmx requests.
         * Example: hx-config='{"timeout":10000}'
         */
        const val HX_CONFIG = "hx-config"

        /**
         * Specifies a URL to redirect to when htmx encounters an error during a request.
         * Example: hx-on:error-url="/error"
         */
        const val HX_ON_ERROR_URL = "hx-on:error-url"

        /**
         * Specifies the default time in milliseconds that htmx will wait for a response before automatically canceling the request.
         * Example: hx-on:timeout="1000"
         */
        const val HX_ON_TIMEOUT = "hx-on:timeout"
    }

    // CSS Classes

    /**
     * CSS Classes used by htmx
     */
    object CssClasses {
        /**
         * Applied to an element that is currently making an AJAX request.
         */
        const val HX_REQUEST = "htmx-request"

        /**
         * Applied to an element for the duration of a request.
         */
        const val HX_REQUESTING = "htmx-requesting"

        /**
         * Applied to an element if a request has failed.
         */
        const val HX_ERROR = "htmx-error"

        /**
         * Applied to an element that has an indicator element.
         */
        const val HX_INDICATOR = "htmx-indicator"

        /**
         * Applied to indicators while a request is in flight.
         */
        const val HX_SHOW = "htmx-show"

        /**
         * Applied to elements that will be swapped.
         */
        const val HX_SWAPPING = "htmx-swapping"

        /**
         * Applied to elements that have been swapped.
         */
        const val HX_SWAPPED = "htmx-swapped"

        /**
         * Applied to elements that have been settled.
         */
        const val HX_SETTLING = "htmx-settling"

        /**
         * Applied to elements that have had their requests boosted.
         */
        const val HX_BOOSTED = "htmx-boosted"

        /**
         * Applied to elements that have been preserved.
         */
        const val HX_PRESERVED = "htmx-preserved"

        /**
         * Applied to the document while a request is in flight to support showing progress animations.
         */
        const val HX_ACTIVE = "htmx-active"
    }

    // Response Headers

    /**
     * Response Headers used by htmx
     */
    object ResponseHeaders {
        /**
         * Allows you to specify the target to swap the response into as a CSS selector.
         */
        const val HX_RETARGET = "HX-Retarget"

        /**
         * Allows you to specify how the response will be swapped in relative to the target.
         */
        const val HX_RESWAP = "HX-Reswap"

        /**
         * Pushes a new URL into the browser's address bar.
         */
        const val HX_PUSH_URL = "HX-Push-Url"

        /**
         * Allows htmx to control browser history navigation.
         */
        const val HX_PUSH = "HX-Push"

        /**
         * Suppresses any history element creation.
         */
        const val HX_HISTORY_NO = "HX-History-No"

        /**
         * Allows you to select the content you want to swap in from a response.
         */
        const val HX_RESELECT = "HX-Reselect"

        /**
         * Causes a hard refresh of the page.
         */
        const val HX_REFRESH = "HX-Refresh"

        /**
         * Redirects to a new location.
         */
        const val HX_REDIRECT = "HX-Redirect"

        /**
         * Forces htmx-related scripts to not execute on the response.
         */
        const val HX_NO_SCRIPTS = "HX-No-Scripts"

        /**
         * Triggers client-side events.
         */
        const val HX_TRIGGER = "HX-Trigger"

        /**
         * Triggers client-side events after the settlement phase.
         */
        const val HX_TRIGGER_AFTER_SETTLE = "HX-Trigger-After-Settle"

        /**
         * Triggers client-side events after the swap phase.
         */
        const val HX_TRIGGER_AFTER_SWAP = "HX-Trigger-After-Swap"

        /**
         * Allows you to replace the current URL in the browser location bar.
         */
        const val HX_REPLACE_URL = "HX-Replace-Url"

        /**
         * Allows you to specify a location to redirect to.
         */
        const val LOCATION = "Location"
    }

    // Request Headers

    /**
     * Request Headers used by htmx
     */
    object RequestHeaders {
        /**
         * Indicates that the request is via htmx.
         */
        const val HX_REQUEST = "HX-Request"

        /**
         * The ID of the target element.
         */
        const val HX_TARGET = "HX-Target"

        /**
         * The name of the triggered element.
         */
        const val HX_TRIGGER_NAME = "HX-Trigger-Name"

        /**
         * The ID of the triggered element.
         */
        const val HX_TRIGGER = "HX-Trigger"

        /**
         * The current URL of the browser.
         */
        const val HX_CURRENT_URL = "HX-Current-URL"

        /**
         * The prompt response if it was triggered by an hx-confirm.
         */
        const val HX_PROMPT = "HX-Prompt"

        /**
         * The name of the event that triggered this request.
         */
        const val HX_EVENT_TARGET = "HX-Event-Target"
    }

    // Event Types

    /**
     * Events that htmx triggers
     */
    object Events {
        /**
         * Triggered before an AJAX request is made.
         */
        const val BEFORE_REQUEST = "htmx:beforeRequest"

        /**
         * Triggered before the request is sent.
         */
        const val BEFORE_SEND = "htmx:beforeSend"

        /**
         * Triggered before the content is swapped.
         */
        const val BEFORE_SWAP = "htmx:beforeSwap"

        /**
         * Triggered after the content is swapped.
         */
        const val AFTER_SWAP = "htmx:afterSwap"

        /**
         * Triggered after the content is completely settled.
         */
        const val AFTER_SETTLE = "htmx:afterSettle"

        /**
         * Triggered when an error occurs.
         */
        const val ON_ERROR = "htmx:error"

        /**
         * Triggered when a link for history navigation is clicked.
         */
        const val BEFORE_HISTORY_SAVE = "htmx:beforeHistorySave"

        /**
         * Triggered when htmx handles a history restoration action.
         */
        const val HISTORY_RESTORE = "htmx:historyRestore"

        /**
         * Triggered when a request is aborted.
         */
        const val ABORT = "htmx:abort"

        /**
         * Triggered when a request has been completed.
         */
        const val AFTER_REQUEST = "htmx:afterRequest"

        /**
         * Triggered when an element is shown due to a request.
         */
        const val SHOW = "htmx:show"

        /**
         * Triggered when an element with a load event is added to the DOM.
         */
        const val LOAD = "htmx:load"

        /**
         * Triggered on the window after configuration has been loaded.
         */
        const val CONFIG_LOAD = "htmx:configLoad"

        /**
         * Triggered when an element is about to be processed by htmx.
         */
        const val BEFORE_PROCESS = "htmx:beforeProcess"

        /**
         * Triggered when an ongoing request is timed out.
         */
        const val TIMEOUT = "htmx:timeout"
    }
}
