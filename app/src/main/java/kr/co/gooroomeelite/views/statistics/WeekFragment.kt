package kr.co.gooroomeelite.views.statistics

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dhaval2404.colorpicker.util.setVisibility
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.firebase.firestore.FirebaseFirestore
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import kr.co.gooroomeelite.R
import kr.co.gooroomeelite.adapter.WeeklySubjectAdapter
import kr.co.gooroomeelite.databinding.FragmentWeekBinding
import kr.co.gooroomeelite.entity.Subject
import kr.co.gooroomeelite.entity.Weekly
import kr.co.gooroomeelite.utils.LoginUtils
import kr.co.gooroomeelite.viewmodel.SubjectViewModel
import kr.co.gooroomeelite.views.statistics.share.ShareActivity
import java.io.Serializable
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


//@RequiresApi(Build.VERSION_CODES.O)
@RequiresApi(Build.VERSION_CODES.Q)
class WeekFragment : Fragment() {
    private lateinit var binding: FragmentWeekBinding
    private val viewModel: SubjectViewModel by viewModels()
    private val weeklySubjectAdapter: WeeklySubjectAdapter by lazy { WeeklySubjectAdapter(emptyList()) }
    private var list: MutableList<Subject> = mutableListOf()

    //    var subject: Subject? = null
    var weekly: Weekly? = null


    //아래,왼쪽 제목 이름
    private val ContentColor by lazy {
        ContextCompat.getColor(this.requireContext(), R.color.content_black)
    }

    //그래프 가로 축,선 (점선으로 변경)
    private val transparentBlackColor by lazy {
        ContextCompat.getColor(this.requireContext(), R.color.transparent_black)
    }
//    private val customMarkerView by lazy {
//        CustomMarketView(this.requireContext(), R.layout.item_marker_view)
//    }

    //   LocalDate 문자열로 포맷
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("E")

    private var subjectListValue: MutableList<Subject> = mutableListOf()
    var count: Int = -1
    var weekCount: Int = -1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWeekBinding.inflate(inflater, container, false)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_week, container, false)
        binding.week = this

        binding.weekBarChart.setNoDataText("")

        initChart()

        weeklySubjectPieChart()
        binding.shareButton.setOnClickListener {
            requestPermission()
        }

        pieChartRecyclerView()

        divideDataFromFirebase()

        moveCalendarByWeek()

        return binding.root
    }

    private fun divideDataFromFirebase() {
        viewModel.list.observe(viewLifecycleOwner) {
            val dateNow: LocalDateTime = LocalDateTime.now() //오늘
            val monDay: LocalDateTime = dateNow.with(DayOfWeek.MONDAY)//해당 주차의 월
            val tuesDay: LocalDateTime = dateNow.with(DayOfWeek.TUESDAY)//해당 주차의 화
            val wednesDay: LocalDateTime = dateNow.with(DayOfWeek.WEDNESDAY)//해당 주차의 수
            val thursDay: LocalDateTime = dateNow.with(DayOfWeek.THURSDAY)//해당 주차의 목
            val friDay: LocalDateTime = dateNow.with(DayOfWeek.FRIDAY)//해당 주차의 금
            val saturDay: LocalDateTime = dateNow.with(DayOfWeek.SATURDAY)//해당 주차의 토
            val sunDay: LocalDateTime = dateNow.with(DayOfWeek.SUNDAY)//해당 주차의 일

            val textformatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

            var monDayFormat: String = monDay.format(textformatter)
            var tuseDayFormat: String = tuesDay.format(textformatter)
            var wednesDayFormat: String = wednesDay.format(textformatter)
            var thursDayFormat: String = thursDay.format(textformatter)
            var friDayFormat: String = friDay.format(textformatter)
            var saturDayFormat: String = saturDay.format(textformatter)
            var sunDayFormat: String = sunDay.format(textformatter)
            //일주일 간 데이터
            var mondays: Float = 0f
            var tuesdays: Float = 0f
            var wednesdays: Float = 0f
            var thursdays: Float = 0f
            var fridays: Float = 0f
            var saturdays: Float = 0f
            var sundays: Float = 0f

            var mondaySum: Float = 0f
            var tuesdaySum: Float = 0f
            var wednesdaySum: Float = 0f
            var thursdaySum: Float = 0f
            var fridaySum: Float = 0f
            var saturdaySum: Float = 0f
            var sundaySum: Float = 0f

            var totalSum: Float = 0f

            //지난주 데이터
            val dateNow2: LocalDateTime = LocalDateTime.now() //오늘
            val monDay2: LocalDateTime = dateNow2.with(DayOfWeek.MONDAY).minusWeeks(1)//해당 주차의 월
            val tuesDay2: LocalDateTime = dateNow2.with(DayOfWeek.TUESDAY).minusWeeks(1)//해당 주차의 화
            val wednesDay2: LocalDateTime =
                dateNow2.with(DayOfWeek.WEDNESDAY).minusWeeks(1)//해당 주차의 수
            val thursDay2: LocalDateTime = dateNow2.with(DayOfWeek.THURSDAY).minusWeeks(1)//해당 주차의 목
            val friDay2: LocalDateTime = dateNow2.with(DayOfWeek.FRIDAY).minusWeeks(1)//해당 주차의 금
            val saturDay2: LocalDateTime = dateNow2.with(DayOfWeek.SATURDAY).minusWeeks(1)//해당 주차의 토
            val sunDay2: LocalDateTime = dateNow2.with(DayOfWeek.SUNDAY).minusWeeks(1)//해당 주차의 일
            Log.d("totalSum222", monDay2.toString())
            Log.d("totalSum222", tuesDay2.toString())
            Log.d("totalSum222", wednesDay2.toString())

            //일주일 간 데이터
            var mondaysValue2: Float = 0f
            var tuesdaysValue2: Float = 0f
            var wednesdaysValue2: Float = 0f
            var thursdaysValue2: Float = 0f
            var fridaysValue2: Float = 0f
            var saturdaysValue2: Float = 0f
            var sundaysValue2: Float = 0f

            var mondaySum2: Float = 0f
            var tuesdaySum2: Float = 0f
            var wednesdaySum2: Float = 0f
            var thursdaySum2: Float = 0f
            var fridaySum2: Float = 0f
            var saturdaySum2: Float = 0f
            var sundaySum2: Float = 0f

            var totalSum2: Float = 0f

            var its: Int = 0
//            var subject: Subject? = null
            Log.d("asdgsdfg", its.toString())
            it.forEachIndexed { index, subject ->
                its = it.size
                val calen: Calendar = Calendar.getInstance()
//                val day = calen.get(Calendar.DATE).toString()
//                val day1 = (calen.get(Calendar.DATE)+1).toString()
                //서버에서 가져온 요일
                val dateFormat: DateFormat = SimpleDateFormat("yyyy.MM.dd")

                calen.add(Calendar.DATE, 1)
                val serverDateFormat: String = dateFormat.format(subject.timestamp)
                val serverDateFormatPlus1: String =
                    dateFormat.format(subject.timestamp?.day?.minus(1.toLong()))
//                val serverDateFormatPlus2 = serverDateFormat.format(calen.time)
//                    .format(calen.get(Calendar.DATE)+(1)).toString()

                Log.d("calendar_day1_ss", serverDateFormat.toString())
                Log.d("calendar_day1_sssPlus", serverDateFormatPlus1.toString())


                for (it in 0..its) {
                    if (monDayFormat == serverDateFormat) {
                        mondays = subject.studytimeCopy.toFloat()
                        mondaySum = mondaySum + mondays
                        break
                    } else if (monDay2.format(textformatter) == serverDateFormat) {
                        mondaysValue2 = subject.studytimeCopy.toFloat()
                        mondaySum2 = mondaySum2 + mondaysValue2
                        break
                    }
                }
                for (it in 0..its) {
                    if (tuseDayFormat == serverDateFormat) {
                        tuesdays = subject.studytimeCopy.toFloat()
                        tuesdaySum = tuesdaySum + tuesdays
                        break
                    } else if (tuesDay2.format(textformatter) == serverDateFormat) {
                        tuesdaysValue2 = subject.studytimeCopy.toFloat()
                        tuesdaySum2 = tuesdaySum2 + tuesdaysValue2
                        break
                    }
                }
                for (it in 0..its) {
                    if (wednesDayFormat == serverDateFormat) {
                        wednesdays = subject.studytimeCopy.toFloat()
                        Log.d("요일", wednesdays.toString() + " 수")//21
                        Log.d("요일", subject.name.toString() + " : name")
                        wednesdaySum = wednesdaySum + wednesdays
                        break
                    } else if (wednesDay2.format(textformatter) == serverDateFormat) {
                        wednesdaysValue2 = subject.studytimeCopy.toFloat()
                        wednesdaySum2 = wednesdaySum2 + wednesdaysValue2
                        break
                    }
                }
                for (it in 0..its) {
                    if (thursDayFormat == serverDateFormat) {
                        thursdays = subject.studytimeCopy.toFloat()
                        Log.d("요일", thursdays.toString() + " 목")//22
                        thursdaySum = thursdaySum + thursdays
                        break
                    } else if (thursDay2.format(textformatter) == serverDateFormat) {
                        thursdaysValue2 = subject.studytimeCopy.toFloat()
                        thursdaySum2 = thursdaySum2 + thursdaysValue2
                        break
                    }
                }
                for (it in 0..its) {
                    if (friDayFormat == serverDateFormat) {
                        fridays = subject.studytimeCopy.toFloat()
                        Log.d("요일", fridays.toString() + " 금")//23
                        fridaySum = fridaySum + fridays
                        break
                    } else if (friDay2.format(textformatter) == serverDateFormat) {
                        fridaysValue2 = subject.studytimeCopy.toFloat()
                        fridaySum2 = fridaySum2 + fridaysValue2
                        break
                    }
                }
                for (it in 0..its) {
                    if (saturDayFormat == serverDateFormat) {
                        saturdays = subject.studytimeCopy.toFloat()
                        Log.d("요일", saturdays.toString() + " 토")//24
                        Log.d("요일", subject.name.toString() + " : name")
                        saturdaySum = saturdaySum + saturdays
                        break
                    } else if (saturDay2.format(textformatter) == serverDateFormat) {
                        saturdaysValue2 = subject.studytimeCopy.toFloat()
                        saturdaySum2 = saturdaySum2 + saturdaysValue2
                        break
                    }
                }
                for (it in 0..its) {
                    if (sunDayFormat == serverDateFormat) {
                        sundays = subject.studytimeCopy.toFloat()
                        Log.d("요일2", sundays.toString() + " --일--")//25
                        Log.d("요일2", subject.name.toString() + " : name")
                        sundaySum = sundaySum + sundays
                        break
                    } else if (sunDay2.format(textformatter) == serverDateFormat) {
                        sundaysValue2 = subject.studytimeCopy.toFloat()
                        sundaySum2 = sundaySum2 + sundaysValue2
                        break
                    }
                }
            }

            //이번 주
            totalSum =
                mondaySum + tuesdaySum + wednesdaySum + thursdaySum + fridaySum + saturdaySum + sundaySum
            //저번 주
            totalSum2 =
                mondaySum2 + tuesdaySum2 + wednesdaySum2 + thursdaySum2 + fridaySum2 + saturdaySum2 + sundaySum2
            Log.d("totalSum", totalSum.toString() + " 총합") //25
            Log.d("totalSum2", totalSum2.toString() + " 총합") //25

            binding.weeklyTotalTime.text =
                "${(totalSum.toInt()) / 60}시간 ${(totalSum.toInt()) % 60}분"
            //지난주와 비교값
            var compareSum: Int = 0
            if (totalSum > totalSum2) {
                val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                lp.setMargins(20, 15, 0, 0)
                binding.compareWeekTimeImage.setLayoutParams(lp)
                compareSum = totalSum.toInt() - totalSum2.toInt() //text
                binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_up)
                binding.compareWeekTimeText.text = "${compareSum / 60}시간"
                binding.compareWeekTimeText.setTextColor(Color.parseColor("#F95849"))

            } else if (totalSum < totalSum2) {
                val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                lp.setMargins(20, 15, 0, 0)
                binding.compareWeekTimeImage.setLayoutParams(lp)
                compareSum = totalSum2.toInt() - totalSum.toInt() //text
                binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_down)
                binding.compareWeekTimeText.text = "${compareSum / 60}시간"
                binding.compareWeekTimeText.setTextColor(Color.parseColor("#0F8CFF"))
            } else {
                binding.compareWeekTimeImage.setImageResource(R.drawable.ic_linezero)
                val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                lp.setMargins(20, 25, 0, 0)
                binding.compareWeekTimeImage.setLayoutParams(lp)
                binding.compareWeekTimeText.text  = "0시간"
                binding.compareWeekTimeText.setTextColor(Color.parseColor("#80000000"))
            }
            //평균값 설정
            binding.weekBarChart.axisRight.removeAllLimitLines() //// 라인이 겹치지 않도록 모든 제한 라인을 재설정한다.
            var lineDefault = LimitLine(((totalSum / 60) / 7), "평균").apply {
                lineColor = Color.BLACK
                lineWidth = 1f
                textColor = Color.BLACK
                textSize = 12f
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                enableDashedLine(5f, 5f, 15f)
            }
            binding.weekBarChart.axisRight.addLimitLine(lineDefault)

            setData(mondaySum, tuesdaySum, wednesdaySum, thursdaySum, fridaySum, saturdaySum, sundaySum, totalSum)
            Log.d("confirmdata3", mondaySum.toString())
        }
    }

    private fun moveCalendarByWeek() {
        val mondayWeek: LocalDateTime = LocalDateTime.now().with(DayOfWeek.MONDAY)//해당 주차의 월요일
        val sundayWeek: LocalDateTime = LocalDateTime.now().with(DayOfWeek.SUNDAY)

        val firstDayOfWeek: LocalDate = LocalDate.now() //현재 날짜
        val fieek: Int = firstDayOfWeek.get(ChronoField.ALIGNED_WEEK_OF_MONTH) //현재 날짜의 주일
        val firstDay: LocalDate = firstDayOfWeek.withDayOfMonth(1) //매월 첫 날
        val endDay: LocalDate =
            firstDayOfWeek.withDayOfMonth(firstDayOfWeek.lengthOfMonth())// 매월 마지막 날
//                val textformatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        val weektextformatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월") //7월
//        val weekend: Int = LocalDate.now().get(ChronoField.ALIGNED_WEEK_OF_MONTH)//몇째주

        val textformatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        binding.calendarMonday.text = mondayWeek.format(textformatter)
        binding.calendarSunday.text = sundayWeek.format(textformatter)
        Log.d("weekCount", count.toString())
        Log.d("weekCount++", weekCount.toString())
        count = 0
        weekCount = 0
//        binding.compareWeekTimeImage.setVisibility(true)
//        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_up)
//        binding.compareWeekTimeText.text = "${compareSum / 60}시간"
//        binding.compareWeekTimeText.setTextColor(Color.parseColor("#F95849"))

        //오른쪽 버튼
        binding.calRightBtn.setOnClickListener {
            count++
            Log.d("weekPlusCOUNT", count.toString())
            if (count == 1) {
                binding.calRightBtn.isEnabled = false
            } else {
                //2021.07.19 ~ 2021.07.25 표시
                val mondayValue: LocalDateTime = mondayWeek.plusWeeks(count.toLong())
                binding.calendarMonday.text = mondayValue.format(textformatter).toString()
                val sundayValue: LocalDateTime = sundayWeek.plusWeeks(count.toLong())
                binding.calendarSunday.text = sundayValue.format(textformatter).toString()

                if (count == 0) {
                    binding.titleWeek.text = "이번"
                    binding.titleWeekNext.text = "주에"
                } else if (count == -1) {
                    binding.calRightBtn.isEnabled = true
                    binding.titleWeek.text = "지난"
                    binding.titleWeekNext.text = "주에"
                } else {  //ALIGNED_WEEK_OF_MONTH : 그 달의 n 번째 주
                    binding.titleWeekNext.text =
                        mondayValue.get(ChronoField.ALIGNED_WEEK_OF_MONTH)
                            .toString() + "째 주에"
                    binding.titleWeek.text =
                        mondayValue.format(weektextformatter).toString()
                }
            }
            weekCount++
            if (weekCount <= 0) {
                viewModel.list.observe(viewLifecycleOwner) {
                    val dateNow: LocalDateTime = LocalDateTime.now() //오늘
                    val monDay: LocalDateTime =
                        dateNow.with(DayOfWeek.MONDAY).plusWeeks(weekCount.toLong())//해당 주차의 월
                    val tuesDay: LocalDateTime =
                        dateNow.with(DayOfWeek.TUESDAY).plusWeeks(weekCount.toLong())//해당 주차의 화
                    val wednesDay: LocalDateTime =
                        dateNow.with(DayOfWeek.WEDNESDAY).plusWeeks(weekCount.toLong())//해당 주차의 수
                    val thursDay: LocalDateTime =
                        dateNow.with(DayOfWeek.THURSDAY).plusWeeks(weekCount.toLong())//해당 주차의 목
                    val friDay: LocalDateTime =
                        dateNow.with(DayOfWeek.FRIDAY).plusWeeks(weekCount.toLong())//해당 주차의 금
                    val saturDay: LocalDateTime =
                        dateNow.with(DayOfWeek.SATURDAY).plusWeeks(weekCount.toLong())//해당 주차의 토
                    val sunDay: LocalDateTime =
                        dateNow.with(DayOfWeek.SUNDAY).plusWeeks(weekCount.toLong())//해당 주차의 일
                    Log.d("weekCount++", weekCount.toString())
                    Log.d("weekPlusmonDay+++", monDay.toString())
                    Log.d("weekPlusmonDay", sunDay.toString())

                    val textformatter: DateTimeFormatter =
                        DateTimeFormatter.ofPattern("yyyy.MM.dd")

                    var monDayFormat: String = monDay.format(textformatter)
                    var tuseDayFormat: String = tuesDay.format(textformatter)
                    var wednesDayFormat: String = wednesDay.format(textformatter)
                    var thursDayFormat: String = thursDay.format(textformatter)
                    var friDayFormat: String = friDay.format(textformatter)
                    var saturDayFormat: String = saturDay.format(textformatter)
                    var sunDayFormat: String = sunDay.format(textformatter)
                    //일주일 간 데이터
                    var mondays: Float = 0f
                    var tuesdays: Float = 0f
                    var wednesdays: Float = 0f
                    var thursdays: Float = 0f
                    var fridays: Float = 0f
                    var saturdays: Float = 0f
                    var sundays: Float = 0f

                    var mondaySum: Float = 0f
                    var tuesdaySum: Float = 0f
                    var wednesdaySum: Float = 0f
                    var thursdaySum: Float = 0f
                    var fridaySum: Float = 0f
                    var saturdaySum: Float = 0f
                    var sundaySum: Float = 0f

                    var totalSum: Float = 0f

                    //지난주 데이터
                    val dateNow2: LocalDateTime = LocalDateTime.now() //오늘
                    val monDay2: LocalDateTime = dateNow2.with(DayOfWeek.MONDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 월
                    val tuesDay2: LocalDateTime = dateNow2.with(DayOfWeek.TUESDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 화
                    val wednesDay2: LocalDateTime = dateNow2.with(DayOfWeek.WEDNESDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 수
                    val thursDay2: LocalDateTime = dateNow2.with(DayOfWeek.THURSDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 목
                    val friDay2: LocalDateTime = dateNow2.with(DayOfWeek.FRIDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 금
                    val saturDay2: LocalDateTime = dateNow2.with(DayOfWeek.SATURDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 토
                    val sunDay2: LocalDateTime = dateNow2.with(DayOfWeek.SUNDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 일

                    var mondaysValue2: Float = 0f
                    var tuesdaysValue2: Float = 0f
                    var wednesdaysValue2: Float = 0f
                    var thursdaysValue2: Float = 0f
                    var fridaysValue2: Float = 0f
                    var saturdaysValue2: Float = 0f
                    var sundaysValue2: Float = 0f

                    var mondaySum2: Float = 0f
                    var tuesdaySum2: Float = 0f
                    var wednesdaySum2: Float = 0f
                    var thursdaySum2: Float = 0f
                    var fridaySum2: Float = 0f
                    var saturdaySum2: Float = 0f
                    var sundaySum2: Float = 0f

                    var totalSum2: Float = 0f


                    var its: Int = 0
//            var subject: Subject? = null
                    Log.d("asdgsdfg", its.toString())
                    it.forEachIndexed { index, subject ->
                        its = it.size
                        //서버에서 가져온 요일
                        val dateFormat: DateFormat = SimpleDateFormat("yyyy.MM.dd")
                        val serverDateFormat: String = dateFormat.format(subject.timestamp)

                        for (it in 0..its) {
                            if (monDayFormat == serverDateFormat) {
                                mondays = subject.studytimeCopy.toFloat()
                                mondaySum = mondaySum + mondays
                                break
                            }else if (monDay2.format(textformatter) == serverDateFormat) {
                                mondaysValue2 = subject.studytimeCopy.toFloat()
                                mondaySum2 = mondaySum2 + mondaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (tuseDayFormat == serverDateFormat) {
                                tuesdays = subject.studytimeCopy.toFloat()
                                tuesdaySum = tuesdaySum + tuesdays
                                break
                            }else if (tuesDay2.format(textformatter) == serverDateFormat) {
                                tuesdaysValue2 = subject.studytimeCopy.toFloat()
                                tuesdaySum2 = tuesdaySum2 + tuesdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (wednesDayFormat == serverDateFormat) {
                                wednesdays = subject.studytimeCopy.toFloat()
                                Log.d("요일", wednesdays.toString() + " 수")//21
                                Log.d("요일", subject.name.toString() + " : name")
                                wednesdaySum = wednesdaySum + wednesdays
                                break
                            }else if (wednesDay2.format(textformatter) == serverDateFormat) {
                                wednesdaysValue2 = subject.studytimeCopy.toFloat()
                                wednesdaySum2 = wednesdaySum2 + wednesdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (thursDayFormat == serverDateFormat) {
                                thursdays = subject.studytimeCopy.toFloat()
                                Log.d("요일", thursdays.toString() + " 목")//22
                                thursdaySum = thursdaySum + thursdays
                                break
                            }else if (thursDay2.format(textformatter) == serverDateFormat) {
                                thursdaysValue2 = subject.studytimeCopy.toFloat()
                                thursdaySum2 = thursdaySum2 + thursdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (friDayFormat == serverDateFormat) {
                                fridays = subject.studytimeCopy.toFloat()
                                Log.d("요일", fridays.toString() + " 금")//23
                                fridaySum = fridaySum + fridays
                                break
                            }else if(friDay2.format(textformatter) == serverDateFormat) {
                                fridaysValue2 = subject.studytimeCopy.toFloat()
                                fridaySum2 = fridaySum2 + fridaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (saturDayFormat == serverDateFormat) {
                                saturdays = subject.studytimeCopy.toFloat()
                                Log.d("요일", saturdays.toString() + " 토")//24
                                Log.d("요일", subject.name.toString() + " : name")
                                saturdaySum = saturdaySum + saturdays
                                break
                            }else if(saturDay2.format(textformatter) == serverDateFormat) {
                                saturdaysValue2 = subject.studytimeCopy.toFloat()
                                saturdaySum2 = saturdaySum2 + saturdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (sunDayFormat == serverDateFormat) {
                                sundays = subject.studytimeCopy.toFloat()
                                Log.d("요일", sundays.toString() + " --일--")//25
                                Log.d("요일", subject.name.toString() + " : name")
                                sundaySum = sundaySum + sundays
                                break
                            }else if(sunDay2.format(textformatter) == serverDateFormat) {
                                sundaysValue2 = subject.studytimeCopy.toFloat()
                                sundaySum2 = sundaySum2 + sundaysValue2
                                break
                            }
                        }
                    }
                    totalSum =
                        mondaySum + tuesdaySum + wednesdaySum + thursdaySum + fridaySum + saturdaySum + sundaySum
                    //저번 주
                    totalSum2 =
                        mondaySum2 + tuesdaySum2 + wednesdaySum2 + thursdaySum2 + fridaySum2 + saturdaySum2 + sundaySum2

                    Log.d("totalSum", totalSum.toString() + " 총합") //25
                    Log.d("totalSum2", totalSum2.toString() + " 총합") //25

                    Log.d("totalSum", totalSum.toString() + " 총합") //25
                    binding.weeklyTotalTime.text = "${(totalSum.toInt()) / 60}시간 ${(totalSum.toInt()) % 60}분"
                    //지난주와 비교값
                    var compareSum: Int = 0
                    if (totalSum > totalSum2) {
                        compareSum = totalSum.toInt() - totalSum2.toInt() //text
                        binding.compareWeekTimeImage.setVisibility(true)
                        val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                        lp.setMargins(20, 15, 0, 0)
                        binding.compareWeekTimeImage.setLayoutParams(lp)
                        binding.compareWeekTimeImage.setVisibility(true)
                        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_up)
                        binding.compareWeekTimeText.text = "${compareSum / 60}시간"
                        binding.compareWeekTimeText.setTextColor(Color.parseColor("#F95849"))

                    } else if (totalSum < totalSum2) {
                        compareSum = totalSum2.toInt() - totalSum.toInt() //text
                        binding.compareWeekTimeImage.setVisibility(true)
                        val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                        lp.setMargins(20, 15, 0, 0)
                        binding.compareWeekTimeImage.setLayoutParams(lp)
                        binding.compareWeekTimeImage.setVisibility(true)
                        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_down)
                        binding.compareWeekTimeText.text = "${compareSum / 60}시간"
                        binding.compareWeekTimeText.setTextColor(Color.parseColor("#0F8CFF"))
                    } else {
                        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_linezero)
                        val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                        lp.setMargins(20, 25, 0, 0)
                        binding.compareWeekTimeImage.setLayoutParams(lp)
                        binding.compareWeekTimeText.text  = "0시간"
                        binding.compareWeekTimeText.setTextColor(Color.parseColor("#80000000"))
                    }

                    //평균값 설정
                    binding.weekBarChart.axisRight.removeAllLimitLines() //// 라인이 겹치지 않도록 모든 제한 라인을 재설정한다.
                    var lineRight = LimitLine(((totalSum / 60) / 7), "평균").apply {
                        lineColor = Color.BLACK
                        lineWidth = 1f
                        textColor = Color.BLACK
                        textSize = 12f
                        labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                        enableDashedLine(5f, 5f, 15f)
                    }
                    binding.weekBarChart.axisRight.addLimitLine(lineRight)

                    setData(
                        mondaySum,
                        tuesdaySum,
                        wednesdaySum,
                        thursdaySum,
                        fridaySum,
                        saturdaySum,
                        sundaySum,
                        totalSum
                    )
                    Log.d("confirmdata3", mondaySum.toString())
                }
            }
        }

        //왼쪽 버튼
        binding.calLeftBtn.setOnClickListener {
            count--
            Log.d("weekPlusCOUNT", count.toString())
            //2021.07.19 ~ 2021.07.25 표시
            val sundayValue: LocalDateTime = sundayWeek.plusWeeks(count.toLong())
            binding.calendarSunday.text = sundayValue.format(textformatter).toString()
            val mondayValue: LocalDateTime = mondayWeek.plusWeeks(count.toLong())
            binding.calendarMonday.text = mondayValue.format(textformatter).toString()

            if (count == 0) {
                binding.titleWeek.text = "이번"
                binding.titleWeekNext.text = "주에"
            } else if (count == -1) {
                binding.titleWeek.text = "지난"
                binding.titleWeekNext.text = "주에"
                binding.calRightBtn.isEnabled = true
            } else {
                binding.titleWeekNext.text =
                    mondayValue.get(ChronoField.ALIGNED_WEEK_OF_MONTH).toString() + "째 주에"
                binding.titleWeek.text = mondayValue.format(weektextformatter).toString()
            }
            weekCount--
//            var weekMinus : Int = 1
            if (weekCount <= 0) {
                viewModel.list.observe(viewLifecycleOwner) {
                    val dateNow: LocalDateTime = LocalDateTime.now() //오늘
                    val monDay: LocalDateTime =
                        dateNow.with(DayOfWeek.MONDAY).plusWeeks(weekCount.toLong())//해당 주차의 월
                    val tuesDay: LocalDateTime =
                        dateNow.with(DayOfWeek.TUESDAY).plusWeeks(weekCount.toLong())//해당 주차의 화
                    val wednesDay: LocalDateTime =
                        dateNow.with(DayOfWeek.WEDNESDAY).plusWeeks(weekCount.toLong())//해당 주차의 수
                    val thursDay: LocalDateTime =
                        dateNow.with(DayOfWeek.THURSDAY).plusWeeks(weekCount.toLong())//해당 주차의 목
                    val friDay: LocalDateTime =
                        dateNow.with(DayOfWeek.FRIDAY).plusWeeks(weekCount.toLong())//해당 주차의 금
                    val saturDay: LocalDateTime =
                        dateNow.with(DayOfWeek.SATURDAY).plusWeeks(weekCount.toLong())//해당 주차의 토
                    val sunDay: LocalDateTime =
                        dateNow.with(DayOfWeek.SUNDAY).plusWeeks(weekCount.toLong())//해당 주차의 일
                    Log.d("weekCount", weekCount.toString())
                    Log.d("weekPlusmonDay", monDay.toString())
                    Log.d("weekPlusmonDay", sunDay.toString())

                    val textformatter: DateTimeFormatter =
                        DateTimeFormatter.ofPattern("yyyy.MM.dd")

                    var monDayFormat: String = monDay.format(textformatter)
                    var tuseDayFormat: String = tuesDay.format(textformatter)
                    var wednesDayFormat: String = wednesDay.format(textformatter)
                    var thursDayFormat: String = thursDay.format(textformatter)
                    var friDayFormat: String = friDay.format(textformatter)
                    var saturDayFormat: String = saturDay.format(textformatter)
                    var sunDayFormat: String = sunDay.format(textformatter)
                    //일주일 간 데이터
                    var mondays: Float = 0f
                    var tuesdays: Float = 0f
                    var wednesdays: Float = 0f
                    var thursdays: Float = 0f
                    var fridays: Float = 0f
                    var saturdays: Float = 0f
                    var sundays: Float = 0f

                    var mondaySum: Float = 0f
                    var tuesdaySum: Float = 0f
                    var wednesdaySum: Float = 0f
                    var thursdaySum: Float = 0f
                    var fridaySum: Float = 0f
                    var saturdaySum: Float = 0f
                    var sundaySum: Float = 0f

                    var totalSum: Float = 0f

                    //지난주 데이터
                    val dateNow2: LocalDateTime = LocalDateTime.now() //오늘
                    val monDay2: LocalDateTime = dateNow2.with(DayOfWeek.MONDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 월
                    val tuesDay2: LocalDateTime = dateNow2.with(DayOfWeek.TUESDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 화
                    val wednesDay2: LocalDateTime = dateNow2.with(DayOfWeek.WEDNESDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 수
                    val thursDay2: LocalDateTime = dateNow2.with(DayOfWeek.THURSDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 목
                    val friDay2: LocalDateTime = dateNow2.with(DayOfWeek.FRIDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 금
                    val saturDay2: LocalDateTime = dateNow2.with(DayOfWeek.SATURDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 토
                    val sunDay2: LocalDateTime = dateNow2.with(DayOfWeek.SUNDAY).minusWeeks(1)
                        .plusWeeks(weekCount.toLong())//해당 주차의 일

                    var mondaysValue2: Float = 0f
                    var tuesdaysValue2: Float = 0f
                    var wednesdaysValue2: Float = 0f
                    var thursdaysValue2: Float = 0f
                    var fridaysValue2: Float = 0f
                    var saturdaysValue2: Float = 0f
                    var sundaysValue2: Float = 0f

                    var mondaySum2: Float = 0f
                    var tuesdaySum2: Float = 0f
                    var wednesdaySum2: Float = 0f
                    var thursdaySum2: Float = 0f
                    var fridaySum2: Float = 0f
                    var saturdaySum2: Float = 0f
                    var sundaySum2: Float = 0f

                    var totalSum2: Float = 0f

                    var its: Int = 0
//            var subject: Subject? = null
                    Log.d("asdgsdfg", its.toString())
                    it.forEachIndexed { index, subject ->
                        its = it.size
                        //서버에서 가져온 요일
                        val dateFormat: DateFormat = SimpleDateFormat("yyyy.MM.dd")
                        val serverDateFormat: String = dateFormat.format(subject.timestamp)

                        for (it in 0..its) {
                            if (monDayFormat == serverDateFormat) {
                                mondays = subject.studytimeCopy.toFloat()
                                mondaySum = mondaySum + mondays
                                break
                            }else if (monDay2.format(textformatter) == serverDateFormat) {
                                mondaysValue2 = subject.studytimeCopy.toFloat()
                                mondaySum2 = mondaySum2 + mondaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (tuseDayFormat == serverDateFormat) {
                                tuesdays = subject.studytimeCopy.toFloat()
                                tuesdaySum = tuesdaySum + tuesdays
                                break
                            }else if (tuesDay2.format(textformatter) == serverDateFormat) {
                                tuesdaysValue2 = subject.studytimeCopy.toFloat()
                                tuesdaySum2 = tuesdaySum2 + tuesdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (wednesDayFormat == serverDateFormat) {
                                wednesdays = subject.studytimeCopy.toFloat()
                                Log.d("요일", wednesdays.toString() + " 수")//21
                                Log.d("요일", subject.name.toString() + " : name")
                                wednesdaySum = wednesdaySum + wednesdays
                                break
                            }else if (wednesDay2.format(textformatter) == serverDateFormat) {
                                wednesdaysValue2 = subject.studytimeCopy.toFloat()
                                wednesdaySum2 = wednesdaySum2 + wednesdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (thursDayFormat == serverDateFormat) {
                                thursdays = subject.studytimeCopy.toFloat()
                                Log.d("요일", thursdays.toString() + " 목")//22
                                thursdaySum = thursdaySum + thursdays
                                break
                            }else if (thursDay2.format(textformatter) == serverDateFormat) {
                                thursdaysValue2 = subject.studytimeCopy.toFloat()
                                thursdaySum2 = thursdaySum2 + thursdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (friDayFormat == serverDateFormat) {
                                fridays = subject.studytimeCopy.toFloat()
                                Log.d("요일", fridays.toString() + " 금")//23
                                fridaySum = fridaySum + fridays
                                break
                            }else if (friDay2.format(textformatter) == serverDateFormat) {
                                fridaysValue2 = subject.studytimeCopy.toFloat()
                                fridaySum2 = fridaySum2 + fridaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (saturDayFormat == serverDateFormat) {
                                saturdays = subject.studytimeCopy.toFloat()
                                Log.d("요일", saturdays.toString() + " 토")//24
                                Log.d("요일", subject.name.toString() + " : name")
                                saturdaySum = saturdaySum + saturdays
                                break
                            }else if (saturDay2.format(textformatter) == serverDateFormat) {
                                saturdaysValue2 = subject.studytimeCopy.toFloat()
                                saturdaySum2 = saturdaySum2 + saturdaysValue2
                                break
                            }
                        }
                        for (it in 0..its) {
                            if (sunDayFormat == serverDateFormat) {
                                sundays = subject.studytimeCopy.toFloat()
                                Log.d("요일", sundays.toString() + " --일--")//25
                                Log.d("요일", subject.name.toString() + " : name")
                                sundaySum = sundaySum + sundays
                                break
                            }else if (sunDay2.format(textformatter) == serverDateFormat) {
                                sundaysValue2 = subject.studytimeCopy.toFloat()
                                sundaySum2 = sundaySum2 + sundaysValue2
                                break
                            }
                        }

                    }
                    totalSum =
                        mondaySum + tuesdaySum + wednesdaySum + thursdaySum + fridaySum + saturdaySum + sundaySum
                    //저번 주
                    totalSum2 =
                        mondaySum2 + tuesdaySum2 + wednesdaySum2 + thursdaySum2 + fridaySum2 + saturdaySum2 + sundaySum2

                    //평균값 설정
                    binding.weekBarChart.axisRight.removeAllLimitLines() //// 라인이 겹치지 않도록 모든 제한 라인을 재설정한다.
                    var lineLeft = LimitLine(((totalSum / 60) / 7), "평균").apply {
                        lineColor = Color.BLACK
                        lineWidth = 1f
                        textColor = Color.BLACK
                        textSize = 12f
                        labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                        enableDashedLine(5f, 5f, 15f)
                    }
                    binding.weekBarChart.axisRight.addLimitLine(lineLeft)


                    binding.weeklyTotalTime.text =
                        "${(totalSum.toInt()) / 60}시간 ${(totalSum.toInt()) % 60}분"
                    //지난주와 비교값
                    var compareSum: Int = 0
                    if (totalSum > totalSum2) {
                        compareSum = totalSum.toInt() - totalSum2.toInt() //text
                        binding.compareWeekTimeImage.setVisibility(true)
                        val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                        lp.setMargins(20, 15, 0, 0)
                        binding.compareWeekTimeImage.setLayoutParams(lp)
                        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_up)
                        binding.compareWeekTimeText.text = "${compareSum / 60}시간"
                        binding.compareWeekTimeText.setTextColor(Color.parseColor("#F95849"))

                    } else if (totalSum < totalSum2) {
                        compareSum = totalSum2.toInt() - totalSum.toInt() //text
                        binding.compareWeekTimeImage.setVisibility(true)
                        val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                        lp.setMargins(20, 15, 0, 0)
                        binding.compareWeekTimeImage.setLayoutParams(lp)
                        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_polygon_down)
                        binding.compareWeekTimeText.text = "${compareSum / 60}시간"
                        binding.compareWeekTimeText.setTextColor(Color.parseColor("#0F8CFF"))
                    } else {
                        binding.compareWeekTimeImage.setImageResource(R.drawable.ic_linezero)
                        val lp = LinearLayout.LayoutParams(binding.compareWeekTimeImage.getLayoutParams())
                        lp.setMargins(20, 25, 0, 0)
                        binding.compareWeekTimeImage.setLayoutParams(lp)
                        binding.compareWeekTimeText.text  = "0시간"
                        binding.compareWeekTimeText.setTextColor(Color.parseColor("#80000000"))
                    }
                    Log.d("totalSumCompare", compareSum.toString() + "비교") //25


                    setData(
                        mondaySum,
                        tuesdaySum,
                        wednesdaySum,
                        thursdaySum,
                        fridaySum,
                        saturdaySum,
                        sundaySum,
                        totalSum
                    )
                    Log.d("confirmdata3", mondaySum.toString())
                }
            }
        }
    }

    private fun initChart() {
//        customMarkerView.chartView = chart
        with(binding.weekBarChart) {
//            marker = customMarkerView
            description.isEnabled = false
            legend.isEnabled = false
            isDoubleTapToZoomEnabled = false

            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(false)
            //둥근 모서리 색상
            val barChartRender = CustomBarChartRender(this, animator, viewPortHandler).apply {
//                setRadius(20)
            }
            renderer = barChartRender
        }
    }

    fun setData(monday: Float, tuseday: Float, wednesday: Float, thursday: Float, friday: Float, saturday: Float, sunday: Float, totalSum: Float) {
        val values = mutableListOf<BarEntry>()
        values.add(BarEntry(0f, (monday / 60)))
        values.add(BarEntry(1f, (tuseday / 60)))
        values.add(BarEntry(2f, (wednesday / 60)))
        values.add(BarEntry(3f, (thursday / 60)))
        values.add(BarEntry(4f, (friday / 60)))
        values.add(BarEntry(5f, (saturday / 60)))
        values.add(BarEntry(6f, (sunday / 60)))

        //막대 그래프 색상 추가
        val barDataSet = BarDataSet(values, "").apply {
            setDrawValues(false)

            val colors = ArrayList<Int>()
            colors.add(Color.parseColor("#FF339BFF"))
            setColors(colors)
            highLightAlpha = 0
        }

        //막대 그래프 너비 설정
        val dataSets = mutableListOf(barDataSet)
        val data = BarData(dataSets as List<IBarDataSet>?).apply {
//            setValueTextSize(30F)
            barWidth = 0.2F
        }
//        with(binding.weekBarChart) {
//            axisRight.addLimitLine(ll)
////            axisRight.removeLimitLine(ll)
//        }


//        binding.weekBarChart.animateY(0)
        with(binding.weekBarChart) {
            animateY(1000)
            //x축을 나타냄
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false) //축 점선
                textColor = ContentColor
//                gridColor = transparentBlackColor

                //월 ~ 일 (x축 label 이름)
                val xAxisLabels = listOf("월", "화", "수", "목", "금", "토", "일")
                valueFormatter = IndexAxisValueFormatter(xAxisLabels)

            }

            //차트 왼쪽 축, Y방향 ( 수치 최소값,최대값 )
            axisRight.apply {
//                addLimitLine(ll)
                textColor = ContentColor
                setDrawAxisLine(false) //격자
                gridLineWidth = 1F
                gridColor = transparentBlackColor
                axisLineColor = transparentBlackColor //축의 축선 색상
                enableGridDashedLine(5f, 5f, 5f)

                axisMaximum = 24F //최대값
                granularity = 3F //30단위마다 선을 그리려고 granularity 설정을 해 주었음
                axisMinimum = 0F //최소값
                setLabelCount(4, true) //축 고정간격
                //y축 제목 커스텀
                valueFormatter = object : ValueFormatter() {
                    private val mFormat: DecimalFormat = DecimalFormat("###")
                    override fun getFormattedValue(value: Float): String {
                        return mFormat.format(value) + "시간"
                    }
                }
            }

            //차트 오른쪽 축, Y방향 false처리
            axisLeft.apply {
//                addLimitLine(ll)
                isEnabled = false
                setDrawAxisLine(false) //격자
                gridLineWidth = 1F
                gridColor = ContentColor
                axisLineColor = transparentBlackColor

                axisMaximum = 24F //최대값
                granularity = 3F //30단위마다 선을 그리려고 granularity 설정을 해 주었음
                axisMinimum = 0F //최소값
                setLabelCount(4, true) //축 고정간격
            }

            notifyDataSetChanged()
            this.data = data
            invalidate()
        }
    }

    //주간별 원 차트
    private fun weeklySubjectPieChart() {
        val pieChart: PieChart = binding.weeklyPieChart
        pieChart.setUsePercentValues(true)
        val values = mutableListOf<PieEntry>()
        val colorItems = mutableListOf<Int>()
        viewModel.list.observe(viewLifecycleOwner) {
//            it.forEachIndexed{index, subject ->
//                subjectListValue.add(index,subject)
//            }

            it.forEach {
                values.add(PieEntry(it.studytime.toFloat(), it.name.toString()))
            }
            it.forEachIndexed { index, subject ->
                colorItems.add(index, Color.parseColor(subject.color))
            }

            val pieDataSet = PieDataSet(values, "")
            pieDataSet.colors = colorItems
            pieDataSet.apply {
//            valueTextColor = Color.BLACK
                setDrawValues(false) //차트에 표시되는 값 지우기
                valueTextSize = 16f
            }
            //% : 퍼센트 수치 색상과 사이즈 지정
            val pieData = PieData(pieDataSet)
            pieChart.apply {
                data = pieData
                description.isEnabled = false //해당 그래프 오른쪽 아래 그래프의 이름을 표시한다.
                isRotationEnabled = false //그래프를 회전판처럼 돌릴 수 있다
//            centerText = "this is color" //그래프 한 가운데 들어갈 텍스트
//            setEntryLabelColor(Color.RED) //그래프 아이템의 이름의 색 지정
                isEnabled = false
                legend.isEnabled = false //범례 지우기
                isDrawHoleEnabled = true //중앙의 흰색 테두리 제거
                holeRadius = 50f //흰색을 증앙에 꽉 채우기
                setDrawEntryLabels(false) //차트에 있는 이름 지우
                animateY(1400, Easing.EaseInOutQuad)
                animate()
            }

        }
        Log.d("zxcvzxcvzzz", list.size.toString())
        Log.d("zxcvzxcvzzz", list.toString())
    }

    private fun pieChartRecyclerView() {
        subjectListValue.forEach {
            Log.d("gtguvsdf", "it.color.toString()")
            Log.d("gtguvsdf", it.color.toString())
            Log.d("gtguvsdf", it.studytime.toString())
        }
        binding.recyclerViewWeek.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = weeklySubjectAdapter
        }
    }

    //adapter에 데이터 추가
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.subjectList.observe(viewLifecycleOwner) {
            weeklySubjectAdapter.setData(it)
        }
    }

    private fun requestPermission(): Boolean {
        var permissions = false
        TedPermission.with(context)
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                    permissions = true      //p0=response(응답)
                    val shareIntent = Intent(context, ShareActivity::class.java)
                    startActivity(shareIntent)
//                    finish()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    permissions = false
                }

            })
            .setDeniedMessage("앱을 실행하려면 권한을 허가하셔야합니다.")
            .setPermissions(Manifest.permission.CAMERA)
            .check()
        return permissions
    }
}

