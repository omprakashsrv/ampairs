package com.ampairs.auth.utils

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class UniqueIdGenerator {
    private val random = Random()
    /**
     * Get the uniqueIdGroup.
     *
     * @return the uniqueIdGroup
     */
    /**
     * Set the uniqueIdGroup.
     *
     * @param uniqueIdGroup the uniqueIdGroup to set
     */
    var uniqueIdGroup = UniqueIdGroup.DATETIME
    /**
     * Get the allowedChars.
     *
     * @return the allowedChars
     */
    /**
     * Set the allowedChars.
     *
     * @param allowedChars the allowedChars to set
     */
    var allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789"

    /**
     * Generate a random string
     *
     * @return generated random string
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun generate(length: Int): String {
        return generate("", length)
    }

    /**
     * Generate a random string starting from the prefix, and having 'length' size
     *
     * @param prefix
     * @param length
     * @return generated random string
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun generate(prefix: String?, length: Int): String {
        val base = uniqueIdGroup.value
        val builder = StringBuilder(prefix)
        builder.append(base)
        require(length >= builder.length + 1) {
            ("Cannot generate random string of length " + length + " for GeneratorType: " + uniqueIdGroup.value)
        }
        appendRandomString(builder, length)
        return builder.substring(0, length).uppercase(Locale.getDefault())
    }

    /**
     * Populates builder with random characters selected from allowedChars having max length
     *
     * @param builder
     * @param length
     */
    @Throws(IllegalStateException::class)
    fun appendRandomString(builder: StringBuilder, length: Int) {
        check(allowedChars.length != 0) { "AllowedChars cannot be empty string" }
        while (builder.length < length) {
            val randomPosition = random.nextInt(allowedChars.length)
            builder.append(allowedChars[randomPosition])
        }
    }

    enum class UniqueIdGroup {
        DATE {

            override val value: String
                get() {
                    val now = Timestamp(System.currentTimeMillis())
                    val simpleDateFormat = SimpleDateFormat("yyyyMMdd")
                    return simpleDateFormat.format(now)
                }
        },
        DATETIME {
            override val value: String
                get() {
                    val now = Timestamp(System.currentTimeMillis())
                    val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmss")
                    return simpleDateFormat.format(now)
                }
        },
        DATETIMEMILLI {
            override val value: String
                get() {
                    val now = Timestamp(System.currentTimeMillis())
                    val simpleDateFormat = SimpleDateFormat("yyyyMMddHHmmssSSS")
                    return simpleDateFormat.format(now)
                }
        },
        NONE {

            override val value: String
                get() = ""
        },
        UUID_GEN {

            override val value: String
                get() = UUID.randomUUID().toString().replace("-".toRegex(), "")
        };

        abstract val value: String
    }
}
