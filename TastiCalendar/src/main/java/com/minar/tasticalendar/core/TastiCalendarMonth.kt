package com.minar.tasticalendar.core

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Range
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.minar.tasticalendar.R
import com.minar.tasticalendar.databinding.TasticalendarMonthBinding
import com.minar.tasticalendar.utilities.getBestContrast
import com.minar.tasticalendar.utilities.getThemeColor
import com.minar.tasticalendar.utilities.showSnackbar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class TastiCalendarMonth(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    // Custom attributes
    private var month = 0
    private var hideWeekDays: Boolean
    private var sundayFirst: Boolean
    private var showSnackBars: Boolean
    private var appearance = 0

    private var dateWithChosenMonth: LocalDate
    private lateinit var cellsList: MutableList<TextView>
    private lateinit var weekDaysList: MutableList<TextView>
    private lateinit var monthTitle: TextView
    private var binding: TasticalendarMonthBinding
    private var eventCount = 0

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.TastiCalendarMonth, 0, 0
        ).apply {
            try {
                month = getInteger(R.styleable.TastiCalendarMonth_month, 0)
                hideWeekDays = getBoolean(R.styleable.TastiCalendarMonth_hideWeekDays, false)
                sundayFirst = getBoolean(R.styleable.TastiCalendarMonth_sundayAsFirstDay, false)
                showSnackBars = getBoolean(R.styleable.TastiCalendarMonth_showInfoSnackBars, true)
                appearance = getInteger(R.styleable.TastiCalendarMonth_appearance, 0)
            } finally {
                recycle()
            }
        }
        binding = TasticalendarMonthBinding.inflate(LayoutInflater.from(context), this, true)
        dateWithChosenMonth = LocalDate.now().withMonth(month + 1).withDayOfMonth(1)
        initMonth()
    }

    // Render the appropriate numbers and hide any useless text view
    private fun renderDays(monthRange: Range<Int>) {
        val min = monthRange.lower
        val max = monthRange.upper

        // Render the month numbers with a leading space for single digit numbers
        for (i in min..max) {
            val dayValue = i - min + 1
            // Manage single digit dates differently
            val dayNumber = if (dayValue <= 9) " $dayValue" else dayValue.toString()
            cellsList[i].text = dayNumber
            cellsList[i].visibility = View.VISIBLE
            // Accessibility related info
            try {
                val correspondingDate =
                    LocalDate.of(dateWithChosenMonth.year, dateWithChosenMonth.month - 1, dayValue)
                val formatter: DateTimeFormatter =
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                cellsList[i].contentDescription = correspondingDate.format(formatter)
            } catch (_: Exception) {
            }
        }
        // Hide unnecessary cells, also resetting the text to avoid false positive when highlighting
        if (min != 0) for (i in 0 until min) {
            cellsList[i].visibility = View.INVISIBLE
            cellsList[i].text = ""
        }
        when (dateWithChosenMonth.month) {
            Month.NOVEMBER, Month.APRIL, Month.JUNE, Month.SEPTEMBER -> {
                for (i in (30 + min) until cellsList.size) {
                    cellsList[i].visibility = View.INVISIBLE
                    cellsList[i].text = ""
                }
            }
            Month.FEBRUARY -> {
                val leapIndex = if (dateWithChosenMonth.isLeapYear) 29 else 28
                for (i in (leapIndex + min) until cellsList.size) {
                    cellsList[i].visibility = View.INVISIBLE
                    cellsList[i].text = ""
                }
            }
            else -> {
                for (i in (31 + min) until cellsList.size) {
                    cellsList[i].visibility = View.INVISIBLE
                    cellsList[i].text = ""
                }
            }
        }
    }

    // Highlight a day in the month using a drawable or a color
    fun highlightDay(
        day: Int,
        color: Int,
        drawable: Drawable? = null,
        makeBold: Boolean = false,
        autoOpacity: Boolean = false,
        autoTextColor: Boolean = false,
        asForeground: Boolean = false,
        snackbarText: String = ""
    ) {
        // Update the global event count
        eventCount += 1
        var currentAlpha = 0
        // The textview will be hidden if the day doesn't exist in the current month
        for (cell in cellsList) {
            if (cell.text.trim() == day.toString()) {
                // Graphical stuff
                if (drawable == null) {
                    cell.setTextColor(color)
                } else {
                    // Use the drawable as background or foreground
                    if (asForeground) {
                        cell.foreground = drawable
                        cell.foregroundTintList = ColorStateList.valueOf(color)
                    }
                    // In case of background, compute the opacity
                    else {
                        if (autoOpacity) {
                            if (cell.background != null) currentAlpha = cell.background.alpha
                        }
                        cell.background = drawable
                        cell.backgroundTintList = ColorStateList.valueOf(color)

                        if (autoOpacity) {
                            if (currentAlpha > 185) cell.background.alpha = 255
                            else cell.background.alpha = currentAlpha + 70
                        } else cell.background.alpha = 255
                        if (autoTextColor) {
                            cell.setTextColor(
                                getBestContrast(
                                    color,
                                    context,
                                    cell.background.alpha,
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) resources.configuration.isNightModeActive else null
                                )
                            )
                        }
                    }
                    // Display a snackbar on tap if the text exists
                    if (snackbarText.isNotBlank()) {
                        cell.setOnClickListener {
                            showSnackbar(snackbarText.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.ROOT
                                ) else it.toString()
                            }, binding.root, 3000)
                        }
                    }
                }
                // The font will change, and monospace doesn't have a bold style
                if (makeBold) {
                    cell.setTypeface(null, Typeface.BOLD)
                }
                break
            }
        }
    }

    // Initialize the month
    private fun initMonth() {
        // Week days
        val weekDayOne = binding.tastiCalendarWeekDayOne
        val weekDayTwo = binding.tastiCalendarWeekDayTwo
        val weekDayThree = binding.tastiCalendarWeekDayThree
        val weekDayFour = binding.tastiCalendarWeekDayFour
        val weekDayFive = binding.tastiCalendarWeekDayFive
        val weekDaySix = binding.tastiCalendarWeekDaySix
        val weekDaySeven = binding.tastiCalendarWeekDaySeven
        weekDaysList = mutableListOf(
            weekDayOne, weekDayTwo, weekDayThree, weekDayFour, weekDayFive, weekDaySix, weekDaySeven
        )

        // Month cells
        val cell1 = binding.tastiCalendarCell1
        val cell2 = binding.tastiCalendarCell2
        val cell3 = binding.tastiCalendarCell3
        val cell4 = binding.tastiCalendarCell4
        val cell5 = binding.tastiCalendarCell5
        val cell6 = binding.tastiCalendarCell6
        val cell7 = binding.tastiCalendarCell7
        val cell8 = binding.tastiCalendarCell8
        val cell9 = binding.tastiCalendarCell9
        val cell10 = binding.tastiCalendarCell10
        val cell11 = binding.tastiCalendarCell11
        val cell12 = binding.tastiCalendarCell12
        val cell13 = binding.tastiCalendarCell13
        val cell14 = binding.tastiCalendarCell14
        val cell15 = binding.tastiCalendarCell15
        val cell16 = binding.tastiCalendarCell16
        val cell17 = binding.tastiCalendarCell17
        val cell18 = binding.tastiCalendarCell18
        val cell19 = binding.tastiCalendarCell19
        val cell20 = binding.tastiCalendarCell20
        val cell21 = binding.tastiCalendarCell21
        val cell22 = binding.tastiCalendarCell22
        val cell23 = binding.tastiCalendarCell23
        val cell24 = binding.tastiCalendarCell24
        val cell25 = binding.tastiCalendarCell25
        val cell26 = binding.tastiCalendarCell26
        val cell27 = binding.tastiCalendarCell27
        val cell28 = binding.tastiCalendarCell28
        val cell29 = binding.tastiCalendarCell29
        val cell30 = binding.tastiCalendarCell30
        val cell31 = binding.tastiCalendarCell31
        val cell32 = binding.tastiCalendarCell32
        val cell33 = binding.tastiCalendarCell33
        val cell34 = binding.tastiCalendarCell34
        val cell35 = binding.tastiCalendarCell35
        val cell36 = binding.tastiCalendarCell36
        val cell37 = binding.tastiCalendarCell37
        // Create a list of every cell
        cellsList = mutableListOf(
            cell1,
            cell2,
            cell3,
            cell4,
            cell5,
            cell6,
            cell7,
            cell8,
            cell9,
            cell10,
            cell11,
            cell12,
            cell13,
            cell14,
            cell15,
            cell16,
            cell17,
            cell18,
            cell19,
            cell20,
            cell21,
            cell22,
            cell23,
            cell24,
            cell25,
            cell26,
            cell27,
            cell28,
            cell29,
            cell30,
            cell31,
            cell32,
            cell33,
            cell34,
            cell35,
            cell36,
            cell37
        )

        // Set the letters for the week days
        val monday = DayOfWeek.MONDAY
        val tuesday = DayOfWeek.TUESDAY
        val thursday = DayOfWeek.THURSDAY
        val wednesday = DayOfWeek.WEDNESDAY
        val friday = DayOfWeek.FRIDAY
        val saturday = DayOfWeek.SATURDAY
        val sunday = DayOfWeek.SUNDAY
        val locale = Locale.getDefault()
        if (!hideWeekDays) {
            if (!sundayFirst) {
                weekDayOne.text = monday.getDisplayName(TextStyle.NARROW, locale)
                weekDayTwo.text = tuesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayThree.text = wednesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFour.text = thursday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFive.text = friday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySix.text = saturday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySeven.text = sunday.getDisplayName(TextStyle.NARROW, locale)
            } else {
                weekDayOne.text = sunday.getDisplayName(TextStyle.NARROW, locale)
                weekDayTwo.text = monday.getDisplayName(TextStyle.NARROW, locale)
                weekDayThree.text = tuesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFour.text = wednesday.getDisplayName(TextStyle.NARROW, locale)
                weekDayFive.text = thursday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySix.text = friday.getDisplayName(TextStyle.NARROW, locale)
                weekDaySeven.text = saturday.getDisplayName(TextStyle.NARROW, locale)
            }
        } else {
            weekDayOne.visibility = View.GONE
            weekDayTwo.visibility = View.GONE
            weekDayThree.visibility = View.GONE
            weekDayFour.visibility = View.GONE
            weekDayFive.visibility = View.GONE
            weekDaySix.visibility = View.GONE
            weekDaySeven.visibility = View.GONE
        }

        // Set the number and name (capitalized) for the month (from range 0-11 to 1-12)
        val firstDayOfWeekForChosenMonth = dateWithChosenMonth.dayOfWeek
        monthTitle = binding.tastiCalendarMonthName
        monthTitle.text =
            dateWithChosenMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                .replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                    else it.toString()
                }


        if (!sundayFirst)
        // Case 1: monday is the first day of the week
            when (firstDayOfWeekForChosenMonth) {
                DayOfWeek.MONDAY -> renderDays(Range(0, 30))
                DayOfWeek.TUESDAY -> renderDays(Range(1, 31))
                DayOfWeek.WEDNESDAY -> renderDays(Range(2, 32))
                DayOfWeek.THURSDAY -> renderDays(Range(3, 33))
                DayOfWeek.FRIDAY -> renderDays(Range(4, 34))
                DayOfWeek.SATURDAY -> renderDays(Range(5, 35))
                DayOfWeek.SUNDAY -> renderDays(Range(6, 36))
                else -> {}
            }
        else
        // Case 2: sunday is the first day of the week
            when (firstDayOfWeekForChosenMonth) {
                DayOfWeek.SUNDAY -> renderDays(Range(0, 30))
                DayOfWeek.MONDAY -> renderDays(Range(1, 31))
                DayOfWeek.TUESDAY -> renderDays(Range(2, 32))
                DayOfWeek.WEDNESDAY -> renderDays(Range(3, 33))
                DayOfWeek.THURSDAY -> renderDays(Range(4, 34))
                DayOfWeek.FRIDAY -> renderDays(Range(5, 35))
                DayOfWeek.SATURDAY -> renderDays(Range(6, 36))
                else -> {}
            }

        // Show snack bars on month header press
        if (showSnackBars) {
            monthTitle.setOnClickListener {
                // TODO Just prints the number of events, quite ugly
                val content = eventCount.toString()
                showSnackbar(content, binding.root)
            }
        }

        // Set the appearance (0 small default, 1 medium, 2 large, 3 xlarge)
        when (appearance) {
            0 -> colorize()
            1 -> setAppearance(1)
            2 -> setAppearance(2)
            3 -> setAppearance(3)
        }
    }

    // Set the information density for month
    fun setAppearance(appearance: Int) {
        when (appearance) {
            0 -> {
                for (cell in cellsList) {
                    cell.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                    cell.setPadding(3, 3, 3, 3)
                    cell.typeface = Typeface.MONOSPACE
                }
                for (day in weekDaysList) {
                    day.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                }
                monthTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            }
            1 -> {
                for (cell in cellsList) {
                    cell.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                    cell.setPadding(8, 8, 8, 8)
                    cell.typeface = Typeface.MONOSPACE
                }
                for (day in weekDaysList) {
                    day.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                monthTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
            }
            2 -> {
                for (cell in cellsList) {
                    cell.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                    cell.setPadding(12, 12, 12, 12)
                    cell.typeface = Typeface.MONOSPACE
                }
                for (day in weekDaysList) {
                    day.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                }
                monthTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
            }
            3 -> {
                for (cell in cellsList) {
                    cell.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium)
                    cell.setPadding(16, 16, 16, 16)
                    cell.typeface = Typeface.MONOSPACE
                }
                for (day in weekDaysList) {
                    day.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium)
                }
                monthTitle.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge)
            }
            else -> return
        }
        colorize()
    }

    // Dynamically set the first day of the week
    fun setSundayFirst(enable: Boolean) {
        if (enable != sundayFirst) {
            sundayFirst = enable
            initMonth()
        }
    }

    // Set a specific year for the overview screen
    fun setYear(year: Int) {
        eventCount = 0
        binding.tastiCalendarMonthName.setTextColor(
            getThemeColor(
                com.google.android.material.R.attr.colorSecondary,
                context
            )
        )
        dateWithChosenMonth = dateWithChosenMonth.withYear(year)
        initMonth()
    }

    // Reset every highlighted cell to the normal state
    fun resetHighlighting() {
        for (cell in cellsList) {
            if (cell.background != null) {
                cell.background.alpha = 0
                cell.background = null
                cell.setOnClickListener(null)
            }
            cell.setTextColor(
                getThemeColor(
                    com.google.android.material.R.attr.colorOnBackground,
                    context
                )
            )
            cell.foreground = null
        }
    }

    // Color some elements in the month
    private fun colorize() {
        if (dateWithChosenMonth.month == LocalDate.now().month && dateWithChosenMonth.year == LocalDate.now().year) {
            monthTitle.setTextColor(
                getThemeColor(
                    com.google.android.material.R.attr.colorTertiary,
                    context
                )
            )
        } else monthTitle.setTextColor(
            getThemeColor(
                com.google.android.material.R.attr.colorSecondary,
                context
            )
        )
        for (weekDay in weekDaysList) weekDay.alpha = .85f
    }
}