package edu.skku.map.airllution.location

class MyLocation(
    var latitude: Double,
    var longitude: Double,
    var cityName: String
): java.io.Serializable {
    constructor(): this(0.0, 0.0, "")
}