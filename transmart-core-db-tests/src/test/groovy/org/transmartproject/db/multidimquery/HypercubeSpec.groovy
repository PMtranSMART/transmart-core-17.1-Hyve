package org.transmartproject.db.multidimquery

import com.google.common.collect.ImmutableMap
import spock.lang.Specification

class HypercubeSpec extends Specification {


    def "test hypercube projection mapping"() {

        when:
        ImmutableMap<String, Integer> mockMap = ImmutableMap.of(
                "zero", 0,
                "one", 1,
                "two", 2
        )
        Object[] mockObject = ["zeroValue", "firstValue", "secondValue"] as Object[]

        ProjectionMap map = new ProjectionMap(mockMap, mockObject)

        ImmutableMap<String, Integer> compareMockMap = ImmutableMap.of(
                "one", 0,
                "two", 1,
                "zero", 2
        )
        Object[] compareMockObject = ["firstValue", "secondValue", "zeroValue"] as Object[]

        ProjectionMap compareMap = new ProjectionMap(compareMockMap, compareMockObject)

        then:
        // equal, ignore order
        map == compareMap

        // size
        map.size() == 3

        // map entries, key, value
        map['one'] == "firstValue"
        "zero" in map.keySet()
        "secondValue" in map.values()
    }
}