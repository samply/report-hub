library
Retrieve
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

codesystem SampleMaterialType: 'https://fhir.bbmri.de/CodeSystem/SampleMaterialType'

context Patient

define ExliquidSpecimen:
  from [Specimen] S
  where S.identifier.system contains 'https://dktk.dkfz.de/fhir/NamingSystem/exliquid-specimen'

define InInitialPopulation:
  exists ExliquidSpecimen

define Diagnosis:
  First(from [Condition] C
    return C.code.coding.where(system = 'http://fhir.de/CodeSystem/dimdi/icd-10-gm').code.first())

define function SampleType(specimen FHIR.Specimen):
  specimen.type.coding.where(system = 'https://fhir.bbmri.de/CodeSystem/SampleMaterialType').code.first()
