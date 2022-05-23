package de.uni_muenster.imi.fhirFacade.utils

import java.text.SimpleDateFormat
import java.util.*


object UUIDHelper {

    private const val MIN_SEQ = 0
    private const val MAX_SEQ = 9

    private var lastDate = SimpleDateFormat("yyMMddHHmmssSSS").format(Date())
    private var lastSequence = 0

    fun getUID(): String {
        val currentDateTime = SimpleDateFormat("yyMMddHHmmssSSS").format(Date())
        if (currentDateTime == lastDate) {
            lastSequence++
        } else {
            lastDate = currentDateTime
            lastSequence = MIN_SEQ
        }
        check(lastSequence <= MAX_SEQ) { "Sequence numbers out of range.!" }
        return "$lastDate$lastSequence"
    }

}