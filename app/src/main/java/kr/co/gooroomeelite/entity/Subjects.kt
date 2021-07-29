package kr.co.gooroomeelite.entity

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Subjects(
    val uid: String? = null,
    var name: String? = null,
    var color: String? = null,
    var studytime: Int = 0,
    var prevDocumentId: String? = null,
    var nextDocumentId: String? = null,
    var dayStartTime : String? = null,

    var time_01 : String? = null,

    @ServerTimestamp
    val timestamp: Date? = null,
    val plusStudytime : Int = 0
//    @ServerTimestamp
//    val studyStartTime : Date? = null,
//    @ServerTimestamp
//    val studyEndTime : Date? = null
)
