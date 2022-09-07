package de.unimuenster.imi.medic.fhir

import ca.uhn.fhir.jpa.searchparam.SearchParameterMap
import ca.uhn.fhir.model.api.IQueryParameterType
import ca.uhn.fhir.rest.param.DateParam
import ca.uhn.fhir.rest.param.DateRangeParam
import de.unimuenster.imi.medic.fhir.helper.encodeFromResource
import de.unimuenster.imi.medic.utils.UUIDHelper
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.formats.ParserType
import org.hl7.fhir.r4.model.IdType
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.LinkedHashMap

fun IBaseResource.incrementVersion() {
    if (this.idElement.hasVersionIdPart()) {
        this.setId(
            IdType(
                this.fhirType(),
                this.idElement.idPart,
                "${this.idElement.versionIdPart.toInt() + 1}"
            )
        )
        this.meta.lastUpdated = Date(System.currentTimeMillis())
    } else {
        this.addVersion()
    }
}

fun IBaseResource.addVersion() {
    this.setId(
        IdType(
            this.fhirType(),
            this.idElement.idPart,
            "1"
        )
    )
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.setNewVersion(newVersion: String) {
    this.setId(IdType(this.fhirType(), this.idElement.idPart, newVersion))
    this.meta.versionId = newVersion
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.generateAndSetNewId() {
    this.setId(IdType(this.fhirType(), UUIDHelper.getUID(), "1"))
    this.meta.versionId = "1"
    this.meta.lastUpdated = Date(System.currentTimeMillis())
}

fun IBaseResource.hasVersionIdPart(): Boolean {
    return this.idElement!!.hasVersionIdPart()
}

fun IBaseResource.asXML(): String {
    return encodeFromResource(this, ParserType.XML)!!
}

fun IBaseResource.asJSON(): String {
    return encodeFromResource(this, ParserType.JSON)!!
}

fun DateRangeParam.completeInformation() {
    this.fillInRange()
    this.convertToDateTime()
}

fun DateRangeParam.fillInRange() {
    if (this.lowerBound == null) {
        this.lowerBound = DateParam("gt1970-01-01T00:00")
    }
    if (this.upperBound == null) {
        this.upperBound = DateParam("le${Date(System.currentTimeMillis()).toInstant()}")
    }
}

fun DateRangeParam.convertToDateTime() {
    this.lowerBound = DateParam(
        this.lowerBound.prefix.value +
                DateTime(this.lowerBound.value).toString()
    )
    this.upperBound = DateParam(
        this.upperBound.prefix.value +
                DateTime(this.upperBound.value).toString()
    )
}

fun SearchParameterMap.getSearchParameterMap(): LinkedHashMap<String, List<List<IQueryParameterType>>> {
    return SearchParameterMap::class.java.getDeclaredField("mySearchParameterMap").let {
        it.isAccessible = true
        val value = it.get(this)
        return@let (value as LinkedHashMap<String, List<List<IQueryParameterType>>>)
    }
}