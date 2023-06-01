package edu.skku.map.airllution

class DataModel {

    /**
     * 대기 오염정보 백엔드 API
     */
    data class PollutionInfo(
        var totalGrade: String ?= null,
        var totalScore: String ?= null,
        var pmGrade: String ?= null,
        var pmScore: String ?= null,
        var ultraPmGrade: String ?= null,
        var ultraPmScore: String ?= null,
        var so2Grade: String ?= null,
        var so2Score: String ?= null,
        var coGrade: String ?= null,
        var coScore: String ?= null,
        var o3Grade: String ?= null,
        var o3Score: String ?= null,
        var no2Grade: String ?= null,
        var no2Score: String ?= null,
        var currentDate: String ?= null
    )

    data class MainInfo(
        var cityName: String ?= null,
        var pollutionInfo: PollutionInfo ?= null
    )

    /**
     * 네이버 뉴스 API
     */
    data class SingleNewsItem(
        var title: String ?= null,
        var originallink: String ?= null,
        var link: String ?= null,
        var description: String ?= null,
        var pubDate: String ?= null
    )

    data class NewsItem(
        var lastBuildDate: String ?= null,
        var total: Int ?= null,
        var start: Int ?= null,
        var display: Int ?= null,
        var items: ArrayList<SingleNewsItem> ?= null
    )

}