package de.unimuenster.imi.medic.utils

import java.io.File
import java.io.FileInputStream
import javax.servlet.ServletContext

class Properties @JvmOverloads constructor(context: ServletContext? = null, path: String) {

    private val props: java.util.Properties
    private val file: File
    init {
        if (context != null) {
            file = File(context.getRealPath(path))
        } else {
            file = File(path)
        }
        props = java.util.Properties()
        props.load(FileInputStream(file))
    }

    operator fun get(key: String): String? {
        return System.getProperty(key) ?: props.getProperty(key)
    }

    operator fun get(key: String, required: Boolean): String {
        return System.getProperty(key) ?: props.getProperty(key)
        ?: throw Exception("Cannot find setting '$key'! Please add to $file or VM properties!")
    }

    fun getBool(key: String): Boolean {
        return get(key).toBoolean()
    }

    fun getList(key: String): List<String> {
        return get(key)?.split(",") ?: listOf()
    }
}