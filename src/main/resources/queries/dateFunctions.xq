declare function local:getUpperDateRange($date as xs:string?) as xs:string?
{
	if ( (string-length($date) = 4) ) (: YYYY :)
    	then (
            let $result := fn:concat(
            	$date, "-", "12", "-", "31", "T", "23",
            	":", "59", ":", "59",
                ".", "999"
            )
        	return $result
        )
	else if ( (string-length($date) = 7) ) (: YYYY-MM :)
    	then (
            let $result := fn:concat(
            	$date, "-", "31", "T", "23",
            	":", "59", ":", "59",
                ".", "999"
            )
        	return $result
        )
	else if ( (string-length($date) = 10) ) (: YYYY-MM-dd :)
    	then (
            let $result := fn:concat($date, "T", "23",
            	":", "59", ":", "59",
                ".", "999"
            )
        	return $result
        )
    else if ( (string-length($date) = 13) ) (: YYYY-MM-ddTHH :)
    	then (
    		let $result := fn:concat(
            	$date, ":", "59",
            	":", "59", ".", "999"
            )
        	return $result
        )
    else if ( (string-length($date) = 16) ) (: YYYY-MM-ddTHH:mm :)
    	then (
    		let $result := fn:concat(
            	$date, ":", "59", ".", "999"
            )
        	return $result
        )
    else if ( (string-length($date) = 19) ) (: YYYY-MM-ddTHH:mm:ss :)
    	then (
    		let $result := fn:concat(
            	$date, ".", "999"
            )
        	return $result
        )
    else if ( (string-length($date) = 23) ) (: YYYY-MM-ddTHH:mm:ss:fff :)
    	then (
        	let $result := $date
        	return $result
        )
    else if ( (string-length($date) > 23) )
        then (
            let $result := fn:substring($date, 1, 23)
            return $result
        )
    else ()
};

declare function local:getLowerDateRange($date as xs:string?) as xs:string?
{
	if ( (string-length($date) = 4) ) (: YYYY :)
    	then (
            let $result := fn:concat(
            	$date, "-", "01", "-", "01", "T", "00",
            	":", "00", ":", "00",
                ".", "000"
            )
        	return $result
        )
	else if ( (string-length($date) = 7) ) (: YYYY-MM :)
    	then (
            let $result := fn:concat(
                $date, "-", "01", "T", "00",
                ":", "00", ":", "00",
                ".", "000"
            )
        	return $result
        )
	else if ( (string-length($date) = 10) ) (: YYYY-MM-dd :)
    	then (
            let $result := fn:concat(
                $date, "T", "00",
                ":", "00", ":", "00",
                ".", "000"
            )
        	return $result
        )
    else if ( (string-length($date) = 13) ) (: YYYY-MM-ddTHH :)
    	then (
    		let $result := fn:concat(
                $date, ":",
                "00", ":", "00", ".", "000"
            )
        	return $result
        )
    else if ( (string-length($date) = 16) ) (: YYYY-MM-ddTHH:mm :)
    	then (
    		let $result := fn:concat(
                $date, ":", "00", ".", "000"
            )
        	return $result
        )
    else if ( (string-length($date) = 19) ) (: YYYY-MM-ddTHH:mm:ss :)
    	then (
    		let $result := fn:concat(
                $date, ".", "000"
            )
        	return $result
        )
    else if ( (string-length($date) = 23) ) (: YYYY-MM-ddTHH:mm:ss:fff :)
    	then (
        	let $result := $date
        	return $result
        )
    else if ( (string-length($date) > 23) )
        then (
            let $result := fn:substring($date, 1, 23)
            return $result
        )
    else ()
};
