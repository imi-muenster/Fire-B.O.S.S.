package de.unimuenster.imi.medic.fhir.helper

import ca.uhn.fhir.rest.param.NumberParam
import ca.uhn.fhir.rest.param.ParamPrefixEnum
import ca.uhn.fhir.rest.param.QuantityParam
import org.apache.commons.lang.math.DoubleRange
import java.math.BigDecimal
import java.math.MathContext

object SignificanceHelper {

    fun getSignificantRange(param: NumberParam): DoubleRange {
        return computeSignificantRange(param.prefix, param.value)
    }

    fun getSignificantRange(param: QuantityParam): DoubleRange {
        return computeSignificantRange(param.prefix, param.value)
    }


    private fun computeSignificantRange(prefixType: ParamPrefixEnum?, value: BigDecimal): DoubleRange {
        val fuzz = getFuzzAmount(prefixType, value)
        val min = value.subtract(fuzz, MathContext.DECIMAL64)
        val max = value.add(fuzz, MathContext.DECIMAL64)

        return DoubleRange(min, max)
    }

    private fun getFuzzAmount(prefixType: ParamPrefixEnum?, theValue: BigDecimal): BigDecimal? {
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
}