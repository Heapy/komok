package io.heapy.komok.tech.ktor.htmx

object HtmxRequestHeaders {
    /**
     * indicates that the request is via an element using hx-boost
     */
    const val HX_BOOSTED = "HX-Boosted"

    /**
     * the current URL of the browser
     */
    const val HX_CURRENT_URL = "HX-Current-URL"

    /**
     * “true” if the request is for history restoration after a miss in the local history cache
     */
    const val HX_HISTORY_RESTORE_REQUEST = "HX-History-Restore-Request"

    /**
     * the user response to an hx-prompt
     */
    const val HX_PROMPT = "HX-Prompt"

    /**
     * always “true”
     */
    const val HX_REQUEST: String = "HX-Request"

    /**
     * the id of the target element if it exists
     */
    const val HX_TARGET = "HX-Target"

    /**
     * the name of the triggered element if it exists
     */
    const val HX_TRIGGER_NAME = "HX-Trigger-Name"

    /**
     * the id of the triggered element if it exists
     */
    const val HX_TRIGGER = "HX-Trigger"
}
