package de.unimuenster.imi.medic.utils

import java.io.File
import java.io.FileInputStream
import javax.servlet.ServletContext

class Properties @JvmOverloads constructor(context: ServletContext? = null, path: String) {

    private val props: java.util.Properties
    private val file: File
    init {
        file = File(if (context != null) context.getRealPath(path) else { path })
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

}