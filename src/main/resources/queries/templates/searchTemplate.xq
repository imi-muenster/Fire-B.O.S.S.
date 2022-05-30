let $result :=
<results>
{
  for $x in db:open("Patient")
  #CONSTANT_CONDITIONS

  #OPTIONAL_SEARCHPARAMETERS

  return $x
}
</results>

return $result