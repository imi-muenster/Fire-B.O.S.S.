for $x in db:open("#TYPE")/#TYPE/id[contains(@value, "#ID")]

let $split := tokenize(base-uri($x), "/")
let $db := $split[2]
let $file := $split[3]

return db:delete($db, $file)