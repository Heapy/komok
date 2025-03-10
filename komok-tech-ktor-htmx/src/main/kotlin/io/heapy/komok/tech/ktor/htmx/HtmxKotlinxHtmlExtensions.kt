package io.heapy.komok.tech.ktor.htmx

import io.ktor.server.response.*
import io.ktor.server.request.*
import kotlinx.html.*

/**
 * HTMX Extensions for Ktor and kotlinx.html
 */

// Core Extensions for kotlinx.html.TagConsumer

/**
 * Base function for setting HTMX attributes
 */
fun CommonAttributeGroupFacade.htmx(attributeName: String, value: String?) {
    if (value != null) {
        attributes[attributeName] = value
    }
}

/**
 * Issues a GET request to the specified URL
 */
fun CommonAttributeGroupFacade.hxGet(url: String) {
    htmx(HtmxAttributes.HX_GET, url)
}

/**
 * Issues a POST request to the specified URL
 */
fun CommonAttributeGroupFacade.hxPost(url: String) {
    htmx(HtmxAttributes.HX_POST, url)
}

/**
 * Issues a PUT request to the specified URL
 */
fun CommonAttributeGroupFacade.hxPut(url: String) {
    htmx(HtmxAttributes.HX_PUT, url)
}

/**
 * Issues a PATCH request to the specified URL
 */
fun CommonAttributeGroupFacade.hxPatch(url: String) {
    htmx(HtmxAttributes.HX_PATCH, url)
}

/**
 * Issues a DELETE request to the specified URL
 */
fun CommonAttributeGroupFacade.hxDelete(url: String) {
    htmx(HtmxAttributes.HX_DELETE, url)
}

/**
 * Specifies the trigger event for the element
 */
fun CommonAttributeGroupFacade.hxTrigger(triggerSpec: String) {
    htmx(HtmxAttributes.HX_TRIGGER, triggerSpec)
}

/**
 * Specifies which element to swap the response into
 */
fun CommonAttributeGroupFacade.hxTarget(target: String) {
    htmx(HtmxAttributes.HX_TARGET, target)
}

/**
 * Specifies how the response will be swapped into the DOM
 */
fun CommonAttributeGroupFacade.hxSwap(swapStyle: String) {
    htmx(HtmxAttributes.HX_SWAP, swapStyle)
}

/**
 * Specifies how the response will be swapped into the DOM using predefined SwapValues
 */
fun CommonAttributeGroupFacade.hxSwap(swapValue: String, modifiers: Map<String, String>? = null) {
    val value = if (modifiers == null || modifiers.isEmpty()) {
        swapValue
    } else {
        val modifierString = modifiers.entries.joinToString(" ") { (key, value) -> "$key:$value" }
        "$swapValue $modifierString"
    }
    htmx(HtmxAttributes.HX_SWAP, value)
}

/**
 * Includes additional data in the request
 */
fun CommonAttributeGroupFacade.hxVals(json: String) {
    htmx(HtmxAttributes.HX_VALS, json)
}

/**
 * Specifies which part of the response to use
 */
fun CommonAttributeGroupFacade.hxSelect(selector: String) {
    htmx(HtmxAttributes.HX_SELECT, selector)
}

/**
 * Allows you to filter on a specific part of the element for Out-of-Band Swaps
 */
fun CommonAttributeGroupFacade.hxSelectOob(selector: String) {
    htmx(HtmxAttributes.HX_SELECT_OOB, selector)
}

/**
 * Pushes the URL into the browser location bar, creating a new history entry
 */
fun CommonAttributeGroupFacade.hxPushUrl(enabled: Boolean) {
    htmx(HtmxAttributes.HX_PUSH_URL, enabled.toString())
}

/**
 * Pushes the URL into the browser location bar with a specific URL
 */
fun CommonAttributeGroupFacade.hxPushUrl(url: String) {
    htmx(HtmxAttributes.HX_PUSH_URL, url)
}

/**
 * Controls if the element should have AJAX behavior enabled (boosting)
 */
fun CommonAttributeGroupFacade.hxBoost(enabled: Boolean) {
    htmx(HtmxAttributes.HX_BOOST, enabled.toString())
}

/**
 * Aborts any in-flight AJAX requests issued by the element
 */
fun CommonAttributeGroupFacade.hxAbort(selector: String? = null) {
    htmx(HtmxAttributes.HX_ABORT, selector ?: "true")
}

/**
 * Controls form parameters inclusion in the request
 */
fun CommonAttributeGroupFacade.hxParams(value: String) {
    htmx(HtmxAttributes.HX_PARAMS, value)
}

/**
 * Specifies to include additional elements in the request
 */
fun CommonAttributeGroupFacade.hxInclude(selector: String) {
    htmx(HtmxAttributes.HX_INCLUDE, selector)
}

/**
 * Indicates elements that should be updated in the UI when an element is clicked
 */
fun CommonAttributeGroupFacade.hxIndicator(selector: String) {
    htmx(HtmxAttributes.HX_INDICATOR, selector)
}

/**
 * Controls if the element will be included in the browser history
 */
fun CommonAttributeGroupFacade.hxHistory(enabled: Boolean) {
    htmx(HtmxAttributes.HX_HISTORY, enabled.toString())
}

/**
 * Adds a confirmation dialog before issuing a request
 */
fun CommonAttributeGroupFacade.hxConfirm(message: String) {
    htmx(HtmxAttributes.HX_CONFIRM, message)
}

/**
 * Adds headers to the request
 */
fun CommonAttributeGroupFacade.hxHeaders(json: String) {
    htmx(HtmxAttributes.HX_HEADERS, json)
}

/**
 * Disables htmx processing for an element and all of its children
 */
fun CommonAttributeGroupFacade.hxDisable() {
    htmx(HtmxAttributes.HX_DISABLE, "true")
}

/**
 * Enables htmx processing for an element and all of its children
 */
fun CommonAttributeGroupFacade.hxEnable() {
    htmx(HtmxAttributes.HX_ENABLE, "true")
}

/**
 * Specifies extensions to use for this element
 */
fun CommonAttributeGroupFacade.hxExt(extensionList: String) {
    htmx(HtmxAttributes.HX_EXT, extensionList)
}

/**
 * Preserves an element during page transitions
 */
fun CommonAttributeGroupFacade.hxPreserve() {
    htmx(HtmxAttributes.HX_PRESERVE, "true")
}

/**
 * Adds JavaScript event handlers for htmx events
 */
fun CommonAttributeGroupFacade.hxOn(event: String, script: String) {
    htmx("${HtmxAttributes.HX_ON}::$event", script)
}

// Convenience extensions for common trigger options
/**
 * Sets up element to trigger on click
 */
fun CommonAttributeGroupFacade.hxTriggerOnClick(additionalOptions: String? = null) {
    val value = if (additionalOptions != null) "click $additionalOptions" else "click"
    htmx(HtmxAttributes.HX_TRIGGER, value)
}

/**
 * Sets up element to trigger on form submission
 */
fun CommonAttributeGroupFacade.hxTriggerOnSubmit(additionalOptions: String? = null) {
    val value = if (additionalOptions != null) "submit $additionalOptions" else "submit"
    htmx(HtmxAttributes.HX_TRIGGER, value)
}

/**
 * Sets up element to trigger on input change
 */
fun CommonAttributeGroupFacade.hxTriggerOnChange(additionalOptions: String? = null) {
    val value = if (additionalOptions != null) "change $additionalOptions" else "change"
    htmx(HtmxAttributes.HX_TRIGGER, value)
}

/**
 * Sets up element to trigger when element loads
 */
fun CommonAttributeGroupFacade.hxTriggerOnLoad(additionalOptions: String? = null) {
    val value = if (additionalOptions != null) "load $additionalOptions" else "load"
    htmx(HtmxAttributes.HX_TRIGGER, value)
}

/**
 * Sets up element to trigger when element is revealed in viewport
 */
fun CommonAttributeGroupFacade.hxTriggerOnReveal(additionalOptions: String? = null) {
    val value = if (additionalOptions != null) "revealed $additionalOptions" else "revealed"
    htmx(HtmxAttributes.HX_TRIGGER, value)
}

/**
 * Sets up polling on an element
 * @param interval The polling interval, e.g. "1s", "500ms"
 */
fun CommonAttributeGroupFacade.hxTriggerEvery(interval: String, additionalOptions: String? = null) {
    val value = if (additionalOptions != null) "every $interval $additionalOptions" else "every $interval"
    htmx(HtmxAttributes.HX_TRIGGER, value)
}

// Extensions for Ktor Response handling

/**
 * Gets the HTMX target element ID from the request
 */
fun ApplicationRequest.getHtmxTarget(): String? {
    return header(HtmxAttributes.RequestHeaders.HX_TARGET)
}

/**
 * Gets the HTMX trigger element ID from the request
 */
fun ApplicationRequest.getHtmxTrigger(): String? {
    return header(HtmxAttributes.RequestHeaders.HX_TRIGGER)
}

/**
 * Gets the HTMX trigger name from the request
 */
fun ApplicationRequest.getHtmxTriggerName(): String? {
    return header(HtmxAttributes.RequestHeaders.HX_TRIGGER_NAME)
}

/**
 * Sets a header to control the HTMX target for the response
 */
fun ApplicationResponse.setHtmxRetarget(selector: String) {
    header(HtmxAttributes.ResponseHeaders.HX_RETARGET, selector)
}

/**
 * Sets a header to control how HTMX swaps the response
 */
fun ApplicationResponse.setHtmxReswap(swapStyle: String) {
    header(HtmxAttributes.ResponseHeaders.HX_RESWAP, swapStyle)
}

/**
 * Sets a header to trigger browser URL change
 */
fun ApplicationResponse.setHtmxPushUrl(url: String) {
    header(HtmxAttributes.ResponseHeaders.HX_PUSH_URL, url)
}

/**
 * Sets a header to replace the current URL
 */
fun ApplicationResponse.setHtmxReplaceUrl(url: String) {
    header(HtmxAttributes.ResponseHeaders.HX_REPLACE_URL, url)
}

/**
 * Sets a header to trigger client-side events
 */
fun ApplicationResponse.setHtmxTrigger(events: Map<String, String?>) {
    val eventString = events.entries.joinToString(",") { (event, detail) ->
        if (detail == null) {
            "\"$event\""
        } else {
            "\"$event\":$detail"
        }
    }
    header(HtmxAttributes.ResponseHeaders.HX_TRIGGER, eventString)
}

/**
 * Sets a header to trigger client-side events after swap
 */
fun ApplicationResponse.setHtmxTriggerAfterSwap(events: Map<String, String?>) {
    val eventString = events.entries.joinToString(",") { (event, detail) ->
        if (detail == null) {
            "\"$event\""
        } else {
            "\"$event\":$detail"
        }
    }
    header(HtmxAttributes.ResponseHeaders.HX_TRIGGER_AFTER_SWAP, eventString)
}

/**
 * Sets a header to trigger client-side events after settle
 */
fun ApplicationResponse.setHtmxTriggerAfterSettle(events: Map<String, String?>) {
    val eventString = events.entries.joinToString(",") { (event, detail) ->
        if (detail == null) {
            "\"$event\""
        } else {
            "\"$event\":$detail"
        }
    }
    header(HtmxAttributes.ResponseHeaders.HX_TRIGGER_AFTER_SETTLE, eventString)
}

/**
 * Sets a header to refresh the page
 */
fun ApplicationResponse.setHtmxRefresh() {
    header(HtmxAttributes.ResponseHeaders.HX_REFRESH, "true")
}

/**
 * Sets a header to redirect the page
 */
fun ApplicationResponse.setHtmxRedirect(url: String) {
    header(HtmxAttributes.ResponseHeaders.HX_REDIRECT, url)
}

/**
 * Builder to create a strongly-typed swap value with modifiers
 */
class SwapBuilder {
    private var swapValue: String = HtmxAttributes.SwapValues.INNER_HTML
    private val modifiers = mutableMapOf<String, String>()

    fun value(value: String): SwapBuilder {
        swapValue = value
        return this
    }

    fun scroll(target: String? = null): SwapBuilder {
        val modValue = target ?: "true"
        modifiers[HtmxAttributes.SwapModifiers.SCROLL] = modValue
        return this
    }

    fun scrollTop(): SwapBuilder {
        modifiers[HtmxAttributes.SwapModifiers.SCROLL] = "top"
        return this
    }

    fun scrollBottom(): SwapBuilder {
        modifiers[HtmxAttributes.SwapModifiers.SCROLL] = "bottom"
        return this
    }

    fun swap(time: String): SwapBuilder {
        modifiers[HtmxAttributes.SwapModifiers.SWAP] = time
        return this
    }

    fun settle(time: String): SwapBuilder {
        modifiers[HtmxAttributes.SwapModifiers.SETTLE] = time
        return this
    }

    fun transition(enabled: Boolean = true): SwapBuilder {
        modifiers[HtmxAttributes.SwapModifiers.TRANSITION] = enabled.toString()
        return this
    }

    fun show(target: String): SwapBuilder {
        modifiers[HtmxAttributes.SwapModifiers.SHOW] = target
        return this
    }

    fun build(): String {
        if (modifiers.isEmpty()) return swapValue

        val modString = modifiers.entries.joinToString(" ") { (key, value) ->
            "$key:$value"
        }
        return "$swapValue $modString"
    }
}

/**
 * Builder to create a strongly-typed trigger value with modifiers
 */
class TriggerBuilder {
    private val triggers = mutableListOf<String>()
    private val modifiers = mutableMapOf<String, MutableMap<String, String>>()

    fun on(event: String): TriggerBuilder {
        triggers.add(event)
        return this
    }

    fun delay(event: String, time: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.DELAY] = time
        return this
    }

    fun throttle(event: String, time: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.THROTTLE] = time
        return this
    }

    fun changed(event: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.CHANGED] = "true"
        return this
    }

    fun once(event: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.ONCE] = "true"
        return this
    }

    fun from(event: String, selector: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.FROM] = selector
        return this
    }

    fun target(event: String, selector: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.TARGET] = selector
        return this
    }

    fun queue(event: String, strategy: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.QUEUE] = strategy
        return this
    }

    fun consume(event: String): TriggerBuilder {
        modifiers.getOrPut(event) { mutableMapOf() }[HtmxAttributes.TriggerModifiers.CONSUME] = "true"
        return this
    }

    fun poll(interval: String): TriggerBuilder {
        triggers.add("every $interval")
        return this
    }

    fun load(): TriggerBuilder {
        triggers.add("load")
        return this
    }

    fun revealed(): TriggerBuilder {
        triggers.add("revealed")
        return this
    }

    fun build(): String {
        val result = mutableListOf<String>()

        for (trigger in triggers) {
            val triggerName = if (trigger.startsWith("every ")) "every" else trigger
            val mods = modifiers[triggerName]

            if (mods == null || mods.isEmpty()) {
                result.add(trigger)
            } else {
                val modString = mods.entries.joinToString(" ") { (key, value) ->
                    if (value == "true") key else "$key:$value"
                }

                if (trigger.startsWith("every ")) {
                    // Handle the special case for polling triggers
                    result.add("$trigger $modString")
                } else {
                    result.add("$trigger $modString")
                }
            }
        }

        return result.joinToString(", ")
    }
}

// Convenience functions for using the builders
fun CommonAttributeGroupFacade.hxSwap(init: SwapBuilder.() -> Unit) {
    val builder = SwapBuilder()
    builder.init()
    htmx(HtmxAttributes.HX_SWAP, builder.build())
}

fun CommonAttributeGroupFacade.hxTrigger(init: TriggerBuilder.() -> Unit) {
    val builder = TriggerBuilder()
    builder.init()
    htmx(HtmxAttributes.HX_TRIGGER, builder.build())
}
