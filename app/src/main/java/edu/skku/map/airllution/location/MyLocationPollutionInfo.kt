package edu.skku.map.airllution.location

class MyLocationPollutionInfo(
    var cityName: String,
    var status: String,
    var totalScore: String,
    var pmScore: String,
    var ultraPmScore: String,
    var latitude: Double,
    var longitude: Double
) {
    constructor(cityName: String, latitude: Double, longitude: Double): this(cityName, "", "", "", "", latitude, longitude) { }
}