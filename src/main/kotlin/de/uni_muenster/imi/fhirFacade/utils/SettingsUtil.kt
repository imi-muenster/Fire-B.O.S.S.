package de.uni_muenster.imi.fhirFacade.utils

import java.io.File
import java.io.FileInputStream
import javax.servlet.ServletContext

class Properties(context: ServletContext, path: String) {

    val file = File(context.getRealPath(path))
    private val props = java.util.Properties()

    init {
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
        return get(key).toBoolean() ?: false
    }

    fun getList(key: String): List<String> {
        return get(key)?.split(",") ?: listOf()
    }
}