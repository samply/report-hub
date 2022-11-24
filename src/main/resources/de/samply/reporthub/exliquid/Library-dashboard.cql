library "Library-dashboard"
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

codesystem SampleMaterialType: 'https://fhir.bbmri.de/CodeSystem/SampleMaterialType'

context Patient

define ExliquidSpecimen:
  from [Specimen] S
  where S.identifier.system contains 'https://dktk.dkfz.de/fhir/NamingSystem/exliquid-specimen'

define InInitialPopulation:
  exists ExliquidSpecimen

define retrieveCondition:
 First(from [Condition] C
  return C.code.coding.where(system = 'http://fhir.de/CodeSystem/bfarm/icd-10-gm').code.first())

define Diagnosis:
  if (retrieveCondition is null) then 'unknown' else retrieveCondition

define function SampleType(specimen FHIR.Specimen):
  specimen.type.coding.where(system = 'https://fhir.bbmri.de/CodeSystem/SampleMaterialType').code.first()
