package io.particle.android.sdk.utils

/**
 * Methods copied from Apache commons-lang, so that I don't have to incur the 4k+ method overhead
 * of commons-lang itself.
 */
object ParticleDeviceSetupInternalStringUtils {
    private const val INDEX_NOT_FOUND = -1

    fun remove(str: String?, remove: String): String? {
        return if (isEmpty(str) || isEmpty(remove)) {
            str
        } else replace(str!!, remove, "", -1)
    }

    fun removeStart(str: String, remove: String): String {
        if (isEmpty(str) || isEmpty(remove)) {
            return str
        }
        return if (str.startsWith(remove)) {
            str.substring(remove.length)
        } else str
    }

    fun removeEnd(str: String, remove: String): String {
        if (isEmpty(str) || isEmpty(remove)) {
            return str
        }
        return if (str.endsWith(remove)) {
            str.substring(0, str.length - remove.length)
        } else str
    }

    private fun replace(text: String, searchString: String,
                        replacement: String?, max: Int): String? {
        var mutableMax = max
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || mutableMax == 0) {
            return text
        }
        var start = 0
        var end = text.indexOf(searchString, start)
        if (end == INDEX_NOT_FOUND) {
            return text
        }
        val replLength = searchString.length
        var increase = replacement.length - replLength
        increase = if (increase < 0) 0 else increase
        increase *= if (mutableMax < 0) 16 else if (mutableMax > 64) 64 else mutableMax
        val buf = StringBuilder(text.length + increase)
        while (end != INDEX_NOT_FOUND) {
            buf.append(text.substring(start, end)).append(replacement)
            start = end + replLength
            if (--mutableMax == 0) {
                break
            }
            end = text.indexOf(searchString, start)
        }
        buf.append(text.substring(start))
        return buf.toString()
    }

    private fun isEmpty(cs: CharSequence?): Boolean {
        return cs == null || cs.isEmpty()
    }
}
