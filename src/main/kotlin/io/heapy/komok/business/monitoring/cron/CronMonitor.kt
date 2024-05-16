package io.heapy.komok.business.monitoring.cron

/**
 *
 * function sentry_start() {
 *     curl -X POST \
 *         'https://sentry.io/api/0/organizations/heapy/monitors/75b54c8c-7ef2-4077-9f00-adccd33841b3/checkins/' \
 *         --header "Authorization: DSN ${SENTRY_DSN}" \
 *         --header 'Content-Type: application/json' \
 *         --data-raw '{"status": "in_progress"}'
 * }
 *
 * function sentry_finish() {
 *     CHECKIN_ID=$1
 *     curl -X PUT \
 *         "https://sentry.io/api/0/organizations/heapy/monitors/75b54c8c-7ef2-4077-9f00-adccd33841b3/checkins/${CHECKIN_ID}/" \
 *         --header "Authorization: DSN ${SENTRY_DSN}" \
 *         --header 'Content-Type: application/json' \
 *         --data-raw '{"status": "ok"}'
 * }
 *
 */
class CronMonitor {
    fun createMonitor(
        cronName: String,
    ) {
        println("CronMonitor createMonitor")
    }

    fun checkin(
        cronId: String,
    ): CheckinResponse {
        println("CronMonitor checkin")
        return CheckinResponse(
            checkinId = "checkinId",
        )
    }

    data class CheckinResponse(
        val checkinId: String,
    )

    data class CheckoutRequest(
        val cronId: String,
        val checkinId: String,
    )

    fun checkout(
        request: CheckoutRequest,
    ) {
        println("CronMonitor checkout")
    }
}
