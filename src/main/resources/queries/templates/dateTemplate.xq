let $date := "2021-01-31T23:45:23.235"

let $param_conv :=
  try {
      [$date cast as xs:dateTime,
      $date cast as xs:dateTime]

  } catch * {
  	try {
  		[fn:dateTime(($date cast as xs:date), xs:time("00:00:00")),
        fn:dateTime(($date cast as xs:date), xs:time("23:59:59.999"))]
    } catch * {
    	$err:code || $err:description
    }
  }

return $param_conv

(: array:get($param_conv, 1) :)