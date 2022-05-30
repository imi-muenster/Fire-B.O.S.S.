package de.uni_muenster.imi.fhirFacade.fhir.helper

import ca.uhn.fhir.rest.param.NumberParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import org.apache.commons.lang.math.DoubleRange
import java.math.BigDecimal
import java.math.MathContext

fun NumberParam.getApproximationRange(): DoubleRange {
    //Recommended value for approximation: 10% of the stated value
    val min = ( this.value.toDouble() ) - ( this.value.toDouble() * 0.1 )
    val max = ( this.value.toDouble() ) + ( this.value.toDouble() * 0.1 )
    return DoubleRange(min, max)
}

fun NumberParam.getSignificantRange(): DoubleRange {
    val fuzz = getFuzzAmount(this.prefix, this.value)
    val min = this.value.subtract(fuzz, MathContext.DECIMAL64)
    val max = this.value.add(fuzz, MathContext.DECIMAL64)

    return DoubleRange(min, max)
}

fun getFuzzAmount(prefixType: ParamPrefixEnum, theValue: BigDecimal): BigDecimal? {
    return if (prefixType == ParamPrefixEnum.APPROXIMATE) {
        theValue.multiply(BigDecimal(0.1))
    } else {
        val plainString: String = theValue.toPlainString()
        val dotIdx = plainString.indexOf('.')
        if (dotIdx == -1) {
            return BigDecimal(0.5)
        }
        val precision = plainString.length - dotIdx
        val mul = Math.pow(10.0, -precision.toDouble())
        val fuzz = mul * 5.0
        BigDecimal(fuzz)
    }
}