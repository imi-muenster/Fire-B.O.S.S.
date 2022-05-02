for $x in db:open("#TYPE")/#TYPE/id[contains(@value, "#ID")]
where $x/../meta/versionId/@value="#VERSION"

return $x/../..