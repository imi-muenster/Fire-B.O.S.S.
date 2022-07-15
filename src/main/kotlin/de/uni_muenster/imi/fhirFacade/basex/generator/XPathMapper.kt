package de.uni_muenster.imi.fhirFacade.basex.generator

import java.util.*

object XPathMapper {

    fun mapPathToXPath(path: String): String {
        //TODO: First check for "|" or "and", split and handle parts recursively (?), connect with "where ..."
        //Special path handling
        return if (path.contains("where")) {
            handlePathWithWhere(path)
        } else if (path.contains(" as ") || path.contains(".as(")) {
            handlePathWithAs(path)
        } else { //Normal Path handling (. separated)
            path.split(".").joinToString("/")
        }
    }

    fun removeTypeFromPath(path: String): String {
        return path.split("/")
            .drop(1)
            .joinToString("/")
    }

    private fun handlePathWithWhere(path: String): String {
        val partBefore = path.substringBefore(".where")
        val partWhere = path.substringAfter("$partBefore.")

        if (partWhere.contains("resolve()")) {
            val resolveType = partWhere.substringAfter("resolve() is ")
            //TODO: Evaluate: with or without /.. at the end. Should be without as the reference is the important bit
            return "${partBefore.split(".").joinToString("/")}/reference[contains(@value, \"$resolveType\")]"
        } else {
            val partAfter = partWhere.substringAfter(".", "")

            //looks like this: where(property='value')
            val property = partWhere
                .substringAfter("(")
                .substringBefore("=")
            val value = partWhere
                .substringAfter("='")
                .substringBefore("')")

            return "${partBefore.split(".").joinToString("/")}/$property[contains(@value, \"$value\")]/.." +
                    if (partAfter.isNotEmpty()) "/$partAfter" else ""
        }
    }

    private fun handlePathWithAs(path: String): String {
        val partBefore: String
        val partAfter: String
        if (path.contains(" as ")) {
            partBefore = path.substringBefore(" as ")
            partAfter = path
                .substringAfter(" as ")
                .capitalize()
        } else {
            partBefore = path.substringBefore(".as(")
            partAfter = path
                .substringAfter(".as(")
                .substringBefore(")")
                .capitalize()
        }
        return "${partBefore.split(".").joinToString("/")}$partAfter"
    }

    private fun String.capitalize(): String {
        return this.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault())
            else it.toString()
        }
    }

}