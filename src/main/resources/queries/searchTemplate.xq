let $result :=
<results>
{
  for $x in db:open("#TYPE")
  #CONSTANT_CONDITIONS

  #OPTIONAL_SEARCHPARAMETERS

  return element result {$x}
}
</results>

return $result