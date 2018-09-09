// This file is comprised entirely of code (C) the ASF, but modified
// to fit into a single file, for simplicity

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.particle.android.sdk.devicesetup.commands

import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.util.*

/**
 * Created by jensck from code lifted from Apache commons-lang, to avoid pulling in a 4k+ method
 * count dependency just for a few String methods
 */
internal object CommandClientUtils {
    private val JAVA_CTRL_CHARS_ESCAPE = arrayOf<Array<CharSequence>>(
            arrayOf("\b", "\\b"),
            arrayOf("\n", "\\n"),
            arrayOf("\t", "\\t"),
            arrayOf("\\f", "\\f"),
            arrayOf("\r", "\\r"))

    private val ESCAPE_JAVA = LookupTranslator(
            arrayOf<Array<CharSequence>>(arrayOf("\"", "\\\""), arrayOf("\\", "\\\\")))
            .with(LookupTranslator(JAVA_CTRL_CHARS_ESCAPE()))
            .with(JavaUnicodeEscaper.outsideOf(32, 0x7f))

    fun escapeJava(input: String): String? {
        return ESCAPE_JAVA.translate(input)
    }

    // Everything beneath this comment line is all just to feed the `escapeJava()` method above.
    private fun JAVA_CTRL_CHARS_ESCAPE(): Array<Array<CharSequence>> {
        return JAVA_CTRL_CHARS_ESCAPE.clone()
    }

    internal abstract class CharSequenceTranslator {

        /**
         * Translate a set of codepoints, represented by an int index into a CharSequence,
         * into another set of codepoints. The number of codepoints consumed must be returned,
         * and the only IOExceptions thrown must be from interacting with the Writer so that
         * the top level API may reliably ignore StringWriter IOExceptions.
         *
         * @param input CharSequence that is being translated
         * @param index int representing the current point of translation
         * @param out Writer to translate the text to
         * @return int count of codepoints consumed
         * @throws IOException if and only if the Writer produces an IOException
         */
        @Throws(IOException::class)
        internal abstract fun translate(input: CharSequence, index: Int, out: Writer): Int

        /**
         * Helper for non-Writer usage.
         * @param input CharSequence to be translated
         * @return String output of translation
         */
        fun translate(input: CharSequence?): String? {
            if (input == null) {
                return null
            }
            try {
                val writer = StringWriter(input.length * 2)
                translate(input, writer)
                return writer.toString()
            } catch (ioe: IOException) {
                // this should never ever happen while writing to a StringWriter
                throw RuntimeException(ioe)
            }

        }

        /**
         * Translate an input onto a Writer. This is intentionally final as its algorithm is
         * tightly coupled with the abstract method of this class.
         *
         * @param input CharSequence that is being translated
         * @param out Writer to translate the text to
         * @throws IOException if and only if the Writer produces an IOException
         */
        @Throws(IOException::class)
        private fun translate(input: CharSequence?, out: Writer?) {
            if (out == null) {
                throw IllegalArgumentException("The Writer must not be null")
            }
            if (input == null) {
                return
            }
            var pos = 0
            val len = input.length
            while (pos < len) {
                val consumed = translate(input, pos, out)
                if (consumed == 0) {
                    // inlined implementation of Character.toChars(Character.codePointAt(input, pos))
                    // avoids allocating temp char arrays and duplicate checks
                    val c1 = input[pos]
                    out.write(c1.toInt())
                    pos++
                    if (Character.isHighSurrogate(c1) && pos < len) {
                        val c2 = input[pos]
                        if (Character.isLowSurrogate(c2)) {
                            out.write(c2.toInt())
                            pos++
                        }
                    }
                    continue
                }
                // contract with translators is that they have to understand codepoints
                // and they just took care of a surrogate pair
                for (pt in 0 until consumed) {
                    pos += Character.charCount(Character.codePointAt(input, pos))
                }
            }
        }

        /**
         * Helper method to create a merger of this translator with another set of
         * translators. Useful in customizing the standard functionality.
         *
         * @param translators CharSequenceTranslator array of translators to merge with this one
         * @return CharSequenceTranslator merging this translator with the others
         */
        internal fun with(vararg translators: CharSequenceTranslator): CharSequenceTranslator {
            val newArray = arrayOfNulls<CharSequenceTranslator>(translators.size + 1)
            newArray[0] = this
            System.arraycopy(translators, 0, newArray, 1, translators.size)
            return AggregateTranslator(newArray)
        }

        companion object {

            internal val HEX_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

            /**
             *
             * Returns an upper case hexadecimal `String` for the given
             * character.
             *
             * @param codepoint The codepoint to convert.
             * @return An upper case hexadecimal `String`
             */
            internal fun hex(codepoint: Int): String {
                return Integer.toHexString(codepoint).toUpperCase(Locale.ENGLISH)
            }
        }

    }


    private class AggregateTranslator
    /**
     * Specify the translators to be used at creation time.
     *
     * @param translators CharSequenceTranslator array to aggregate
     */
    internal constructor(translators: Array<CharSequenceTranslator?>) : CharSequenceTranslator() {
        private val translators: Array<CharSequenceTranslator?> = translators.clone()

        /**
         * The first translator to consume codepoints from the input is the 'winner'.
         * Execution stops with the number of consumed codepoints being returned.
         * {@inheritDoc}
         */
        @Throws(IOException::class)
        override fun translate(input: CharSequence, index: Int, out: Writer): Int {
            for (translator in translators) {
                val consumed = translator!!.translate(input, index, out)
                if (consumed != 0) {
                    return consumed
                }
            }
            return 0
        }

    }

    internal abstract class CodePointTranslator : CharSequenceTranslator() {

        /**
         * Implementation of translate that maps onto the abstract translate(int, Writer) method.
         * {@inheritDoc}
         */
        @Throws(IOException::class)
        override fun translate(input: CharSequence, index: Int, out: Writer): Int {
            val codepoint = Character.codePointAt(input, index)
            val consumed = translate(codepoint, out)
            return if (consumed) 1 else 0
        }

        /**
         * Translate the specified codepoint into another.
         *
         * @param codepoint int character input to translate
         * @param out Writer to optionally push the translated output to
         * @return boolean as to whether translation occurred or not
         * @throws IOException if and only if the Writer produces an IOException
         */
        @Throws(IOException::class)
        internal abstract fun translate(codepoint: Int, out: Writer): Boolean

    }


    internal open class UnicodeEscaper
    /**
     *
     * Constructs a `UnicodeEscaper` for the specified range. This is
     * the underlying method for the other constructors/builders. The `below`
     * and `above` boundaries are inclusive when `between` is
     * `true` and exclusive when it is `false`.
     *
     * @param below int value representing the lowest codepoint boundary
     * @param above int value representing the highest codepoint boundary
     * @param between whether to escape between the boundaries or outside them
     */
    (private val below: Int, private val above: Int, private val between: Boolean) : CodePointTranslator() {

        /**
         *
         * Constructs a `UnicodeEscaper` for all characters.
         */
        private constructor() : this(0, Integer.MAX_VALUE, true)

        /**
         *
         * Constructs a `UnicodeEscaper` below the specified value (exclusive).
         *
         * @param codepoint below which to escape
         * @return the newly created `UnicodeEscaper` instance
         */
        private fun below(codepoint: Int): UnicodeEscaper {
            return outsideOf(codepoint, Integer.MAX_VALUE)
        }

        /**
         *
         * Constructs a `UnicodeEscaper` above the specified value (exclusive).
         *
         * @param codepoint above which to escape
         * @return the newly created `UnicodeEscaper` instance
         */
        private fun above(codepoint: Int): UnicodeEscaper {
            return outsideOf(0, codepoint)
        }

        /**
         *
         * Constructs a `UnicodeEscaper` outside of the specified values (exclusive).
         *
         * @param codepointLow below which to escape
         * @param codepointHigh above which to escape
         * @return the newly created `UnicodeEscaper` instance
         */
        private fun outsideOf(codepointLow: Int, codepointHigh: Int): UnicodeEscaper {
            return UnicodeEscaper(codepointLow, codepointHigh, false)
        }

        /**
         *
         * Constructs a `UnicodeEscaper` between the specified values (inclusive).
         *
         * @param codepointLow above which to escape
         * @param codepointHigh below which to escape
         * @return the newly created `UnicodeEscaper` instance
         */
        private fun between(codepointLow: Int, codepointHigh: Int): UnicodeEscaper {
            return UnicodeEscaper(codepointLow, codepointHigh, true)
        }

        /**
         * {@inheritDoc}
         */
        @Throws(IOException::class)
        override fun translate(codepoint: Int, out: Writer): Boolean {
            if (between) {
                if (codepoint < below || codepoint > above) {
                    return false
                }
            } else {
                if (codepoint in below..above) {
                    return false
                }
            }

            // TODO: Handle potential + sign per various Unicode escape implementations
            if (codepoint > 0xffff) {
                out.write(toUtf16Escape(codepoint))
            } else {
                out.write("\\u")
                out.write(CommandClientUtils.CharSequenceTranslator.HEX_DIGITS[codepoint shr 12 and 15].toInt())
                out.write(CommandClientUtils.CharSequenceTranslator.HEX_DIGITS[codepoint shr 8 and 15].toInt())
                out.write(CommandClientUtils.CharSequenceTranslator.HEX_DIGITS[codepoint shr 4 and 15].toInt())
                out.write(CommandClientUtils.CharSequenceTranslator.HEX_DIGITS[codepoint and 15].toInt())
            }
            return true
        }

        /**
         * Converts the given codepoint to a hex string of the form `"\\uXXXX"`
         *
         * @param codepoint
         * a Unicode code point
         * @return the hex string for the given codepoint
         *
         * @since 3.2
         */
        protected open fun toUtf16Escape(codepoint: Int): String {
            return "\\u" + CommandClientUtils.CharSequenceTranslator.hex(codepoint)
        }
    }


    private class JavaUnicodeEscaper
    /**
     *
     *
     * Constructs a `JavaUnicodeEscaper` for the specified range. This is the underlying method for the
     * other constructors/builders. The `below` and `above` boundaries are inclusive when
     * `between` is `true` and exclusive when it is `false`.
     *
     *
     * @param below
     * int value representing the lowest codepoint boundary
     * @param above
     * int value representing the highest codepoint boundary
     * @param between
     * whether to escape between the boundaries or outside them
     */
    private constructor(below: Int, above: Int, between: Boolean) : UnicodeEscaper(below, above, between) {

        /**
         *
         *
         * Constructs a `JavaUnicodeEscaper` above the specified value (exclusive).
         *
         *
         * @param codepoint
         * above which to escape
         * @return the newly created `UnicodeEscaper` instance
         */
        private fun above(codepoint: Int): JavaUnicodeEscaper {
            return outsideOf(0, codepoint)
        }

        /**
         *
         *
         * Constructs a `JavaUnicodeEscaper` below the specified value (exclusive).
         *
         *
         * @param codepoint
         * below which to escape
         * @return the newly created `UnicodeEscaper` instance
         */
        private fun below(codepoint: Int): JavaUnicodeEscaper {
            return outsideOf(codepoint, Integer.MAX_VALUE)
        }

        companion object {

            /**
             *
             *
             * Constructs a `JavaUnicodeEscaper` between the specified values (inclusive).
             *
             *
             * @param codepointLow
             * above which to escape
             * @param codepointHigh
             * below which to escape
             * @return the newly created `UnicodeEscaper` instance
             */
            private fun between(codepointLow: Int, codepointHigh: Int): JavaUnicodeEscaper {
                return JavaUnicodeEscaper(codepointLow, codepointHigh, true)
            }

            /**
             *
             *
             * Constructs a `JavaUnicodeEscaper` outside of the specified values (exclusive).
             *
             *
             * @param codepointLow
             * below which to escape
             * @param codepointHigh
             * above which to escape
             * @return the newly created `UnicodeEscaper` instance
             */
            internal fun outsideOf(codepointLow: Int, codepointHigh: Int): JavaUnicodeEscaper {
                return JavaUnicodeEscaper(codepointLow, codepointHigh, false)
            }
        }

        /**
         * Converts the given codepoint to a hex string of the form `"\\uXXXX\\uXXXX"`
         *
         * @param codepoint
         * a Unicode code point
         * @return the hex string for the given codepoint
         */
        override fun toUtf16Escape(codepoint: Int): String {
            val surrogatePair = Character.toChars(codepoint)
            return "\\u" + CommandClientUtils.CharSequenceTranslator.hex(surrogatePair[0].toInt()) + "\\u" + CommandClientUtils.CharSequenceTranslator.hex(surrogatePair[1].toInt())
        }

    }


    internal class LookupTranslator
    /**
     * Define the lookup table to be used in translation
     *
     * Note that, as of Lang 3.1, the key to the lookup table is converted to a
     * java.lang.String. This is because we need the key to support hashCode and
     * equals(Object), allowing it to be the key for a HashMap. See LANG-882.
     *
     * @param lookup CharSequence[][] table of size [*][2]
     */
    internal constructor(lookup: Array<Array<CharSequence>>?) : CharSequenceTranslator() {

        private val lookupMap: HashMap<String, String> = HashMap()
        private val prefixSet: HashSet<Char> = HashSet()
        private val shortest: Int
        private val longest: Int

        init {
            var _shortest = Integer.MAX_VALUE
            var _longest = 0
            if (lookup != null) {
                for (seq in lookup) {
                    this.lookupMap[seq[0].toString()] = seq[1].toString()
                    this.prefixSet.add(seq[0][0])
                    val sz = seq[0].length
                    if (sz < _shortest) {
                        _shortest = sz
                    }
                    if (sz > _longest) {
                        _longest = sz
                    }
                }
            }
            shortest = _shortest
            longest = _longest
        }

        /**
         * {@inheritDoc}
         */
        @Throws(IOException::class)
        override fun translate(input: CharSequence, index: Int, out: Writer): Int {
            // check if translation exists for the input at position index
            if (prefixSet.contains(input[index])) {
                var max = longest
                if (index + longest > input.length) {
                    max = input.length - index
                }
                // implement greedy algorithm by trying maximum match first
                for (i in max downTo shortest) {
                    val subSeq = input.subSequence(index, index + i)
                    val result = lookupMap[subSeq.toString()]

                    if (result != null) {
                        out.write(result)
                        return i
                    }
                }
            }
            return 0
        }
    }

}
