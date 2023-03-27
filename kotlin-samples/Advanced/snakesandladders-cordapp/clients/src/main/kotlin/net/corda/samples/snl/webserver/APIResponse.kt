package net.corda.samples.snl.webserver

class APIResponse<T>(val message: String, val data: Any?, val isStatus: Boolean) {
    companion object {

        fun <T> success(): APIResponse<T> {
            return APIResponse("SUCCESS", null, true)
        }

        fun <T> success(data: T): APIResponse<T> {
            return APIResponse("SUCCESS", data, true)
        }

        fun <T> error(message: String): APIResponse<T> {
            return APIResponse(message, null, false)
        }
    }
}
