(ns fhirplace.resources.meta-test
  (:use midje.sweet)
  (:require
    [fhirplace.resources.meta :as m]))

(fact
  (count (m/elem-children "Patient")) => 21
  (count (m/elem-children "Patient.contact")) => 8
  (count (m/elem-children "Encounter.hospitalization")) => 14)

(fact
  (count (m/elem-children "Address")) => 10
  (count (m/elem-children "CodeableConcept")) => 4)
